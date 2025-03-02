package com.srivath.cart.events;

import com.srivath.cart.dtos.OrderDTO;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class OrderPlacedEvent extends Event {
    private OrderDTO orderDTO;

    public OrderPlacedEvent()
    {
        this.setEventName("ORDER_PLACED");
    }

    public OrderPlacedEvent(OrderDTO orderDTO) {
        this.orderDTO = orderDTO;
        this.setEventName("ORDER_PLACED");
    }
}
