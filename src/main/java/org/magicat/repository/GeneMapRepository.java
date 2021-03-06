package org.magicat.repository;

import io.swagger.annotations.Api;
import org.jetbrains.annotations.NotNull;
import org.magicat.model.GeneMap;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.security.access.annotation.Secured;

import java.util.List;

@Api(tags = "GeneMaps - names are always in id fields in lowercase (e.g., 'kras')")
@RepositoryRestResource(collectionResourceRel = "genemaps", path = "genemaps")
public interface GeneMapRepository extends MongoRepository<GeneMap, String> {

    GeneMap findBySymbol(String symbol);

    @Query(value = "{}", fields="{'symbol': 1}")
    List<GeneMap> findAllSymbols();

    @NotNull
    //@Secured("ROLE_ADMIN")
    @Override
    @RestResource(exported = false)
    <S extends GeneMap> List<S> saveAll(@NotNull Iterable<S> entities);

    @NotNull
    //@Secured("ROLE_ADMIN")
    @Override
    @RestResource(exported = false)
    <S extends GeneMap> S insert(@NotNull S entity);

    @NotNull
    //@Secured("ROLE_ADMIN")
    @Override
    @RestResource(exported = false)
    <S extends GeneMap> List<S> insert(@NotNull Iterable<S> entities);

    @NotNull
    @RestResource(exported = false)
    <S extends GeneMap> S save(@NotNull S entity);

    @Override
    @RestResource(exported = false)
    void deleteById(String id);

    @Secured("ROLE_ADMIN")
    @Override
    @RestResource(exported = false)
    void delete(GeneMap geneMap);

    @Secured("ROLE_ADMIN")
    @Override
    @RestResource(exported = false)
    void deleteAll(Iterable<? extends GeneMap> geneMaps);

    @Secured("ROLE_ADMIN")
    @Override
    @RestResource(exported = false)
    void deleteAll();

}
