package com.srivath.cart.dtos;

import com.srivath.cart.models.Product;
import com.srivath.cart.models.User;
import lombok.Data;

@Data
public class CartDto {
    Product product;
    Integer quantity;
    User user;
}
