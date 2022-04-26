package org.magicat.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@ToString
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Document(collection = "Simulations")
public class Simulation {

    @Id
    private String id;

    private String configurationName;

    private String modelResourceId;

    private String samplesResourceId;

    private String csvResourceId;

    private Double fTrain;

    private Integer time;

    private Integer nStepsNetwork;

    private Integer mcSteps;

    private Integer equilibrationSteps;

    private Integer stateFrequency;

    private Long nInputs;

    private Integer seed;

    private Integer shuffleSeed;

    private String configurationJson;

    private String summary;

    private double[] histogram;

    private Double bias;

    private boolean buffered;

    private Integer bufferSize;

    private Integer batchCount;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getModelResourceId() {
        return modelResourceId;
    }

    public void setModelResourceId(String modelResourceId) {
        this.modelResourceId = modelResourceId;
    }

    public String getConfigurationName() {
        return configurationName;
    }

    public void setConfigurationName(String configurationName) {
        this.configurationName = configurationName;
    }

    public String getCsvResourceId() {
        return csvResourceId;
    }

    public void setCsvResourceId(String csvResourceId) {
        this.csvResourceId = csvResourceId;
    }

    public Integer getnStepsNetwork() {
        return nStepsNetwork;
    }

    public void setnStepsNetwork(Integer nStepsNetwork) {
        this.nStepsNetwork = nStepsNetwork;
    }

    public Long getnInputs() {
        return nInputs;
    }

    public void setnInputs(Long nInputs) {
        this.nInputs = nInputs;
    }

    public Integer getSeed() {
        return seed;
    }

    public void setSeed(Integer seed) {
        this.seed = seed;
    }

    public Integer getShuffleSeed() {
        return shuffleSeed;
    }

    public void setShuffleSeed(Integer shuffleSeed) {
        this.shuffleSeed = shuffleSeed;
    }

    public Integer getTime() {
        return time;
    }

    public void setTime(Integer time) {
        this.time = time;
    }

    public Integer getMcSteps() {
        return mcSteps;
    }

    public void setMcSteps(Integer mcSteps) {
        this.mcSteps = mcSteps;
    }

    public Integer getEquilibrationSteps() {
        return equilibrationSteps;
    }

    public void setEquilibrationSteps(Integer equilibrationSteps) {
        this.equilibrationSteps = equilibrationSteps;
    }

    public String getConfigurationJson() {
        return configurationJson;
    }

    public void setConfigurationJson(String configurationJson) {
        this.configurationJson = configurationJson;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getSamplesResourceId() {
        return samplesResourceId;
    }

    public void setSamplesResourceId(String samples) {
        this.samplesResourceId = samples;
    }

    public Double getfTrain() {
        return fTrain;
    }

    public void setfTrain(Double fTrain) {
        this.fTrain = fTrain;
    }

    public double[] getHistogram() {
        return histogram;
    }

    public void setHistogram(double[] histogram) {
        this.histogram = histogram;
    }

    public Double getBias() {
        return bias;
    }

    public void setBias(Double bias) {
        this.bias = bias;
    }

    public boolean isBuffered() {
        return buffered;
    }

    public void setBuffered(boolean buffered) {
        this.buffered = buffered;
    }

    public Integer getBufferSize() {
        return bufferSize;
    }

    public void setBufferSize(Integer bufferSize) {
        this.bufferSize = bufferSize;
    }

    public Integer getBatchCount() {
        return batchCount;
    }

    public void setBatchCount(Integer batchCount) {
        this.batchCount = batchCount;
    }

    public Integer getStateFrequency() {
        return stateFrequency;
    }

    public void setStateFrequency(Integer stateFrequency) {
        this.stateFrequency = stateFrequency;
    }
}
