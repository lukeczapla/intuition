package org.mskcc.knowledge.repository;

import io.swagger.annotations.Api;
import org.jetbrains.annotations.NotNull;
import org.mskcc.knowledge.model.CancerMap;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.security.access.annotation.Secured;

import java.util.List;

@Api(tags = "CancerMaps - names are always in id fields in lowercase (e.g., 'cns cancer')")
@RepositoryRestResource(collectionResourceRel = "cancermaps", path = "cancermaps")
public interface CancerMapRepository extends MongoRepository<CancerMap, String> {

    CancerMap findByCancerType(String cancerType);

    @Query(value = "{}", fields="{'cancerType': 1}")
    List<CancerMap> findAllCancerTypes();

    @NotNull
    @Secured("ROLE_ADMIN")
    @Override
    <S extends CancerMap> List<S> saveAll(@NotNull Iterable<S> entities);

    @NotNull
    @Secured("ROLE_ADMIN")
    @Override
    <S extends CancerMap> S insert(@NotNull S entity);

    @NotNull
    @Secured("ROLE_ADMIN")
    @Override
    <S extends CancerMap> List<S> insert(@NotNull Iterable<S> entities);


    @NotNull
    <S extends CancerMap> S save(@NotNull S entity);

    @Secured("ROLE_ADMIN")
    @Override
    void deleteById(@NotNull String id);

    @Secured("ROLE_ADMIN")
    @Override
    void delete(@NotNull CancerMap cancerMap);

    @Secured("ROLE_ADMIN")
    @Override
    void deleteAll(@NotNull Iterable<? extends CancerMap> cancerMaps);

    @Secured("ROLE_ADMIN")
    @Override
    void deleteAll();

}