package com.srivath.cart.controllerAdvices;

import com.srivath.cart.exceptions.AddressNotFoundInCartException;
import com.srivath.cart.exceptions.CartNotFoundException;
import com.srivath.cart.exceptions.EmptyCartException;
import com.srivath.cart.exceptions.PaymentMethodNotFoundInCartException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CartNotFoundException.class)
    public String handleCartNotFoundException(CartNotFoundException e){
        return e.getMessage();
    }

    @ExceptionHandler(EmptyCartException.class)
    public String handleEmptyCartException(EmptyCartException e){
        return e.getMessage();
    }

    @ExceptionHandler(AddressNotFoundInCartException.class)
    public String handleCartNotFoundException(AddressNotFoundInCartException e){
        return e.getMessage();
    }

    @ExceptionHandler(PaymentMethodNotFoundInCartException.class)
    public String handleCartNotFoundException(PaymentMethodNotFoundInCartException e){
        return e.getMessage();
    }
}
