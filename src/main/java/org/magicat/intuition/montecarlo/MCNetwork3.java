package org.mskcc.knowledge.montecarlo;


import org.datavec.api.records.reader.RecordReader;
import org.datavec.api.records.reader.impl.csv.CSVRecordReader;
import org.datavec.api.split.FileSplit;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.nd4j.evaluation.classification.Evaluation;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.learning.config.Nesterovs;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class MCNetwork3 extends MCNetwork {
    private static Logger log = LoggerFactory.getLogger(MCNetwork3.class);

    public MCNetwork3(int seed) {
        super();
        configuration = new NeuralNetConfiguration.Builder()
                .seed(seed)
                .weightInit(WeightInit.XAVIER)
                .activation(Activation.SOFTSIGN)
                //         .updater(new Sgd(0.2))
                .dropOut(0.5)
                .updater(new Nesterovs(0.02, 0.8))
                //.l2(1e-4)
                .list()
                .layer(new DenseLayer.Builder().nIn(1021).nOut(180).dropOut(0.5)
                        .build())
                .layer(new DenseLayer.Builder().nIn(180).nOut(20).dropOut(0.5)
                        .build())
                .layer(new OutputLayer.Builder(LossFunctions.LossFunction.XENT)
                        .activation(Activation.SIGMOID)
                        .nIn(20).nOut(1).build())
                .build();
        try {
            RecordReader rr = new CSVRecordReader(1, ',');
            rr.initialize(new FileSplit(new File("inputPROMPT2All.csv")));
            DataSetIterator iterator = new RecordReaderDataSetIterator(rr, 9353, 0, 1);
            //new RecordReaderDataSetIterator.Builder(rr, 128).regression(0).build();
            ds = iterator.next();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void runNetwork(String filename) {

        log.info("Building model");

        model = new MultiLayerNetwork(configuration);

        model.init();
        model.setListeners(new ScoreIterationListener(100));
        for (int i = 0; i < 150000; i++)
            model.fit(training);
        try {
            model.save(new File(filename));
        } catch (IOException e) {
            e.printStackTrace();
        }

        log.info("Evaluate model....");
        //Evaluation eval = model.evaluate(testing);
        eval = new Evaluation(2);
        log.info(testing.getFeatures().shapeInfoToString());

        INDArray output = model.output(training.getFeatures());
        eval.eval(training.getLabels(), output);
        log.info(eval.stats());

        eval = new Evaluation(2);

        output = model.output(testing.getFeatures());
        eval.eval(testing.getLabels(), output);

        log.info(eval.stats());

    }

}

