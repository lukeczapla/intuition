package org.magicat.repository;

import io.swagger.annotations.Api;
import org.jetbrains.annotations.NotNull;
import org.magicat.model.CancerType;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

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
    @RestResource(exported = false)
    <S extends CancerType> S insert(@NotNull S s);

    @NotNull
    @Override
    @RestResource(exported = false)
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
    @RestResource(exported = false)
    <S extends CancerType> List<S> saveAll(@NotNull Iterable<S> iterable);

    @NotNull
    @Override
    @RestResource(exported = false)
    <S extends CancerType> S save(@NotNull S s);

    @Override
    @RestResource(exported = false)
    void deleteById(@NotNull String s);

    @Override
    @RestResource(exported = false)
    void delete(@NotNull CancerType cancerType);

    @Override
    @RestResource(exported = false)
    void deleteAll(@NotNull Iterable<? extends CancerType> iterable);
}
