package com.ridex.matching.repository;

import com.ridex.matching.model.PoolGroup;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface PoolGroupRepository extends MongoRepository<PoolGroup, String> {

    List<PoolGroup> findByOpenTrue();
}
