package com.kunaldo.productservice.repository;

import com.kunaldo.productservice.model.Product;
import org.springframework.data.mongodb.repository.MongoRepository;

//This interface is used to save the data of product into the Database
public interface ProductRepository extends MongoRepository<Product, String> {

}