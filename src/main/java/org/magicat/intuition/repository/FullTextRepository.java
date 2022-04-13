package org.magicat.intuition.repository;

import org.jetbrains.annotations.NotNull;
import org.magicat.intuition.model.FullText;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FullTextRepository extends MongoRepository<FullText, String> {

    @Query(value = "{ _id: { $regex: 'S'} }")
    List<FullText> findAllSupplementary();

    @Query(value = "{ _id: { $regex: 'S'} }")
    Page<FullText> findAllSupplementary(Pageable pageable);

    default List<FullText> findAllSupplementaryFor(String pmId) {
        return findAllFullTextRegex(pmId + "S");
    }

    @Query(value = "{ _id: { $not: { $regex: 'S'} } }")
    List<FullText> findAllMainText();

    @Query(value = "{ _id: { $not: { $regex: 'S'} } }", fields = "{'pmId': 1, 'textEntry': 1}")
    List<FullText> findAllMainTextNoHTML();

    @Query(value = "{ textEntry: {$exists: false} }")
    List<FullText> findAllMissingFulltext();

    @Query(value = "{ _id: ?0 }")
    FullText findFullTextFor(String pmid);

    @Query(value = "{ _id: {$regex: ?0} }")
    List<FullText> findAllFullTextRegex(String pmid);

    @NotNull
    @Override
    <S extends FullText> List<S> saveAll(@NotNull Iterable<S> entities);

    @NotNull
    @Override
    <S extends FullText> S insert(@NotNull S entity);

    @NotNull
    @Override
    <S extends FullText> List<S> insert(@NotNull Iterable<S> entities);

    @NotNull
    @Override
    <S extends FullText> S save(@NotNull S entity);

    @Override
    void deleteById(@NotNull String id);

    @Override
    void delete(@NotNull FullText fullText);

    @Override
    void deleteAll(@NotNull Iterable<? extends FullText> fullTexts);

    @Override
    void deleteAll();

}
