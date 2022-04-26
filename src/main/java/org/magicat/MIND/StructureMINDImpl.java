package org.magicat.MIND;

import org.biojava.nbio.structure.*;
import org.magicat.util.AminoAcids;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

@Service
public class StructureMINDImpl implements StructureMIND, Serializable {
    @Serial
    private static final long serialVersionUID = 12345543211L;
    private final Logger log = LoggerFactory.getLogger(StructureMIND.class);

    private List<String> structures = Arrays.asList("4WA9,5MO4,4TWP,3QRK,3QRJ,3QRI,6HD6,6HD4,4ZOG,5NP2,5HU9,4XEY,3DK7,3DK6,3DK3,2QOH,6NPE,4JJD,4JJC,4JJB,3UYO,3UE4,3OXZ,3MSS,3MS9,3K5V,3K2M,3IK3,3CS9,2Z60,2HZN,2HZ0,2HYY,1OPJ,6TF2,5DC0,7DT2,7CC2,6XRG,6XR7,6XR6,6BL8,6AMW,6AMV,5OAZ,4YC8,4J9B,3OY3,3KFA,3KF4,2V7A,2HZI,2HZ4,2HIW,2G1T,2F4J,2E2B,1ZZP,1M52,3T04,1OPK,4J9H,4J9E,4J9D,4J9C,3EG1,2O88,5NP3,6NPV,6NPU,2GQG,5NP5,5DC9,5DC4,2G2H,1OPL,1IEP,1FPU,4J9I,4J9G,3EGU,3EG3,3EG2,3EG0,2FO0,4XLI,2PL0,4J9F,1ABQ,4CGC,2EG2,2G2I,2G2F,2ABL,1AWO,1ABO,1AB2".split(","));

    @Override
    public void analyzeStructures(List<String> items) {
        if (AminoAcids.codeToLetter.isEmpty()) AminoAcids.populate();
        items.forEach(item -> {
            Structure structure;
            try {
                structure = StructureIO.getStructure(item);
                log.info(structure.getName());
                for (EntityInfo e : structure.getEntityInfos()) {
                    log.info(e.getDescription());
                }
            } catch (IOException | StructureException e) {
                log.error(e.getMessage());
                return;
            }
            List<Chain> chains = structure.getChains();
            chains.forEach(chain -> {
                if (chain.getSeqResGroups() != null && chain.getSeqResGroups().size() > 0) chain.getSeqResGroups().forEach(s -> {
                    if (s != null) {
                        //log.info("{}  {}  {}", s.getPDBName(), s.getType().toString());
                        if (AminoAcids.codeToLetter.get(s.getPDBName()) != null)
                          System.out.print(AminoAcids.codeToLetter.get(s.getPDBName()));
                        else System.out.print("/" +s.getPDBName() + "/");
                    }
                });
                System.out.println();
            });
            //System.out.println();
        });
    }

    @Override
    public List<String> getStructures() {
        return structures;
    }

    @Override
    public void setStructures(List<String> structures) {
        this.structures = structures;
    }
}
