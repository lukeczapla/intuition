package org.magicat.repository;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.jetbrains.annotations.NotNull;
import org.joda.time.DateTime;
import org.magicat.model.Article;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.annotation.Secured;

import java.util.List;
import java.util.stream.Stream;

@Api(tags = "Article Entities - full Pubmed data on articles (in the millions) with pmid, pmcid, doi, and pii")
@RepositoryRestResource(collectionResourceRel = "articles", path = "articles")
public interface ArticleRepository extends MongoRepository<Article, String> {
    Article findByPmId(String pmId);
    Article findByPmcId(String pmcId);
    Article findByDoi(String doi);
    Article findByPii(String pii);

    @Query(value = "{'fulltext':{$exists:true}}")
    List<Article> findByFulltext();

    @Query(value = "{title:{$exists:false},pubAbstract:{$exists:false}}")
    List<Article> findByMissingTitleAndPubAbstract();

    @Override
    Page<Article> findAll(@NotNull Pageable page);

    @Override
    long count();

    @ApiOperation(value = "Returns all information for articles after DateTime given")
    List<Article> findByPublicationDateGreaterThan(@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) DateTime dateTime);

    @ApiOperation(value = "Returns only the pmId (Pubmed id number) for articles after DateTime given")
    @Query(value = "{'publicationDate': {$gt: ?0}}", fields = "{'pmId': 1}")
    Stream<Article> findArticlesGreaterThanPublicationDate(@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) DateTime dateTime);

    @Query(value = "{}", fields="{'pmId': 1}")
    List<Article> findAllPmId();

    @NotNull
    @Override
    List<Article> findAllById(@NotNull Iterable<String> ids);

    @Query(value = "{ 'pmId' : { $in: ?0 } }")
    List<Article> findAllByPmIdIn(List<String> pmIds);

    @Query(value = "{journal: {$exists: true}}", fields = "{journal: 1}")
    List<Article> findAllWithJournalOnly(Pageable page);

    @Query(value = "{ 'journal' : { $exists: true } }")
    Page<Article> findAllWithJournal(Pageable page);

    @Query(value = "{ 'hasFullText' : true }")
    Page<Article> findAllFullText(Pageable page);

    @Query(value = "{ 'pmId' : { $in: ?0 }, 'fulltext' : { $exists: false } }")
    List<Article> findAllByPmIdInNoFullText(List<String> pmIds);

    @Query(value = "{ 'pmId' : { $in: ?0 }, 'publicationDate' : { $gt: ?1 } }")
    List<Article> findAllByPmIdInAndPublicationDateAfter(List<String> pmIds, @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) DateTime dateTime);

    @Query(value = "{ 'fulltext' : {$regex: ?0} }", fields = "{'pmId': 1}")
    List<Article> findMatchingArticles(String regex);

    @NotNull
    <S extends Article> List<S> saveAll(@NotNull Iterable<S> entities);

    @NotNull
    <S extends Article> S insert(@NotNull S entity);

    @NotNull
    <S extends Article> List<S> insert(@NotNull Iterable<S> entities);

    @NotNull
    <S extends Article> S save(@NotNull S entity);

    @Secured("ROLE_ADMIN")
    @Override
    void deleteById(@NotNull String id);

    @Secured("ROLE_ADMIN")
    @Override
    void delete(@NotNull Article article);

    @Secured("ROLE_ADMIN")
    @Override
    void deleteAll(@NotNull Iterable<? extends Article> articles);

    @Secured("ROLE_ADMIN")
    @Override
    void deleteAll();


}
