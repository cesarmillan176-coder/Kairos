package com.cesarmillan.kairos.show;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface ShowRepository extends MongoRepository<CachedShow, Integer> {
}
