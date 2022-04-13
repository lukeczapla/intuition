package org.magicat.intuition.repository;

import org.magicat.intuition.model.GlobalTimestamp;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GlobalTimestampRepository extends MongoRepository<GlobalTimestamp, String> {
}
