package org.magicat.intuition.repository;

import org.jetbrains.annotations.NotNull;
import org.magicat.intuition.model.ProjectList;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectListRepository extends MongoRepository<ProjectList, String> {

    @NotNull
    @Override
    Optional<ProjectList> findById(@NotNull String id);

    @Query(value = "{ 'id' : {$regex: ?0} }")
    List<ProjectList> findIdExpression(String pattern);

    @NotNull
    @Override
    <S extends ProjectList> S save(@NotNull S entity);

}
