package com.srivath.cart.models;

import lombok.Data;

import java.io.Serializable;

@Data
public class Address implements Serializable {
    private String addressLine1;
    private String addressLine2;
    private String addressLine3;
    private String addressLine4;
    private String city;
    private String state;
    private String country;
    private String pinCode;
}
