package org.magicat.MIND;

import org.magicat.model.Simulation;
import org.magicat.montecarlo.MCNetwork;

public interface SimulationMIND {
    /**
     * Save the MCNetwork instance into the MongoDB Simulations collection
     * @param m - Any MCNetwork instance (or derived classes)
     * @return The Simulation object that has been persisted into the MongoDB database as a Simulation object
     */
    Simulation saveSimulation(MCNetwork m);
    MCNetwork loadSimulation(Simulation simulation);
}
