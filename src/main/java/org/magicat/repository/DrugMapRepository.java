package org.magicat.repository;

import io.swagger.annotations.Api;
import org.jetbrains.annotations.NotNull;
import org.magicat.model.DrugMap;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

import java.util.List;
import java.util.Optional;

@Api(tags = "DrugMaps - names are always in id fields in lowercase (e.g., 'sotorasib')")
@RepositoryRestResource(collectionResourceRel = "drugmaps", path = "drugmaps")
public interface DrugMapRepository extends MongoRepository<DrugMap, String> {

    DrugMap findByDrug(String drug);
    @Query(value = "{synonyms: {$exists: true}}")
    List<DrugMap> findAllWithSynonyms();

    @NotNull
    @Override
    Optional<DrugMap> findById(@NotNull String drug);

    @Query(value = "{}", fields="{'drug': 1}")
    List<DrugMap> findAllDrugs();

    @Query(value = "{cancerDrug: true}", fields="{'drug':1, 'synonyms':1}")
    List<DrugMap> findCancerDrugs();

    @NotNull
    @Override
    @RestResource(exported = false)
    <S extends DrugMap> List<S> saveAll(@NotNull Iterable<S> entities);

    @NotNull
    @Override
    @RestResource(exported = false)
    <S extends DrugMap> S insert(@NotNull S entity);

    @NotNull
    @Override
    @RestResource(exported = false)
    <S extends DrugMap> List<S> insert(@NotNull Iterable<S> entities);

    @NotNull
    @RestResource(exported = false)
    <S extends DrugMap> S save(@NotNull S entity);

    @Override
    @RestResource(exported = false)
    void deleteById(@NotNull String id);

    @Override
    @RestResource(exported = false)
    void delete(@NotNull DrugMap drugMap);

    @Override
    @RestResource(exported = false)
    void deleteAll(@NotNull Iterable<? extends DrugMap> drugMaps);

    @Override
    @RestResource(exported = false)
    void deleteAll();


}
