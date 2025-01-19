package com.srivath.cart.controllerAdvices;

import com.srivath.cart.exceptions.CartNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CartNotFoundException.class)
    public String handleCartNotFoundException(CartNotFoundException e){
        return e.getMessage();
    }
}
