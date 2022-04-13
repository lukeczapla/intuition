package org.mskcc.knowledge.repository;

import io.swagger.annotations.Api;
import org.mskcc.knowledge.model.CancerMap;
import org.mskcc.knowledge.model.Journal;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.security.access.annotation.Secured;

import java.util.List;

@Api(tags = "Journal Entities - data on journals")
@RepositoryRestResource(collectionResourceRel = "journals", path = "journals")
public interface JournalRepository extends MongoRepository<Journal, String> {

    @Secured("ROLE_ADMIN")
    @Override
    <S extends Journal> List<S> saveAll(Iterable<S> entities);

    @Secured("ROLE_ADMIN")
    @Override
    <S extends Journal> S insert(S entity);

    @Secured("ROLE_ADMIN")
    @Override
    <S extends Journal> List<S> insert(Iterable<S> entities);

    @Secured("ROLE_ADMIN")
    @Override
    <S extends Journal> S save(S entity);

    @Secured("ROLE_ADMIN")
    @Override
    void deleteById(String id);

    @Secured("ROLE_ADMIN")
    @Override
    void delete(Journal journal);

    @Secured("ROLE_ADMIN")
    @Override
    void deleteAll(Iterable<? extends Journal> journals);

    @Secured("ROLE_ADMIN")
    @Override
    void deleteAll();


}
