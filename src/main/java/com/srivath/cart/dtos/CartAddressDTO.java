package com.srivath.cart.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CartAddressDTO {
    private String addressLine1;
    private String addressLine2;
    private String addressLine3;
    private String addressLine4;
    private String city;
    private String state;
    private String country;
    private String pinCode;
    private String userEmail;
}
