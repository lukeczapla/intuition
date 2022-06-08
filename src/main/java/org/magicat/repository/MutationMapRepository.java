package org.magicat.repository;

import io.swagger.annotations.Api;
import org.magicat.model.MutationMap;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.security.access.annotation.Secured;

import java.util.List;

@Api(tags = "MutationMaps - names are always in id fields in lowercase (e.g., 'g12c')")
@RepositoryRestResource(collectionResourceRel = "mutationmaps", path = "mutationmaps")
public interface MutationMapRepository extends MongoRepository<MutationMap, String> {
    MutationMap findBySymbol(String symbol);

    @Query(value = "{}", fields="{'symbol': 1}")
    List<MutationMap> findAllSymbols();

    @Query(value = "{synonyms: {$exists: true}}")
    List<MutationMap> findAllWithSynonyms();

    @Override
    @RestResource(exported = false)
    <S extends MutationMap> List<S> saveAll(Iterable<S> entities);

    @Override
    @RestResource(exported = false)
    <S extends MutationMap> S insert(S entity);

    @Override
    @RestResource(exported = false)
    <S extends MutationMap> List<S> insert(Iterable<S> entities);

    @RestResource(exported = false)
    <S extends MutationMap> S save(S entity);

    @Override
    @RestResource(exported = false)
    void deleteById(String id);

    @Override
    @RestResource(exported = false)
    void delete(MutationMap mutationMap);

    @Secured("ROLE_ADMIN")
    @Override
    @RestResource(exported = false)
    void deleteAll(Iterable<? extends MutationMap> mutationMaps);

    @Secured("ROLE_ADMIN")
    @Override
    @RestResource(exported = false)
    void deleteAll();


}
