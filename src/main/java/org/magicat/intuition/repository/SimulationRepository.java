package org.magicat.intuition.repository;

import org.magicat.intuition.model.Simulation;
import org.magicat.intuition.montecarlo.MCNetwork;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.security.access.annotation.Secured;
import springfox.documentation.annotations.ApiIgnore;

import java.util.List;

@ApiIgnore
//@Secured("ROLE_USER")
@RepositoryRestResource(collectionResourceRel = "simulation", path = "simulation")
public interface SimulationRepository extends MongoRepository<Simulation, String> {

    List<Simulation> findAllByConfigurationName(String configurationName);

}
