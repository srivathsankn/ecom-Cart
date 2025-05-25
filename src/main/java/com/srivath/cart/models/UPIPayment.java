package com.srivath.cart.models;

public class UPIPayment extends Payment{
    String UPIAddress;

    public UPIPayment() {
    }

    public UPIPayment(long amount, String paymentMethod, String UPIAddress) {
        super(amount, paymentMethod);
        this.UPIAddress = UPIAddress;
    }
}

