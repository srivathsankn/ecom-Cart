package com.srivath.cart.controllers;

import com.srivath.cart.dtos.CartAddressDto;
import com.srivath.cart.dtos.CartPaymentMethodDto;
import com.srivath.cart.dtos.CartDto;
import com.srivath.cart.dtos.CartItemsDto;
import com.srivath.cart.exceptions.CartNotFoundException;
import com.srivath.cart.models.Cart;
import com.srivath.cart.models.User;
import com.srivath.cart.services.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/cart")
public class CartController {
    @Autowired
    CartService cartService;

    @PostMapping("")
    public Cart addProductToCart(@RequestBody CartDto cartDto) throws InterruptedException {
        return cartService.updateCart(cartDto.getProduct(),  cartDto.getQuantity(), cartDto.getUser());
    }

    @GetMapping("/{id}")
    public Cart getCartDetails(@PathVariable String id) throws CartNotFoundException {
        return cartService.getCartById(id);
    }

    @GetMapping("")
    public Cart getCartDetailsByEmailId(@RequestParam("user") String emailid) throws InterruptedException {
        return cartService.getCartByEmailId(emailid);
    }

    @GetMapping("/old")
    public List<Cart> getCartsCreatedBefore(@RequestParam("createdBefore") String date) {
        return cartService.getCartsCreatedBefore(LocalDate.parse(date));
    }

    @GetMapping("/search")
    public Page<Cart> getCartsCreatedWith (@RequestParam(required = false) String createdOn,
                                            @RequestParam(required = false) Double minAmount,
                                            @RequestParam(required = false) Double maxAmount,
                                            @RequestParam(required = false, defaultValue = "0") Integer page,
                                            @RequestParam(required = false, defaultValue = "5") Integer size) {

        Pageable pageable = PageRequest.of(page, size);
        return cartService.searchCarts(createdOn, minAmount, maxAmount, pageable);
    }

    @GetMapping("/searchItems")
    public Page<CartItemsDto> getCartItemsWith(@RequestParam(required = false) String ownerEmail,
                                               @RequestParam(required = false) Double minPrice,
                                               @RequestParam(required = false) Double maxPrice,
                                               @RequestParam(required = false) Double minQuantity,
                                               @RequestParam(required = false) Double maxQuantity,
                                               @RequestParam(required = false, defaultValue = "0") Integer page,
                                               @RequestParam(required = false, defaultValue = "5") Integer size) {
        Pageable pageable = PageRequest.of(page, size);
        return cartService.searchCartItems(ownerEmail, minPrice, maxPrice, minQuantity, maxQuantity, pageable);
    }

    @PostMapping("/paymentMethods")
    public Cart addPaymentMethod(@RequestBody CartPaymentMethodDto cartPaymentMethodDto) throws CartNotFoundException, InterruptedException {
        return cartService.addPaymentMethod(cartPaymentMethodDto.getPaymentMethod(), cartPaymentMethodDto.getUser());
    }

    @PostMapping("/address")
    public Cart addAddress(@RequestBody CartAddressDto cartAddressDto) throws CartNotFoundException, InterruptedException {
        return cartService.addAddress(cartAddressDto.getAddress(), cartAddressDto.getUser());
    }

    @PostMapping("/checkout")
    public Cart checkout(@RequestBody User user) throws CartNotFoundException {
        return cartService.checkout(user);
    }

}
