package com.srivath.cart.controllerAdvices;

import com.srivath.cart.exceptions.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CartNotFoundException.class)
    public ResponseEntity<String>  handleCartNotFoundException(CartNotFoundException e){
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    @ExceptionHandler(EmptyCartException.class)
    public ResponseEntity<String>  handleEmptyCartException(EmptyCartException e){
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    @ExceptionHandler(AddressNotFoundInCartException.class)
    public ResponseEntity<String>  handleCartNotFoundException(AddressNotFoundInCartException e){
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    @ExceptionHandler(PaymentMethodNotFoundInCartException.class)
    public ResponseEntity<String> handleCartNotFoundException(PaymentMethodNotFoundInCartException e){
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    @ExceptionHandler(UserDetailsNotProvidedException.class)
    public ResponseEntity<String> handleCartNotFoundException(UserDetailsNotProvidedException e){
        return ResponseEntity.badRequest().body(e.getMessage());
    }

}
