package org.magicat.repository;

import io.swagger.annotations.Api;
import org.bson.types.ObjectId;
import org.jetbrains.annotations.NotNull;
import org.magicat.model.Target;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

import java.util.List;
import java.util.Optional;

@Api(tags = "Target Entities - data on proteins (genes)")
@RepositoryRestResource(collectionResourceRel = "targets", path = "targets")
public interface TargetRepository extends MongoRepository<Target, ObjectId> {

    List<Target> findAllBySymbol(String symbol);
    List<Target> findAllByUniprotID(String UniprotID);
    List<Target> findAllByFamily(String family);
    Optional<Target> findBySymbol(String symbol);
    @Query(value = "{ 'name': {$regex: ?0} }")
    List<Target> findByNameRegex(String regex);

    @NotNull
    //@Secured("ROLE_ADMIN")
    @Override
    @RestResource(exported = false)
    <S extends Target> List<S> saveAll(@NotNull Iterable<S> entities);

    @NotNull
    @Override
    @RestResource(exported = false)
    <S extends Target> S insert(@NotNull S entity);

    @NotNull
    @Override
    @RestResource(exported = false)

    <S extends Target> List<S> insert(@NotNull Iterable<S> entities);


    @NotNull
    @Override
    @RestResource(exported = false)
    <S extends Target> S save(@NotNull S entity);

    @Override
    @RestResource(exported = false)
    void deleteById(@NotNull ObjectId id);

    @Override
    @RestResource(exported = false)
    void delete(@NotNull Target target);

    @Override
    @RestResource(exported = false)
    void deleteAll(@NotNull Iterable<? extends Target> targets);

    @Override
    @RestResource(exported = false)
    void deleteAll();

}
