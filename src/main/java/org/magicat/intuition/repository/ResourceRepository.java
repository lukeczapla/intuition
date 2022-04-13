package org.magicat.intuition.repository;

import org.magicat.intuition.model.Resource;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import springfox.documentation.annotations.ApiIgnore;

import java.util.List;
import java.util.Optional;

@ApiIgnore
@RepositoryRestResource(collectionResourceRel = "resource", path = "resource")
public interface ResourceRepository extends MongoRepository<Resource, String> {

    @Override
    <S extends Resource> List<S> saveAll(Iterable<S> entities);

    @Override
    List<Resource> findAll();

    @Override
    List<Resource> findAll(Sort sort);

    @Override
    <S extends Resource> S insert(S entity);

    @Override
    <S extends Resource> List<S> insert(Iterable<S> entities);

    @Override
    <S extends Resource> List<S> findAll(Example<S> example);

    @Override
    <S extends Resource> List<S> findAll(Example<S> example, Sort sort);

    @Override
    Page<Resource> findAll(Pageable pageable);

    @Override
    <S extends Resource> S save(S entity);

    @Override
    Optional<Resource> findById(String s);

    @Override
    boolean existsById(String s);

    @Override
    Iterable<Resource> findAllById(Iterable<String> strings);

    @Override
    long count();

    @Override
    void deleteById(String s);

    @Override
    void delete(Resource entity);

    @Override
    void deleteAll(Iterable<? extends Resource> entities);

    @Override
    void deleteAll();

}
