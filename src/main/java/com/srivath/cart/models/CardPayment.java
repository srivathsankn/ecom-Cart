package com.srivath.cart.models;

import lombok.Data;

@Data
public class CardPayment extends Payment{
    String CardNumber;
    String CardHolderName;
    String ExpiryDate;
    String CVV;
    String CardType;

    public CardPayment() {
    }

    public CardPayment(long amount, String paymentMethod, String cardNumber, String cardHolderName, String expiryDate, String cvv, String cardType) {
        super(amount, paymentMethod);
        CardNumber = cardNumber;
        CardHolderName = cardHolderName;
        ExpiryDate = expiryDate;
        CVV = cvv;
        CardType = cardType;
    }
}
