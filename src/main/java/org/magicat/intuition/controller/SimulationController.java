package org.mskcc.knowledge.controller;

import io.swagger.annotations.Api;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.mskcc.knowledge.model.SimulationParameters;
import org.mskcc.knowledge.montecarlo.MCNetwork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

@ApiIgnore
@RestController
public class SimulationController {

    public static final Logger log = LoggerFactory.getLogger(SimulationController.class);

    @RequestMapping(value = "/simulationRun", method = RequestMethod.POST)
    public String launchProcess(@RequestBody SimulationParameters parameters) {
        try {
            InputStreamReader input = new InputStreamReader(new FileInputStream(parameters.getInputCSVFile()));
            CSVParser csvParser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(input);
            final int size = (int) csvParser.getRecordNumber();
            new Thread(() -> {
                try {
                    MCNetwork network = new MCNetwork(parameters.getInputCSVFile(), size, parameters.getSeed(), csvParser.getRecords().get(0).size() - 1, 180);
                    network.shuffle(parameters.getSeed(), 0.50);
                    network.normalize();
                    network.runNetwork(parameters.getOutputFile(), parameters.getnSteps());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        } catch (IOException e) {
            StackTraceElement[] result = e.getStackTrace();
            StringBuilder stringBuilder = new StringBuilder(1000);
            for (StackTraceElement element : result) {
                stringBuilder.append(element.toString());
            }
            return stringBuilder.toString();
        }
        return "Running job, waiting for result";
    }

    @RequestMapping(value = "/wlmcRun", method = RequestMethod.POST)
    public String launchWLMC(@RequestBody SimulationParameters parameters) {
        try {
            new Thread(() -> {
                MCNetwork network = new MCNetwork(parameters.getInputFile());
                network.runWLMC(200000, 0.00001);
                log.info(network.getHistogram().toStringFull());
            }).start();
        } catch (Exception e) {
            StackTraceElement[] result = e.getStackTrace();
            StringBuilder stringBuilder = new StringBuilder(1000);
            for (StackTraceElement element : result) {
                stringBuilder.append(element.toString());
            }
            return stringBuilder.toString();
        }
        return "in progress";
    }

}
