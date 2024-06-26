package com.example.proiectpao.repository;

import com.example.proiectpao.collection.Punish;
import com.example.proiectpao.enums.Penalties;
import java.util.Date;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PunishRepository extends MongoRepository<Punish, String> {
    Punish findByUserID(String userID);

    List<Punish> findAllByUserID(String userID);

    List<Punish> findAllByUserIDAndSanctionAndExpiryDateIsAfter(
            String userID, Penalties sanction, Date expiryDate);

    List<Punish> findAllByUserIDAndSanction(String userID, Penalties sanction);

    Punish findByUserIDAndSanction(String userID, Penalties sanction);
}
