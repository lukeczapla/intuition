package org.magicat.intuition.MIND;

import org.magicat.intuition.model.Simulation;
import org.magicat.intuition.montecarlo.MCNetwork;

public interface SimulationMIND {
    /**
     * Save the MCNetwork instance into the MongoDB Simulations collection
     * @param m - Any MCNetwork instance (or derived classes)
     * @return The Simulation object that has been persisted into the MongoDB database as a Simulation object
     */
    Simulation saveSimulation(MCNetwork m);
    MCNetwork loadSimulation(Simulation simulation);
}
