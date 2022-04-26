package org.magicat.repository;

import io.swagger.annotations.Api;
import org.magicat.model.Variant;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Api(tags = "Variant Entities - data on protein and gene variants from OncoKB curation")
@Repository
public interface VariantRepository extends MongoRepository<Variant, String> {

    <S extends Variant> List<S> findAllByGeneAndMutationAndDrugsAndCancerTypes(String gene, String mutation, String drugs, String cancerTypes);

    <S extends Variant> List<S> findAllByGeneAndMutation(String gene, String mutation);

    <S extends Variant> S findByDescriptor(String descriptor);

    <S extends Variant> List<S> findAllByMutation(String mutation);

    <S extends Variant> List<S> findAllByKey(String key);

    @Query(value = "{ 'key': {$exists: false} }")
    <S extends Variant> List<S> findAllKeyless();

    <S extends Variant> List<S> findAllByGene(String gene);



}
