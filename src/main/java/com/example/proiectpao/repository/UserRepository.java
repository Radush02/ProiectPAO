package com.example.proiectpao.repository;

import com.example.proiectpao.collection.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends MongoRepository<User, String> {
    User findByUsernameIgnoreCase(String username);
}
