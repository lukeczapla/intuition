package org.magicat.intuition.MIND;

import com.mongodb.client.gridfs.model.GridFSFile;
import org.bson.types.ObjectId;
import org.magicat.intuition.model.Simulation;
import org.magicat.intuition.montecarlo.MCNetwork;
import org.magicat.intuition.repository.SimulationRepository;
import org.magicat.intuition.util.SolrClientTool;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;

@Service
public class SimulationMINDImpl implements SimulationMIND {

    private final static Logger log = LoggerFactory.getLogger(SimulationMIND.class);
    private final GridFsTemplate gridFsTemplate;
    private final SimulationRepository simulationRepository;

    @Autowired
    public SimulationMINDImpl(GridFsTemplate gridFsTemplate, SimulationRepository simulationRepository) {
        this.gridFsTemplate = gridFsTemplate;
        this.simulationRepository = simulationRepository;
    }

    public Simulation saveSimulation(MCNetwork m) {
        Simulation simulation = new Simulation();
        if (m.getName() != null) simulation.setConfigurationName(m.getName()); // do we want to get more exact naming from the configuration?
        else simulation.setConfigurationName(SolrClientTool.randomId(8));
        simulation.setConfigurationJson(m.getConfiguration().toJson());
        simulation.setSummary(m.getModel().summary());
        if (m.time != 0) simulation.setTime((int)m.time);
        simulation.setnInputs(m.getnInputs());
        simulation.setSeed(m.getSeed());
        simulation.setShuffleSeed(m.getShuffleSeed());
        simulation.setnStepsNetwork(m.getnSteps());
        if (m.isBuffered()) {
            simulation.setBuffered(true);
            simulation.setBufferSize(m.getBufferSize());
        }
        if (m.getBias() != 0) simulation.setBias(m.getBias());
        if (m.getMcSteps() != 0) simulation.setMcSteps(m.getMcSteps());
        if (m.getEquilibrationSteps() != 0) simulation.setEquilibrationSteps(m.getEquilibrationSteps());

        if (m.getSamples() != null && !m.getSamples().isEmpty()) {
            String samplesFile = "samples-" + m.getName() + ".bin";
            try (DataOutputStream output = new DataOutputStream(new FileOutputStream(samplesFile))) {
                Nd4j.write(m.getSamples(), output);
                File file = new File(samplesFile);
                ObjectId oid = gridFsTemplate.store(new ByteArrayInputStream(Files.readAllBytes(file.toPath())), samplesFile, "Nd4j Array");
                simulation.setSamplesResourceId(oid.toString());
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        }
        if (m.getHistogram() != null && !m.getHistogram().isEmpty()) {
            simulation.setHistogram(m.getHistogram().data().asDouble());
        }
        File f = m.getCsvFile();
        if (f != null) try {
            ObjectId oid = gridFsTemplate.store(new ByteArrayInputStream(Files.readAllBytes(f.toPath())), f.getName(), Files.probeContentType(f.toPath()));
            simulation.setCsvResourceId(oid.toString());
        } catch(IOException e) {
            log.error(e.getMessage());
            e.printStackTrace();
        }
        if (m.getModelFileName() != null) try {
            File modelFile = new File(m.getModelFileName());
            ObjectId oid = gridFsTemplate.store(new ByteArrayInputStream(Files.readAllBytes(modelFile.toPath())), modelFile.getName(), "DL4j/Keras Model");
            simulation.setModelResourceId(oid.toString());
        } catch (IOException e) {
            log.error(e.getMessage());
            e.printStackTrace();
        }

        return simulationRepository.save(simulation);
    }

    public MCNetwork loadSimulation(Simulation simulation) {
        MCNetwork mc = new MCNetwork();
        String resourceId = simulation.getModelResourceId();
        GridFSFile modelFile = gridFsTemplate.findOne(new Query(Criteria.where("_id").is(resourceId)));
        if (modelFile != null) {
            try {
                GridFsResource resource = gridFsTemplate.getResource(modelFile);
                if (resource.getFilename() != null) {
                    byte[] data = resource.getContent().readAllBytes();
                    FileOutputStream outputStream = new FileOutputStream(resource.getFilename());
                    outputStream.write(data);
                } else {
                    log.error("GridFS: Filename is null");
                    return null;
                }
            } catch (IOException e) {
                log.error(e.getMessage());
                return null;
            }
        }
        resourceId = simulation.getCsvResourceId();
        GridFSFile csvFile = gridFsTemplate.findOne(new Query(Criteria.where("_id").is(resourceId)));
        if (csvFile != null) {
            try {
                GridFsResource resource = gridFsTemplate.getResource(csvFile);
                if (resource.getFilename() != null) {
                    byte[] data = resource.getContent().readAllBytes();
                    FileOutputStream outputStream = new FileOutputStream(resource.getFilename());
                    outputStream.write(data);
                } else {
                    log.error("GridFS: CSV filename is null");
                    return null;
                }
            } catch (IOException e) {
                log.error(e.getMessage());
                return null;
            }
        }
        resourceId = simulation.getSamplesResourceId();
        GridFSFile samplesFile = gridFsTemplate.findOne(new Query(Criteria.where("_id").is(resourceId)));
        INDArray samples = null;
        GridFsResource resource = null;
        if (samplesFile != null) {
            resource = gridFsTemplate.getResource(samplesFile);
            if (resource.getFilename() != null) {
                try {
                    byte[] data = resource.getContent().readAllBytes();
                    FileOutputStream outputStream = new FileOutputStream(resource.getFilename());
                    outputStream.write(data);
                    outputStream.close();
                    DataInputStream input = new DataInputStream(new FileInputStream(resource.getFilename()));
                    samples = Nd4j.read(input);
                } catch (IOException e) {
                    log.error(e.getMessage());
                    return null;
                }
            }
        }
        if (simulation.isBuffered()) {
            int bs = simulation.getBufferSize();
            mc.setBuffered(true);
            mc.setBufferSize(bs);
            if (resource != null && resource.getFilename() != null) mc.setBufferFileName(resource.getFilename());
        }
        if (samples != null) mc.setSamples(samples);
        mc.setName(simulation.getConfigurationName());
        mc.setEquilibrationSteps(simulation.getEquilibrationSteps());
        mc.setStateFrequency(simulation.getStateFrequency());
        mc.setMcSteps(simulation.getMcSteps());
        mc.setnSteps(simulation.getnStepsNetwork());
        mc.setSeed(simulation.getSeed());
        mc.setShuffleSeed(simulation.getShuffleSeed());
        mc.setfTrain(simulation.getfTrain());
        mc.setHistogram(Nd4j.create(simulation.getHistogram()));
        mc.setBias(simulation.getBias());
        return mc;
    }


}
