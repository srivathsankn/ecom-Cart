package com.srivath.cart.repositories;

import com.srivath.cart.models.Cart;
import com.srivath.cart.models.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CartRepository extends MongoRepository<Cart, String> {

    // One Way to give our Query on Repository method.
    @Query(value = "{ 'createdOn' : { $lt: ?0 } }")
    public List<Cart> findCartsCreatedBefore(LocalDate date);

    Optional<Cart> findById(String id);

}
