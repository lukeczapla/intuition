package org.magicat.repository;

import io.swagger.annotations.Api;
import org.magicat.model.Antibody;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Api(value = "Antibody repository")
@Repository
public interface AntibodyRepository extends MongoRepository<Antibody, String> {
    List<Antibody> findAllByTargetSymbol(String targetSymbol);
    Antibody findByVendorNameAndVendorCode(String vendorName, String vendorCode);
    List<Antibody> findAllByCommentCount(Integer commentCount);

    @Query(value = "{antigenSequence: {$regex: \"[ACDEFGHIKLMNPQRSTVWY]{8}[ACDEFGHIKLMNPQRSTVWY]*\" }}", fields = "{antigenSequence: 1, targetName: 1, targetSymbol: 1}")
    List<Antibody> getAntigenSequences();

    @Query(value = "{epitope: {$regex:\"[ACDEFGHIKLMNPQRSTVWY]{8}[ACDEFGHIKLMNPQRSTVWY]*\"}}", fields = "{epitope: 1, targetName: 1, targetSymbol: 1}")
    List<Antibody> getEpitope();



}
