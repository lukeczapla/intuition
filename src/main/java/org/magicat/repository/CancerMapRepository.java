package org.magicat.repository;

import io.swagger.annotations.Api;
import org.jetbrains.annotations.NotNull;
import org.magicat.model.CancerMap;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

import java.util.List;

@Api(tags = "CancerMaps - names are always in id fields in lowercase (e.g., 'cns cancer')")
@RepositoryRestResource(collectionResourceRel = "cancermaps", path = "cancermaps")
public interface CancerMapRepository extends MongoRepository<CancerMap, String> {

    CancerMap findByCancerType(String cancerType);

    @Query(value = "{}", fields="{'cancerType': 1}")
    List<CancerMap> findAllCancerTypes();

    @NotNull
    @Override
    @RestResource(exported = false)
    <S extends CancerMap> List<S> saveAll(@NotNull Iterable<S> entities);

    @NotNull
    @Override
    @RestResource(exported = false)
    <S extends CancerMap> S insert(@NotNull S entity);

    @NotNull
    @Override
    @RestResource(exported = false)
    <S extends CancerMap> List<S> insert(@NotNull Iterable<S> entities);


    @NotNull
    @RestResource(exported = false)
    <S extends CancerMap> S save(@NotNull S entity);

    @Override
    @RestResource(exported = false)
    void deleteById(@NotNull String id);

    @Override
    @RestResource(exported = false)
    void delete(@NotNull CancerMap cancerMap);

    @Override
    @RestResource(exported = false)
    void deleteAll(@NotNull Iterable<? extends CancerMap> cancerMaps);

    @Override
    @RestResource(exported = false)
    void deleteAll();

}