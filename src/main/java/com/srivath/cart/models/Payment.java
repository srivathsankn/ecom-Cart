package com.srivath.cart.models;

import lombok.Data;

@Data
public class Payment {
    long amount;
    String paymentMethod;

    public Payment() {
    }

    public Payment(long amount, String paymentMethod) {
        this.amount = amount;
        this.paymentMethod = paymentMethod;
    }

}
