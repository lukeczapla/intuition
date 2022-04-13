package org.magicat.intuition.repository;

import io.swagger.annotations.Api;
import org.jetbrains.annotations.NotNull;
import org.magicat.intuition.model.CancerType;
import org.springframework.context.annotation.Role;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.security.access.annotation.Secured;

import java.util.List;

@Api(tags = "CancerTypes - associates main type and subtype with cancer type code")
@RepositoryRestResource(collectionResourceRel = "cancertypes", path = "cancertypes")
public interface CancerTypeRepository extends MongoRepository<CancerType, String> {

    @NotNull
    @Override
    Page<CancerType> findAll(@NotNull Pageable pageable);

    @NotNull
    @Override
    List<CancerType> findAll();

    @NotNull
    @Override
    List<CancerType> findAll(@NotNull Sort sort);

    @NotNull
    @Override
    <S extends CancerType> S insert(@NotNull S s);

    @NotNull
    @Override
    <S extends CancerType> List<S> insert(@NotNull Iterable<S> iterable);

    @NotNull
    @Override
    <S extends CancerType> List<S> findAll(@NotNull Example<S> example);

    @NotNull
    @Override
    <S extends CancerType> List<S> findAll(@NotNull Example<S> example, @NotNull Sort sort);

    @Override
    long count();

    @NotNull
    @Override
    <S extends CancerType> List<S> saveAll(@NotNull Iterable<S> iterable);

    @NotNull
    @Override
    <S extends CancerType> S save(@NotNull S s);

    @Secured("ROLE_ADMIN")
    @Override
    void deleteById(@NotNull String s);

    @Secured("ROLE_ADMIN")
    @Override
    void delete(@NotNull CancerType cancerType);

    @Secured("ROLE_ADMIN")
    @Override
    void deleteAll(@NotNull Iterable<? extends CancerType> iterable);
}
