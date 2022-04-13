package org.mskcc.knowledge.repository;

import org.mskcc.knowledge.model.GlobalTimestamp;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GlobalTimestampRepository extends MongoRepository<GlobalTimestamp, String> {
}
