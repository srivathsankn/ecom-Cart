package com.srivath.cart.dtos;

import com.srivath.cart.models.User;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
public class CartItemsDto {
    private String id;
    private User owner;
    private String status;
    private Double totalAmount;
    //private List<ItemDto> items;
    private String itemName;
    private Double itemPrice;
    private Integer itemQuantity;

    @Getter
    @Setter
    public static class Event {
        private String eventName;
    }
}
