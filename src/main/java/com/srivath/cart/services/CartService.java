package com.srivath.cart.services;

import com.mongodb.BasicDBObject;
import com.srivath.cart.dtos.CartItemsDto;
import com.srivath.cart.dtos.OrderDto;
import com.srivath.cart.events.Event;
import com.srivath.cart.events.OrderPlacedEvent;
import com.srivath.cart.events.PlaceOrderEvent;
import com.srivath.cart.exceptions.AddressNotFoundInCartException;
import com.srivath.cart.exceptions.CartNotFoundException;
import com.srivath.cart.exceptions.PaymentMethodNotFoundInCartException;
import com.srivath.cart.models.*;
import com.srivath.cart.repositories.CartRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.mapping.Document;
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
import java.util.Map;


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

    public Cart updateCart(Product product, Integer quantity, User user) throws InterruptedException {
        //check if Cart exists for this user
        //if not create a new cart  and add the product
        //if cart exists, check if the product exists in the cart
        //if product exists, update the quantity
        //if product does not exist, add the product

        Cart cart = getCartByEmailId(user.getEmail());
        if (cart==null)
        {
            cart = new Cart();
            cart.setOwner(user);
            cart.setStatus("ACTIVE");
            cart.setCreatedOn(LocalDate.now());
            cart.setUpdatedOn(LocalDate.now());

            cart.getCartItems().add(new CartItem(product, quantity));
        }
        else
        {
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
        }
        cart.setTotalAmount(calculateTotalAmount(cart));
        Cart savedCart = cartRepository.save(cart);
        redisTemplate.opsForHash().put("Cart",user.getEmail(),savedCart);
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
        if (cart != null && redisUp)
            redisTemplate.opsForHash().put("Cart",emailId,cart);
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

    public Cart addPaymentMethod(String[] paymentMethods, User user) throws CartNotFoundException, InterruptedException {
        Cart cart = getCartByEmailId(user.getEmail());
        for (String paymentMethod : paymentMethods) {
            cart.getPaymentMethods().add(PaymentMethods.valueOf(paymentMethod));
        }
        return cartRepository.save(cart);
    }

    public Cart addAddress(Address address, User user) throws InterruptedException {
        Cart cart = getCartByEmailId(user.getEmail());
        cart.setDeliveryAddress(address);
        return cartRepository.save(cart);
    }

    public Cart checkout(User user) throws InterruptedException, PaymentMethodNotFoundInCartException, AddressNotFoundInCartException {
        Cart cart = getCartByEmailId(user.getEmail());

        if (cart.getDeliveryAddress() == null)
            throw new AddressNotFoundInCartException("Address not found in the cart. Please add address before checkout");

        if (cart.getPaymentMethods().isEmpty())
            throw new PaymentMethodNotFoundInCartException("Payment Method not found in the cart. Please add payment method before checkout");

        cart.setStatus("TO_BE_ORDERED");
        cart.setOrderedOn(LocalDate.now());
        cart.setOrderId(System.currentTimeMillis());
        Cart savedCart = cartRepository.save(cart);
        //Write to Kafka for Order Service to pickup
        kafkaTemplate.send(topicName, new PlaceOrderEvent(savedCart));
        //Delete from Redis as well as Cart Repository
        redisTemplate.opsForHash().delete("Cart", user.getEmail());
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
