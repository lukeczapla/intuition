package org.magicat.model;

import com.fasterxml.jackson.annotation.JsonView;

public class SimulationParameters {

    @JsonView(Views.SimulationParameters.class)
    private String inputCSVFile;

    @JsonView(Views.SimulationParameters.class)
    private String outputFile;

    @JsonView(Views.SimulationParameters.class)
    private Integer inputRows;

    @JsonView(Views.SimulationParameters.class)
    private String inputFile;

    @JsonView(Views.SimulationParameters.class)
    private Integer nSteps;

    @JsonView(Views.SimulationParameters.class)
    private Integer seed;

    @JsonView(Views.SimulationParameters.class)
    private Boolean runWLMC;

    @JsonView(Views.SimulationParameters.class)
    private Double bias;

    public String getInputCSVFile() {
        return inputCSVFile;
    }

    public void setInputCSVFile(String inputCSVFile) {
        this.inputCSVFile = inputCSVFile;
    }

    public String getOutputFile() {
        return outputFile;
    }

    public void setOutputFile(String outputFile) {
        this.outputFile = outputFile;
    }

    public Integer getInputRows() {
        return inputRows;
    }

    public void setInputRows(Integer inputRows) {
        this.inputRows = inputRows;
    }

    public String getInputFile() {
        return inputFile;
    }

    public void setInputFile(String inputFile) {
        this.inputFile = inputFile;
    }

    public Integer getnSteps() {
        return nSteps;
    }

    public void setnSteps(Integer nSteps) {
        this.nSteps = nSteps;
    }

    public Integer getSeed() {
        return seed;
    }

    public void setSeed(Integer seed) {
        this.seed = seed;
    }

    public Boolean getRunWLMC() {
        return runWLMC;
    }

    public void setRunWLMC(Boolean runWLMC) {
        this.runWLMC = runWLMC;
    }

    public Double getBias() {
        return bias;
    }

    public void setBias(Double bias) {
        this.bias = bias;
    }
}
