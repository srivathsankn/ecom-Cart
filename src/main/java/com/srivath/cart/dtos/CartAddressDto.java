package com.srivath.cart.dtos;

import com.srivath.cart.models.Address;
import com.srivath.cart.models.User;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CartAddressDto {
    private Address address;
    private User user;
}
