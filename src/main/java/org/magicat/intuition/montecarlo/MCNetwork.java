package org.mskcc.knowledge.montecarlo;

import lombok.EqualsAndHashCode;
import org.datavec.api.records.reader.RecordReader;
import org.datavec.api.records.reader.impl.csv.CSVRecordReader;
import org.datavec.api.split.FileSplit;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.deeplearning4j.nn.conf.ComputationGraphConfiguration;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.nd4j.evaluation.classification.Evaluation;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.SplitTestAndTrain;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerStandardize;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Nesterovs;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.List;

@EqualsAndHashCode
public class MCNetwork {

    private final static Logger log = LoggerFactory.getLogger(MCNetwork.class);

    protected String name;

    protected DataSet ds, training, testing;
    protected double fTrain;
    protected File csvFile;
    protected boolean normalized = false;

    protected String modelFileName;

    protected String bufferFileName = null;

    protected long nInputs, layer1;

    protected MultiLayerConfiguration configuration;
    protected ComputationGraphConfiguration graphConfiguration;
    protected ComputationGraph graph;
    protected MultiLayerNetwork model;
    protected Evaluation eval;

    protected INDArray W0 = null;
    protected INDArray systemFeatures = null;
    protected List<String> labels = null;

    protected INDArray state, bestState;
    protected double score = -1000.0, bestScore = 0, accuracy = 0, precision = 0, recall = 0, bias = 0;

    protected INDArray histogram = null;
    protected INDArray samples = null;
    protected int total = 0, nSteps = 0, mcSteps = 0, equilibrationSteps = 0;
    public long time = 0;
    protected int seed = 0, shuffleSeed = 0, bufferSize = 5000, stateFrequency = 10;
    protected boolean buffered = false;

    protected double learningRate = 0.005, momentum = 0.8;

    public MCNetwork() {
        log.info("Creating empty MCNetwork with no training parameters, be sure to run a MCNetwork::setup() method.");
    }

    public MCNetwork(String inputFilename) {
        loadNetwork(inputFilename);
        log.info(model.summary());
        this.configuration = model.getLayerWiseConfigurations();
        nInputs = model.getLayer(0).getParam("W").rows();
        layer1 = model.getLayer(1).getParam("W").rows();
        log.info("Loaded {}, input layer {} layer1 {}", inputFilename, nInputs, layer1);
    }

    public MCNetwork(String csvName, int nRows, int seed, long nInputs, long layer1) {
        this.nInputs = nInputs;
        this.layer1 = layer1;
        this.seed = seed;
        histogram = Nd4j.zeros(nInputs);
        configuration = new NeuralNetConfiguration.Builder()
                .seed(seed)
                .weightInit(WeightInit.XAVIER)
                .activation(Activation.SOFTSIGN)
                //         .updater(new Sgd(0.2))
                .dropOut(0.9)
                .updater(new Nesterovs(learningRate, 0.8))
                //.l2(1e-4)
                .list()
                .layer(new DenseLayer.Builder().nIn(nInputs).nOut(layer1).dropOut(0.9)
                        .build())
                .layer(new DenseLayer.Builder().nIn(layer1).nOut(100).dropOut(0.9).build())
                .layer(new OutputLayer.Builder(LossFunctions.LossFunction.XENT)
                        .activation(Activation.SIGMOID)
                        .nIn(100).nOut(1).build())
                .build();

        try {
            RecordReader rr = new CSVRecordReader(1, ',');
            csvFile = new File(csvName);
            rr.initialize(new FileSplit(csvFile));
            DataSetIterator iterator = new RecordReaderDataSetIterator(rr, nRows, 0, 1);
            //new RecordReaderDataSetIterator.Builder(rr, 128).regression(0).build();
            ds = iterator.next();
            rr.close();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public MCNetwork buffered(String bufferFileName, int bufferSize) {
        this.bufferFileName = bufferFileName;
        this.bufferSize = bufferSize;
        this.buffered = true;
        return this;
    }

    public MCNetwork buffered(String bufferFileName) {
        return buffered(bufferFileName, 5000);
    }

    public void setupData(String csvName, int nRows) {
        try {
            csvFile = new File(csvName);
            RecordReader rr = new CSVRecordReader(1, ',');
            rr.initialize(new FileSplit(csvFile));
            DataSetIterator iterator = new RecordReaderDataSetIterator(rr, nRows, 0, 1);
            //new RecordReaderDataSetIterator.Builder(rr, 128).regression(0).build();
            ds = iterator.next();
            rr.close();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void setup(MultiLayerConfiguration conf, String csvName, int nRows) {
        log.info("Running setup() with provided network configuration/topology");
        this.configuration = conf;
        String json = conf.toJson();
        String nin = json.substring(json.indexOf("\"nin\" : ") + "\"nin\" : ".length());
        String nout = json.substring(json.indexOf("\"nout\" : ") + "\"nout\" : ".length());
        nInputs = Long.parseLong(nin.substring(0, nin.indexOf(",")));
        layer1 = Long.parseLong(nout.substring(0, nout.indexOf(",")));
        log.info("setup() - input layer {} layer1 {}", nInputs, layer1);
        histogram = Nd4j.zeros(nInputs);
        setupData(csvName, nRows);
    }

    public void setup(String csvName, int nRows, int seed, long nInputs, long layer1) {
        log.info("Running setup() with default configuration");
        this.nInputs = nInputs;
        this.layer1 = layer1;
        histogram = Nd4j.zeros(nInputs);
        configuration = new NeuralNetConfiguration.Builder()
                .seed(seed)
                .weightInit(WeightInit.XAVIER)
                .activation(Activation.SOFTSIGN)
                .dropOut(0.9)
                .updater(new Nesterovs(0.005, 0.8))
                .list()
                .layer(new DenseLayer.Builder().nIn(nInputs).nOut(layer1).dropOut(0.9)
                        .build())
                .layer(new DenseLayer.Builder().nIn(layer1).nOut(20).dropOut(0.9).build())
                .layer(new OutputLayer.Builder(LossFunctions.LossFunction.XENT)
                        .activation(Activation.SIGMOID)
                        .nIn(20).nOut(1).build())
                .build();
        setupData(csvName, nRows);
    }

    public void shuffle(int seed, double fTrain) {
        this.fTrain = fTrain;
        this.shuffleSeed = seed;
        ds.shuffle(seed);
        SplitTestAndTrain testAndTrain = ds.splitTestAndTrain(fTrain);
        training = testAndTrain.getTrain();
        testing = testAndTrain.getTest();
    }

    public void shuffle(int seed) {
        shuffle(seed, 0.5);
    }

    /**
     * Experimental code section, drops a node entirely from the system.
     * @param node the index of the input variable to strip from the system.
     */
    public void dropNode(int node) {
        if (W0 == null) {
            W0 = model.getParam("0_W").dup();
        }
        INDArray W = W0.dup();
        W.putRow(node, Nd4j.zeros(layer1));
        model.setParam("0_W", W);
    }

    /**
     * Puts the data on a scale with mean of zero and standard deviation of 1.0, very simple because weights and biases can bring it up.
     */
    public void normalize() {
        DataNormalization normalization = new NormalizerStandardize();
        normalization.fit(ds);
        normalization.transform(training);
        normalization.transform(testing);
        normalized = true;
    }

    public void runNetwork(String filename) {
        runNetwork(filename, 50000);
    }

    public void runNetwork(String filename, int steps) {

        if (!normalized) {
            log.info("Normalizing data");
            normalize();
        }

        log.info("Building model");
        this.nSteps = steps;

        model = new MultiLayerNetwork(configuration);

        model.init();
        model.setListeners(new ScoreIterationListener(1000));
        log.info(model.summary());
        long t0 = System.currentTimeMillis();
        for (int i = 0; i < steps; i++) {
            model.fit(training);
        }
        long t1 = System.currentTimeMillis();
        time = (t1-t0)/1000;
        try {
            model.save(new File(filename));
            setModelFileName(filename);
        } catch (IOException e) {
            e.printStackTrace();
        }

        log.info("Evaluating model....");
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

        state = Nd4j.ones(nInputs);

    }

    public void resumeNetwork(String filename, int nSteps) {
        learningRate /= 10.0;
        momentum -= 0.2;
        NeuralNetConfiguration config = new NeuralNetConfiguration.Builder()
                .seed(seed)
                .weightInit(WeightInit.XAVIER)
                .activation(Activation.SOFTSIGN)
                //         .updater(new Sgd(0.2))
                .dropOut(0.9)
                .updater(new Nesterovs(learningRate, momentum)).build();
        model.init();
        for (int i = 0; i < nSteps; i++)
            model.fit(training);
        try {
            model.save(new File(filename));
            setModelFileName(filename);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void tune() {

        learningRate /= 10.0;
        momentum -= 0.2;
        NeuralNetConfiguration config = new NeuralNetConfiguration.Builder()
                .seed(seed)
                .weightInit(WeightInit.XAVIER)
                .activation(Activation.SOFTSIGN)
                //         .updater(new Sgd(0.2))
                .dropOut(0.9)
                .updater(new Nesterovs(learningRate, momentum)).build();
        model.setConf(config);
    }

    public boolean loadNetwork(String filename) {
        try {
            model = MultiLayerNetwork.load(new File(filename), true);
            configuration = model.getLayerWiseConfigurations();
            log.info(configuration.toJson());
        } catch (IOException e) {
            e.printStackTrace();
            log.error("MAJOR FAILURE - Could not load network Model file");
            return false;
        }
        return true;
    }

    public double evaluate() {
        eval = new Evaluation(2);
        log.info(testing.getFeatures().shapeInfoToString());

        INDArray output = model.output(training.getFeatures());
        eval.eval(training.getLabels(), output);
        log.info(eval.stats());

        eval = new Evaluation(2, 1);

        output = model.output(testing.getFeatures());
        eval.eval(testing.getLabels(), output);

        log.info(eval.stats());
        return eval.f1();
    }

    public void move(INDArray inputMask) {
        INDArray W = Nd4j.zeros(nInputs, layer1);
        for (int i = 0; i < inputMask.size(0); i++) {
            if (inputMask.getInt(i) == 1) {
                W.putRow(i, W0.getRow(i).dup());
                // p.putColumn(index,patientFeatures.getColumn(i));
            }
        }
        model.setParam("0_W", W);
    }

    public double evaluate(INDArray inputMask) {
        if (W0 == null) {
            INDArray W = model.getParam("0_W");
            W0 = W.dup();
            //INDArray result = testing.getFeatures().mmul(W0);
            log.info("weight matrix {}", W0.shapeInfoToString());
        }
        if (systemFeatures == null) {
            systemFeatures = testing.getFeatures().dup();
            log.info("features {}", systemFeatures.shapeInfoToString());
        }
        Integer sum = inputMask.sumNumber().intValue();
        INDArray W = Nd4j.zeros(nInputs, layer1);
        //INDArray p = Nd4j.create(testing.numExamples(), sum);
        for (int i = 0; i < inputMask.size(0); i++) {
            if (inputMask.getInt(i) == 1) {
                W.putRow(i, W0.getRow(i).dup());
                // p.putColumn(index,patientFeatures.getColumn(i));
            }
        }

        model.setParam("0_W", W);
        INDArray output = model.output(testing.getFeatures());

        eval = new Evaluation(2, 1);
        eval.eval(testing.getLabels(), output);
        if (eval.f1() > bestScore) {
            bestScore = eval.f1();
            accuracy = eval.accuracy();
            precision = eval.precision();
            recall = eval.recall();
        }

        return eval.f1();
//        log.info(eval.stats());
    }

    public double evaluate(INDArray inputMask, Double bias) {
        if (W0 == null) {
            INDArray W = model.getParam("0_W");
            W0 = W.dup();
            //INDArray result = testing.getFeatures().mmul(W0);
            log.info("weight matrix {}", W0.shapeInfoToString());
        }
        if (systemFeatures == null) {
            systemFeatures = testing.getFeatures().dup();
            log.info("features {}", systemFeatures.shapeInfoToString());
        }
        Integer sum = inputMask.sumNumber().intValue();
        INDArray W = Nd4j.zeros(nInputs, layer1);
        //INDArray p = Nd4j.create(testing.numExamples(), sum);
        for (int i = 0; i < inputMask.size(0); i++) {
            if (inputMask.getInt(i) == 1) {
                W.putRow(i, W0.getRow(i).dup());
                // p.putColumn(index,patientFeatures.getColumn(i));
            }
        }

        model.setParam("0_W", W);
        INDArray output = model.output(testing.getFeatures());

        eval = new Evaluation(2, 1);
        eval.eval(testing.getLabels(), output);
        double value = eval.f1();
        for (int i = 0; i < inputMask.size(0); i++) {
            if (inputMask.getInt(i) == 1) {
                value -= bias*histogram.getInt(i);
            }
        }
        if (eval.f1() > bestScore) {
            bestScore = eval.f1();
            accuracy = eval.accuracy();
            precision = eval.precision();
            recall = eval.recall();
            bestState = inputMask.dup();
        }

        //log.info(eval.stats());
        return eval.f1();
    }

    public void resetEquilibrated() {
        if (nSteps != 0) {
            equilibrationSteps = nSteps;
        }
        histogram = Nd4j.zeros(nInputs);
        total = 0;
        bestScore = 0;
        score = -1000;
    }

    public void runMC(int nSteps, boolean sample) {
        buffered = false;
        if (sample) {
            if (bufferFileName != null) {
                buffered = true;
                samples = Nd4j.zeros(bufferSize, nInputs);
            }
            else samples = Nd4j.zeros(nSteps, nInputs);
        }
        if (state == null) state = Nd4j.ones(nInputs);
        if (histogram == null) histogram = Nd4j.zeros(nInputs);
        mcSteps = nSteps;
        score = evaluate(state);
        INDArray oldState;
        long t0 = System.currentTimeMillis();
        for (int j = 0; j < nSteps; j++) {
            oldState = state.dup();
            for (int i = 0; i < nInputs; i++) {
                int[] index = {i};
                if (Nd4j.getRandom().nextDouble() < 0.005) state.putScalar(index, state.getInt(index) == 1 ? 0 : 1);
            }
            double newScore = evaluate(state);
            //if (j % 10 == 0) log.info("Proposed Score: {} at attempt {}", newScore, j);
            if (newScore > score || Nd4j.getRandom().nextDouble() < Math.exp(1000*(newScore-score))) {
                //accept move
                score = newScore;
                if (score > bestScore) bestScore = score;
            } else {
                //reject move
                state = oldState;
            }
            histogram.addi(state);
            if (sample && j % stateFrequency == 0) {
                if (buffered) samples.putRow(j % bufferSize / stateFrequency, state);
                else samples.putRow(j / stateFrequency, state);
            }
            if (j % 100 == 0) log.info("Score: {} at iteration {}", score, (j+1));
            if (j > 0 && j % (stateFrequency*bufferSize) == 0) {
                try {
                    DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(bufferFileName, true)));
                    Nd4j.write(samples, dos);
                    dos.close();
                    samples = Nd4j.zeros(bufferSize, nInputs);
                } catch (IOException e) {
                    log.info(e.getMessage());
                }
            }
        }
        long t1 = System.currentTimeMillis();
        log.info("Time (seconds): {} for nSteps: {}", ((t1-t0)/1000.00), nSteps);
        log.info("Best score = " + bestScore + " with accuracy = " + accuracy + " precision = " + precision + " recall = " + recall);
        log.info(state.toStringFull());
        total += nSteps;
        log.info(histogram.div(total).toStringFull());
    }

    /**
     * This is an implementation of the Wang and Landau algorithm for sampling flat histograms and determining the weights of inputs to much greater
     * accuracy than an unweighted histogram (the histogram variable for basic MC sampling).  However, convergence must be measured and running with
     * a small bias over a long time may be simpler than stepwise lowering of the bias functions, flatness criteria is approximated here as per standard
     * implementations of this algorithm to PMF or "free energy sampling".
     * @param nSteps Number of steps to run
     * @param bias Bias penalty to subtract for inputs occuring in the reduced feature set (you might try 0.00001 as an initial guess)
     */
    public void runWLMC(int nSteps, double bias, boolean sample) {
        buffered = false;
        if (sample) {
            if (bufferFileName != null) {
                buffered = true;
                samples = Nd4j.zeros(bufferSize, nInputs);
            }
            else samples = Nd4j.zeros(nSteps, nInputs);
        }
        if (state == null) state = Nd4j.ones(nInputs);
        if (histogram == null) histogram = Nd4j.zeros(nInputs);
        mcSteps = nSteps;
        this.bias = bias;
        if (score == -1000.0) score = evaluate(state, bias);
        INDArray oldState;
        long t0 = System.currentTimeMillis();
        for (int j = 0; j < nSteps; j++) {
            oldState = state.dup();
            for (int i = 0; i < nInputs; i++) {
                int[] index = {i};
                if (Nd4j.getRandom().nextDouble() < 0.005) state.putScalar(index, state.getInt(index) == 1 ? 0 : 1);
            }
            double newScore = evaluate(state, bias);
            if (j % 10 == 0) log.info("Proposed Score: " + newScore + " at attempt " + (j+1));
            if (newScore > score) {
                //accept move
                score = newScore;
                if (score > bestScore) bestScore = score;
            } else {
                //reject move
                state = oldState;
            }
            histogram.addi(state);
            if (j % 10 == 0) log.info("Score: " + score + " at iteration " + (j+1));

        }
        long t1 = System.currentTimeMillis();
        log.info("Time (seconds): " + ((t1-t0)/1000.00) + " for nSteps " + nSteps);
        log.info("Best score = " + bestScore + " with accuracy " + accuracy + " precision " + precision + " recall "+ recall);
        log.info(state.toStringFull());
        total += nSteps;
        log.info(histogram.div(total).toString());
    }

    public void runWLMC(int nSteps, double bias) {
        runWLMC(nSteps, bias, false);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setLabels(List<String> labels) {
        this.labels = labels;
    }

    public List<String> getLabels() {
        return labels;
    }

    public INDArray getState() {
        return state;
    }

    public void setState(INDArray state) {
        this.state = state;
    }

    public double getBestScore() {
        return bestScore;
    }

    public void setBestScore(double bestScore) {
        this.bestScore = bestScore;
    }

    public double getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(double accuracy) {
        this.accuracy = accuracy;
    }

    public double getPrecision() {
        return precision;
    }

    public void setPrecision(double precision) {
        this.precision = precision;
    }

    public double getRecall() {
        return recall;
    }

    public void setRecall(double recall) {
        this.recall = recall;
    }

    public INDArray getHistogram() {
        return histogram;
    }

    public void setHistogram(INDArray histogram) {
        this.histogram = histogram;
    }

    public INDArray getBestState() {
        return bestState;
    }

    public void setBestState(INDArray bestState) {
        this.bestState = bestState;
    }

    public MultiLayerConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(MultiLayerConfiguration configuration) {
        this.configuration = configuration;
    }

    public MultiLayerNetwork getModel() {
        return model;
    }

    public void setModel(MultiLayerNetwork model) {
        this.model = model;
    }

    public String getModelFileName() {
        return modelFileName;
    }

    public void setModelFileName(String modelFileName) {
        this.modelFileName = modelFileName;
    }

    public long getnInputs() {
        return nInputs;
    }

    public void setnInputs(long nInputs) {
        this.nInputs = nInputs;
    }

    public int getnSteps() {
        return nSteps;
    }

    public void setnSteps(int nSteps) {
        this.nSteps = nSteps;
    }

    public int getMcSteps() {
        return mcSteps;
    }

    public void setMcSteps(int mcSteps) {
        this.mcSteps = mcSteps;
    }

    public int getEquilibrationSteps() {
        return equilibrationSteps;
    }

    public void setEquilibrationSteps(int equilibrationSteps) {
        this.equilibrationSteps = equilibrationSteps;
    }

    public int getSeed() {
        return seed;
    }

    public void setSeed(int seed) {
        this.seed = seed;
    }

    public int getShuffleSeed() {
        return shuffleSeed;
    }

    public void setShuffleSeed(int shuffleSeed) {
        this.shuffleSeed = shuffleSeed;
    }

    public File getCsvFile() {
        return csvFile;
    }

    public void setCsvFile(File csvFile) {
        this.csvFile = csvFile;
    }

    public INDArray getSamples() {
        return samples;
    }

    public void setSamples(INDArray samples) {
        this.samples = samples;
    }

    public DataSet getTraining() {
        return training;
    }

    public void setTraining(DataSet training) {
        this.training = training;
    }

    public DataSet getTesting() {
        return testing;
    }

    public void setTesting(DataSet testing) {
        this.testing = testing;
    }

    public double getfTrain() {
        return fTrain;
    }

    public void setfTrain(double fTrain) {
        this.fTrain = fTrain;
    }

    public double getBias() {
        return bias;
    }

    public void setBias(double bias) {
        this.bias = bias;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    public boolean isBuffered() {
        return buffered;
    }

    public void setBuffered(boolean buffered) {
        this.buffered = buffered;
    }

    public String getBufferFileName() {
        return bufferFileName;
    }

    public void setBufferFileName(String bufferFileName) {
        this.bufferFileName = bufferFileName;
    }

    public int getStateFrequency() {
        return stateFrequency;
    }

    public void setStateFrequency(int stateFrequency) {
        this.stateFrequency = stateFrequency;
    }
}
