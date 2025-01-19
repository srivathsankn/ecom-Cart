package com.srivath.cart.dtos;

import com.srivath.cart.models.User;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CartPaymentMethodDto {
    private String[] paymentMethod;
    private User user;

    public CartPaymentMethodDto() {
    }

    public CartPaymentMethodDto(String[] paymentMethod, User user) {
        this.paymentMethod = paymentMethod;
        this.user = user;
    }


}
