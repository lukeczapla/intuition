package org.magicat.repository;

import io.swagger.annotations.Api;
import org.bson.types.ObjectId;
import org.jetbrains.annotations.NotNull;
import org.magicat.model.Target;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.security.access.annotation.Secured;

import java.util.List;
import java.util.Optional;

@Api(tags = "Target Entities - data on proteins (genes)")
//@RepositoryRestResource(collectionResourceRel = "targets", path = "targets")
public interface TargetRepository extends MongoRepository<Target, ObjectId> {

    List<Target> findAllBySymbol(String symbol);
    List<Target> findAllByUniprotID(String UniprotID);
    List<Target> findAllByFamily(String family);
    Optional<Target> findBySymbol(String symbol);
    @Query(value = "{ 'name': {$regex: ?0} }")
    List<Target> findByNameRegex(String regex);

    @NotNull
    @Secured("ROLE_ADMIN")
    @Override
    <S extends Target> List<S> saveAll(@NotNull Iterable<S> entities);

    @NotNull
    @Secured("ROLE_ADMIN")
    @Override
    <S extends Target> S insert(@NotNull S entity);

    @NotNull
    @Secured("ROLE_ADMIN")
    @Override
    <S extends Target> List<S> insert(@NotNull Iterable<S> entities);


    @NotNull
    @Override
    <S extends Target> S save(@NotNull S entity);

    @Secured("ROLE_ADMIN")
    @Override
    void deleteById(@NotNull ObjectId id);

    @Secured("ROLE_ADMIN")
    @Override
    void delete(@NotNull Target target);

    @Secured("ROLE_ADMIN")
    @Override
    void deleteAll(@NotNull Iterable<? extends Target> targets);

    @Secured("ROLE_ADMIN")
    @Override
    void deleteAll();

}
