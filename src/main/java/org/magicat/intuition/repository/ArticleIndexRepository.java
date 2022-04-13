package org.mskcc.knowledge.repository;

import io.swagger.annotations.Api;
import org.jetbrains.annotations.NotNull;
import org.mskcc.knowledge.model.ArticleIndex;
import org.mskcc.knowledge.model.CancerMap;
import org.mskcc.knowledge.model.ArticleIndex;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Repository;

import java.util.List;

@Api(tags = "ArticleIndex Entities - data on ArticleIndex - matches Article to Maps")
@Repository
public interface ArticleIndexRepository extends MongoRepository<ArticleIndex, String> {

    @Query(value = "{ _id : { $in: ?0 } }")
    List<ArticleIndex> findAllByPmIdIn(List<String> pmIds);

    @NotNull
    @Override
    Iterable<ArticleIndex> findAllById(@NotNull Iterable<String> iterable);

    <S extends ArticleIndex> List<S> saveAll(Iterable<S> entities);

    @Override
    <S extends ArticleIndex> S insert(S entity);

    @Override
    <S extends ArticleIndex> List<S> insert(Iterable<S> entities);


    <S extends ArticleIndex> S save(S entity);

    @Override
    void deleteById(String id);

    @Override
    void delete(ArticleIndex articleIndex);

    @Override
    void deleteAll(Iterable<? extends ArticleIndex> articleIndexs);

    @Override
    void deleteAll();

}
