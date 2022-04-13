package org.mskcc.knowledge.repository;

import io.swagger.annotations.Api;
import org.jetbrains.annotations.NotNull;
import org.mskcc.knowledge.model.DrugMap;
import org.mskcc.knowledge.model.MutationMap;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.security.access.annotation.Secured;

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
    @Secured("ROLE_ADMIN")
    @Override
    <S extends DrugMap> List<S> saveAll(@NotNull Iterable<S> entities);

    @NotNull
    @Secured("ROLE_ADMIN")
    @Override
    <S extends DrugMap> S insert(@NotNull S entity);

    @NotNull
    @Secured("ROLE_ADMIN")
    @Override
    <S extends DrugMap> List<S> insert(@NotNull Iterable<S> entities);

    @NotNull
    <S extends DrugMap> S save(@NotNull S entity);

    @Secured("ROLE_ADMIN")
    @Override
    void deleteById(@NotNull String id);

    //@Secured("ROLE_ADMIN")
    @Override
    void delete(@NotNull DrugMap drugMap);

    @Secured("ROLE_ADMIN")
    @Override
    void deleteAll(@NotNull Iterable<? extends DrugMap> drugMaps);

    @Secured("ROLE_ADMIN")
    @Override
    void deleteAll();


}
