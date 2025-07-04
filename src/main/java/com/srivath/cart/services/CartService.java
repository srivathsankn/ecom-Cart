package com.srivath.cart.services;

import com.srivath.cart.dtos.CartAddressDTO;
import com.srivath.cart.dtos.CartItemsDto;
import com.srivath.cart.exceptions.*;
import com.srivath.cart.models.*;
import com.srivath.cart.repositories.CartRepository;
import com.srivath.ecombasedomain.dtos.CartOrderDto;
import com.srivath.ecombasedomain.dtos.OrderDto;
import com.srivath.ecombasedomain.events.Event;
import com.srivath.ecombasedomain.events.OrderPlacedEvent;
import com.srivath.ecombasedomain.events.PlaceOrderEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;


@Service
public class CartService {
    @Autowired
    CartRepository cartRepository;
    @Autowired
    MongoTemplate mongoTemplate;
    @Autowired
    RedisTemplate<String, Object> redisTemplate;
    @Autowired
    KafkaTemplate<String, PlaceOrderEvent> kafkaTemplate;
    @Value("${spring.kafka.topic.name}")
    String topicName;

    public static final Logger logger = LoggerFactory.getLogger(CartService.class);

    public Cart updateCart(Product product, Integer quantity, User user) throws InterruptedException, UserDetailsNotProvidedException {
        //get Cart or get a new Cart created foe the user
        //if cart exists, check if the product exists in the cart
        //if product exists, update the quantity
        //if product does not exist, add the product

        //Check for Name, Email and Phone Number to be populated in User
        if (user.getUserName() == null || user.getEmail() == null || user.getPhoneNumber() == null)
        {
            throw new UserDetailsNotProvidedException("User details are not complete. Please provide Name, Email and Phone Number before checkout");
        }

        Cart cart = getCartByEmailId(user.getEmail());

        cart.setOwner(user); //Setting the owner of the cart

        boolean productExists = false;
        for (CartItem cartItem: cart.getCartItems())
        {
            if (cartItem.getProduct().getId().equals(product.getId()))
            {
                cartItem.getProduct().setPrice(product.getPrice()); //Updating the price of the product
                cartItem.setQuantity(cartItem.getQuantity()+quantity); //Adding the quantity to the existing quantity
                productExists = true;
                break;
            }
        }

        if (!productExists)
        {
            cart.getCartItems().add(new CartItem(product, quantity));
        }

        cart.setTotalAmount(Double.valueOf(calculateTotalAmount(cart)));
        Cart savedCart = cartRepository.save(cart);
        try
        {
            redisTemplate.opsForHash().put("Cart",user.getEmail(),savedCart);
        }
        catch (RedisConnectionFailureException e)
        {
            e.printStackTrace();
            System.out.println("Redis is down. Hence, not updating the cache");
        }
        return savedCart;
    }

    public Cart getCartByEmailId(String emailId) throws InterruptedException {
        boolean redisUp = true;
        try {
            Cart cartFromCache = (Cart) redisTemplate.opsForHash().get("Cart", emailId);

            //System.out.println(cartFromCache);
            if (cartFromCache != null) {
                //System.out.println("Cart fetched from cache");
                return cartFromCache;
            }
        } catch (RedisConnectionFailureException e) {
            redisUp = false;
            e.printStackTrace();
            logger.info("Redis is down; Hence Going to Fetch from DB");
        }
        //Added to make Mongo Fetch operation heavy in order to show advantage of cache.
        //Thread.sleep(1000);
        Query query = new Query(Criteria.where("status").is("ACTIVE").and("owner.email").is(emailId));
        Cart cart = mongoTemplate.findOne(query, Cart.class);


        //If no Active cart found, create a new cart.
        if (cart == null)
        {
            cart = new Cart();
            cart.setOwner(new User(emailId));
            cart.setStatus("ACTIVE");
            cart.setCreatedOn(LocalDate.now());
            cart.setUpdatedOn(LocalDate.now());
            cart.setTotalAmount(Double.valueOf(0));
        }
        cartRepository.save(cart);
        if (redisUp)
        {
            redisTemplate.opsForHash().put("Cart", emailId, cart);
            //System.out.println("Cart fetched from DB and saved to cache");
        }

        //redisTemplate.opsForHash().put("Cart",emailId,cart);
        return cart;
    }

    public Cart getCartById(String id) throws CartNotFoundException {
        return this.cartRepository.findById(id).orElseThrow(() -> new CartNotFoundException("Cart with id "+ id + " is not found"));
    }

    public List<Cart> getCartsCreatedBefore(LocalDate date) {
        return this.cartRepository.findCartsCreatedBefore(date);
    }

    public double calculateTotalAmount(Cart cart) {
        double totalPrice = 0;
        for (CartItem cartItem: cart.getCartItems()) {
            totalPrice += cartItem.getProduct().getPrice() * cartItem.getQuantity();
        }
        return totalPrice;
    }



    public Page<Cart> searchCarts(String createdOn, Double minPrice, Double maxPrice, Pageable pageable) {
        Query query = new Query().with(pageable);
        List<Criteria> criteriaList = new ArrayList();

        if(createdOn != null && !createdOn.isEmpty()) {
            criteriaList.add(Criteria.where("createdOn").is(LocalDate.parse(createdOn)));
        }

        if(minPrice != null) {
            criteriaList.add(Criteria.where("totalAmount").gte(minPrice));
        }

        if (maxPrice != null) {
            criteriaList.add(Criteria.where("totalAmount").lte(maxPrice));
        }

        if (!criteriaList.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        }

        List<Cart> cartList = mongoTemplate.find(query, Cart.class);

        Page<Cart> carts = PageableExecutionUtils.getPage(cartList  , pageable, () -> mongoTemplate.count(query, Cart.class));

        return carts;
    }

    public Page<CartItemsDto> searchCartItems(String ownerEmail, Double minPrice, Double maxPrice, Double minQuantity, Double maxQuantity, Pageable pageable) {

        MatchOperation matchEmailOperation = Aggregation.match(Criteria.where("owner.email").is(ownerEmail));
        UnwindOperation unwindCartItemsOperation = Aggregation.unwind("cartItems");
        UnwindOperation unWindProductOperation = Aggregation.unwind("cartItems.product");
        ProjectionOperation projectOperation = Aggregation.project("id", "status", "totalAmount")
                .and("cartItems.product.name").as("itemName")
                .and("cartItems.product.price").as("itemPrice")
                .and("cartItems.quantity").as("itemQuantity")
                .and("owner").as("owner")
                .andExclude("_id");
        MatchOperation matchMinPriceOperation = Aggregation.match(Criteria.where("cartItems.product.price").gte(minPrice));
        MatchOperation matchMaxPriceOperation = Aggregation.match(Criteria.where("cartItems.product.price").lte(maxPrice));
        MatchOperation matchMinQtyOperation = Aggregation.match(Criteria.where("cartItems.quantity").gte(minQuantity));
        MatchOperation matchMaxQtyOperation = Aggregation.match(Criteria.where("cartItems.quantity").lte(maxQuantity));

        SkipOperation skipOperation = Aggregation.skip((long) pageable.getPageNumber() * pageable.getPageSize());
        LimitOperation limitOperation = Aggregation.limit(pageable.getPageSize());
//        GroupOperation groupOperation = Aggregation.group("owner") // Grouping by owner
//                .first("status").as("status") // Take the first 'status' from the group
//                .first("totalAmount").as("totalAmount") // Take the first 'totalAmount' from the group
//                .push(new BasicDBObject("itemName", "$itemName")
//                        .append("itemPrice", "$itemPrice")
//                        .append("itemQuantity", "$itemQuantity"))
//                .as("items");

        List<AggregationOperation> aggregationOperations = new ArrayList<>();

        //if(ownerEmail != null && !ownerEmail.isEmpty()) {
            aggregationOperations.add(matchEmailOperation);
        //}

        aggregationOperations.add(unwindCartItemsOperation);
        aggregationOperations.add(unWindProductOperation);

        if(minPrice != null) {
            System.out.println("Min Price: "+minPrice);
            aggregationOperations.add(matchMinPriceOperation);
        }

        if (maxPrice != null) {
            System.out.println("Max Price: "+maxPrice);
            aggregationOperations.add(matchMaxPriceOperation);
        }

        if (minQuantity != null) {
            aggregationOperations.add(matchMinQtyOperation);
        }

        if (maxQuantity != null) {
            aggregationOperations.add(matchMaxQtyOperation);
        }

        aggregationOperations.add(projectOperation);

        Aggregation aggregationForCount = Aggregation.newAggregation(aggregationOperations);
        AggregationResults<CartItemsDto> resultForCount = mongoTemplate.aggregate(aggregationForCount, Cart.class, CartItemsDto.class);
        long totalCount = resultForCount.getMappedResults().stream().count();

        aggregationOperations.add(skipOperation);
        aggregationOperations.add(limitOperation);
        //aggregationOperations.add(groupOperation1);


        Aggregation aggregation = Aggregation.newAggregation(aggregationOperations);

        AggregationResults<CartItemsDto> result = mongoTemplate.aggregate(aggregation, Cart.class, CartItemsDto.class);
        List<CartItemsDto> mappedResults = result.getMappedResults();
        mappedResults.forEach(System.out::println);
        Page<CartItemsDto> cartItems = PageableExecutionUtils.getPage(mappedResults, pageable, () -> totalCount);
        return cartItems;
    }

//    public Cart addPaymentMethod(String[] paymentMethods, String userEmail) throws CartNotFoundException, InterruptedException {
//        Cart cart = getCartByEmailId(userEmail);
//        for (String paymentMethod : paymentMethods) {
//            cart.getPayments().add(new Payment(PaymentMethod.valueOf(paymentMethod.toUpperCase())));
//        }
//        Cart savedCart = cartRepository.save(cart);
//        try {
//            redisTemplate.opsForHash().put("Cart", userEmail, savedCart);
//        }
//        catch (RedisConnectionFailureException e) {
//            e.printStackTrace();
//            System.out.println("Redis is down. Hence, not updating the cache");
//        }
//        return savedCart;
//    }

    public Cart addAddress(CartAddressDTO cartAddressDTO) throws InterruptedException {
        Address address = new Address();
        address.setAddressLine1(cartAddressDTO.getAddressLine1());
        address.setAddressLine2(cartAddressDTO.getAddressLine2());
        address.setAddressLine3(cartAddressDTO.getAddressLine3());
        address.setAddressLine4(cartAddressDTO.getAddressLine4());
        address.setCity(cartAddressDTO.getCity());
        address.setState(cartAddressDTO.getState());
        address.setCountry(cartAddressDTO.getCountry());
        address.setPinCode(cartAddressDTO.getPinCode());
        Cart cart = getCartByEmailId(cartAddressDTO.getUserEmail());
        cart.setDeliveryAddress(address);
        Cart savedCart = cartRepository.save(cart);
        try {
            redisTemplate.opsForHash().put("Cart", cartAddressDTO.getUserEmail(), savedCart);
        }
        catch (RedisConnectionFailureException e) {
            e.printStackTrace();
            System.out.println("Redis is down. Hence, not updating the cache");
        }
        return savedCart;
    }

    public Cart checkout(User user) throws InterruptedException, AddressNotFoundInCartException, EmptyCartException, UserDetailsNotProvidedException {
        Cart cart = getCartByEmailId(user.getEmail());

        //Check for Name, Email and Phone Number to be populated in User
        if (user.getUserName() == null || user.getEmail() == null || user.getPhoneNumber() == null)
        {
            throw new UserDetailsNotProvidedException("User details are not complete. Please provide Name, Email and Phone Number before checkout");
        }

        if (cart.getCartItems().isEmpty())
        {
            throw new EmptyCartException("Cart is empty. Please add items to the cart before checkout");
        }

        if (cart.getDeliveryAddress() == null)
            throw new AddressNotFoundInCartException("Address not found in the cart. Please add address before checkout");

//        if (cart.getPayments().isEmpty())
//            throw new PaymentMethodNotFoundInCartException("Payment Method not found in the cart. Please add payment method before checkout");

        cart.setStatus("TO_BE_ORDERED");
        cart.setOrderedOn(LocalDate.now());
        //cart.setOrderId(System.currentTimeMillis());
        Cart savedCart = cartRepository.save(cart);
        //Write to Kafka for Order Service to pickup
        CartOrderDto cartOrderDto = new CartOrderDto();
        cartOrderDto.setCartId(savedCart.getId());
        cartOrderDto.setUserEmail(savedCart.getOwner().getEmail());
        cartOrderDto.setTotalAmount(savedCart.getTotalAmount());
        cartOrderDto.setUserName(savedCart.getOwner().getUserName());
        cartOrderDto.setUserPhone(savedCart.getOwner().getPhoneNumber());

        kafkaTemplate.send(topicName, new PlaceOrderEvent(cartOrderDto));
        //Delete from Redis as well as Cart Repository
        try
        {
            redisTemplate.opsForHash().delete("Cart", user.getEmail());
        }
        catch (RedisConnectionFailureException e) {
            e.printStackTrace();
            System.out.println("Redis is down. Hence, not updating the cache");
        }
        return savedCart;
    }

    @KafkaListener(topics = "${spring.kafka.topic.name}", groupId = "cartService")
    public void consume(Event event) throws CartNotFoundException {
        if (event.getEventName().equals("ORDER_PLACED"))
        {
            OrderPlacedEvent orderPlacedEvent = (OrderPlacedEvent) event;
            OrderDto orderDto = orderPlacedEvent.getOrderDto();
            Cart cart = cartRepository.findById(orderDto.getCartId()).orElseThrow(() -> new CartNotFoundException("Cart with id "+ orderDto.getCartId() + " is not found. Check Order "+ orderDto.getOrderId()));
            cart.setStatus("ORDERED");
            cart.setOrderId(orderDto.getOrderId());
            cart.setOrderedOn(orderDto.getOrderDate());
            cartRepository.save(cart);
        }
    }
}
