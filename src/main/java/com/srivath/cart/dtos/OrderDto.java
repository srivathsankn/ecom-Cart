package com.srivath.cart.dtos;

import com.srivath.cart.models.User;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class OrderDto {
    private String cartId;
    private User userId;
    private Long orderId;
    private LocalDate orderDate;

}
