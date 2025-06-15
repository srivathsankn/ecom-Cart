package com.srivath.cart.models;

import lombok.Data;

@Data
public class Payment {

    PaymentMethod paymentMethod;

    public Payment() {
    }

    public Payment( PaymentMethod paymentMethod) {

        this.paymentMethod = paymentMethod;
    }

}
