package org.magicat.repository;

import org.magicat.model.Simulation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import springfox.documentation.annotations.ApiIgnore;

import java.util.List;

@ApiIgnore
//@Secured("ROLE_USER")
@RepositoryRestResource(collectionResourceRel = "simulation", path = "simulation")
public interface SimulationRepository extends MongoRepository<Simulation, String> {

    List<Simulation> findAllByConfigurationName(String configurationName);

}
