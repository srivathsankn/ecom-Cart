package com.srivath.cart.dtos;

import com.srivath.cart.models.User;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CartPaymentMethodDto {
    private String[] paymentMethod;
    private String userEmail;

    public CartPaymentMethodDto() {
    }

    public CartPaymentMethodDto(String[] paymentMethod, String userEmail) {
        this.paymentMethod = paymentMethod;
        this.userEmail = userEmail;
    }


}
