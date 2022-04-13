package org.mskcc.knowledge.tests;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.biojava.bio.BioException;
import org.biojava.bio.seq.db.SequenceDB;
import org.biojava.bio.seq.io.SeqIOTools;
import org.biojava.bio.symbol.Alphabet;
import org.biojava.bio.symbol.AlphabetManager;
import org.biojavax.SimpleNamespace;
import org.biojavax.bio.SimpleBioEntry;
import org.biojavax.bio.seq.RichSequence;
import org.biojavax.bio.seq.RichSequenceIterator;
import org.biojavax.bio.seq.SimpleRichSequence;
import org.carrot2.attrs.AliasMapper;
import org.carrot2.attrs.Attrs;
import org.carrot2.clustering.Cluster;
import org.carrot2.clustering.Document;
import org.carrot2.clustering.kmeans.BisectingKMeansClusteringAlgorithm;
import org.carrot2.clustering.lingo.LingoClusteringAlgorithm;
import org.carrot2.clustering.stc.STCClusteringAlgorithm;
import org.carrot2.language.LanguageComponents;
import org.carrot2.math.matrix.FactorizationQuality;
import org.carrot2.math.matrix.LocalNonnegativeMatrixFactorizationFactory;
import org.junit.jupiter.api.Test;
import org.mskcc.knowledge.model.Article;
import org.mskcc.knowledge.model.xml.Item;
import org.mskcc.knowledge.model.xml.UpdateConfig;
import org.mskcc.knowledge.model.xml.UpdateItems;
import org.mskcc.knowledge.service.TextServiceImpl;
import org.mskcc.knowledge.util.*;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import static java.lang.Math.*;

/** This example shows how to tweak clustering algorithm parameters, prior to clustering. */
public class Test2 {


    @Test
    public void calculateMidbasis() {

    }

    @Test
    public void calculateBoostPMF() {
        String[] labels = {"D1", "D2", "X_phi", "X_psi", "D_phi", "D_psi", "F_phi", "F_psi", "D_chi1", "D_chi2", "F_chi1", "F_chi2"};
        String[] units = {"nm", "nm", "deg", "deg", "deg", "deg", "deg", "deg", "deg", "deg", "deg", "deg"};
        double total = 0;
        double min = 0, max = 0;
        List<Double> valuesV = new ArrayList<>();
        List<List<Double>> valuesCV = new ArrayList<>();
        int N = 0;
        for (int i = 3; i <= 3; i++) {
            String fileName = String.format("prod.out", i);
            try (Scanner scan = new Scanner(new File(fileName))) {
                while (scan.hasNextLine()) {
                    String line = scan.nextLine();
                    if (line.indexOf("deltaV:") == 0) {
                        String[] nums = line.substring(line.indexOf("[")+1).replace("]", "").split(", ");
                        for (String v : nums) valuesV.add(Double.parseDouble(v));
                    }
                    if (line.indexOf("colvars:") == 0) {
                        String[] samples = line.substring(line.indexOf("(")+1).replace("]", "").replaceAll("\\(|\\)", " ").split(" ,  ");
                        for (String s : samples) {
                            String[] nums = s.split(", ");
                            List<Double> CV = new ArrayList<>();
                            for (String v : nums) {
                                CV.add(Double.parseDouble(v));
                            }
                            valuesCV.add(CV);
                        }
                    }
                }
                PrintWriter writer = new PrintWriter("output_dV.txt");
                min = valuesV.get(0); max = valuesV.get(0);
                for (Double d : valuesV) {
                    total += d;
                    if (d > max) max = d;
                    if (d < min) min = d;
                    writer.println(d);
                }
            } catch (IOException e) {
                System.out.println("READ ERROR");
                return;
            }
        }
        System.out.printf("Average boost potential %f kJ/mol (%f kcal/mol)\n", total/valuesV.size(), total/(4.184*valuesV.size()));
        System.out.printf("Range is %f to %f kJ/mol (%f to %f kcal/mol)\n", min, max, min/4.184, max/4.184);
        double mean = total/valuesV.size();
        double sum = 0.0;
        for (int i = 0; i < valuesV.size(); i++) {
            sum += Math.pow(valuesV.get(i) - mean, 2.0);
            valuesV.set(i, valuesV.get(i)-mean);
        }
        sum /= (valuesV.size() - 1);
        System.out.printf("The standard deviation is %f kJ/mol (%f kcal/mol)\n", sqrt(sum), sqrt(sum)/4.184);
        System.out.printf("%d points were incorporated\n", valuesV.size());
        for (int i = 0; i < 12; i++) {
            total = 0; sum = 0;
            max = valuesCV.get(0).get(i);
            min = valuesCV.get(0).get(i);
            for (int j = 0; j < valuesCV.size(); j++) {
                double value = (i > 1 ? 180.0*valuesCV.get(j).get(i)/Math.PI : valuesCV.get(j).get(i));
                if (value > max) max = value;
                if (value < min) min = value;
                total += value;
            }
            System.out.printf("\nAverage %s is %f %s\n", labels[i], total/valuesCV.size(), units[i]);
            System.out.printf("Range of %s is %f %s to %f %s\n", labels[i], min, units[i], max, units[i]);
            mean = total/valuesCV.size();
            ArrayList<Double> sorted = new ArrayList<>();
            for (int j = 0; j < valuesCV.size(); j++) {
                sum += (i > 1 ? Math.pow(180.0*valuesCV.get(j).get(i)/Math.PI - mean, 2.0) : Math.pow(valuesCV.get(j).get(i) - mean, 2.0));
                sorted.add((i > 1 ? 180*valuesCV.get(j).get(i)/Math.PI : valuesCV.get(j).get(i)));
                if (i > 1) valuesCV.get(j).set(i, 180.0*valuesCV.get(j).get(i)/Math.PI);
            }
            sorted.sort(Double::compare);
            sum /= (valuesCV.size() - 1);
            System.out.printf("The standard deviation of %s is %f %s\n", labels[i], sqrt(sum), units[i]);
            System.out.printf("The median of %s is %f %s\n", labels[i], sorted.get(sorted.size()/2), units[i]);
            System.out.printf("%d points were incorporated\n", valuesCV.size());
        }
        // compute histograms and obtain PMFs, 20 bins(?) for distances, 36 bins for dihedral angles
        // TO-DO: write distances at the end, determine ranges, and make bins for D1 and D2 (i = {0,1})
        //for (int i = 0; i < 2; i++) {

        //}
        for (int i = 2; i < 12; i++) {
            ArrayList<Double> bins = new ArrayList<>(36);
            ArrayList<Integer> hbins = new ArrayList<>(36);
            for (int j = 0; j < 36; j++) {
                bins.add(0.0);
                hbins.add(0);
            }
            for (int j = 0; j < valuesCV.size(); j++) {
                double value = (valuesCV.get(j).get(i) < 0 ? 360+valuesCV.get(j).get(i) : valuesCV.get(j).get(i));
                int bin = ((int)value)/10;
                bins.set(bin, bins.get(bin)+Math.exp(valuesV.get(j)/(4.184*0.616)));
                hbins.set(bin, hbins.get(bin)+1);
            }
            List<Double> pmf = new ArrayList<>();
            min = -Math.log(bins.get(0));
            for (int j = 0; j < 36; j++) {
                double value = -Math.log(bins.get(j));
                pmf.add(value);
                if (value < min) min = value;
            }
            System.out.printf("\n%s free energy histogram:\n", labels[i]);
            for (int j = 18; j < 36; j++) {
                System.out.printf("%s %f kT  (%d samples)\n", (j*10+5 - 360), pmf.get(j) - min, hbins.get(j));
            }
            for (int j = 0; j < 18; j++) {
                System.out.printf("%s %f kT  (%d samples)\n", (j*10+5), pmf.get(j) - min, hbins.get(j));
            }
        }
    }

    @Test
    public void calculateMean() {
        double total = 0;
        double min = 0, max = 0;
        List<Double> values = new ArrayList<>();
        int N = 0;
        for (int i = 15; i <= 15; i++) {
            String fileName = String.format("deprot.out", i);
            try (Scanner scan = new Scanner(new File(fileName))) {
                while (scan.hasNextLine()) {
                    String line = scan.nextLine();
                    String[] tokens = line.split(" ");
                    if (tokens.length == 2 && tokens[1].equals("kJ/mol")) {
                        total += Double.parseDouble(tokens[0]);
                        N++;
                        values.add(Double.parseDouble(tokens[0]));
                    }
                }
                PrintWriter writer = new PrintWriter("output_dihedrals.txt");
                min = values.get(0); max = values.get(0);
                for (Double d : values) {
                    if (d > max) max = d;
                    if (d < min) min = d;
                    writer.println(d);
                }
            } catch (IOException e) {
                System.out.println("READ ERROR");
                return;
            }
        }
        System.out.printf("Average dihedral energy %f kJ/mol (%f kcal/mol)\n", total/N, total/(N*4.184));
        System.out.printf("Range is %f to %f kJ/mol (%f to %f kcal/mol)", min, max, min/4.184, max/4.184);
    }


    @Test
    public void calculateMeanBoost() {
        double total = 0;
        double min = 0, max = 0;
        List<Double> valuesD = new ArrayList<>();
        List<Double> valuesE = new ArrayList<>();
        int N = 0;
        for (int j = 21; j <= 21; j++) {
            String fileName = String.format("step5_%d.out", j);
            try (Scanner scan = new Scanner(new File(fileName))) {
                while (scan.hasNextLine()) {
                    String line = scan.nextLine();
                    if (line.indexOf("Dihedral E") == 0) {
                        valuesD.add(Double.parseDouble(line.split(" ")[2]));
                    } else if (line.indexOf("Effective E") == 0) {
                        valuesE.add(Double.parseDouble(line.split(" ")[2]));
                    }

                }
                PrintWriter writer = new PrintWriter("output_dihedral_boost.txt");
                for (int i = 0; i < valuesD.size(); i++) {
                    total += valuesE.get(i) - valuesD.get(i);
                    writer.println((valuesE.get(i) - valuesD.get(i)));
                    if (i == 0) {
                        min = valuesE.get(i) - valuesD.get(i);
                        max = valuesE.get(i) - valuesD.get(i);
                    } else {
                        if (valuesE.get(i) - valuesD.get(i) < min) min = valuesE.get(i) - valuesD.get(i);
                        if (valuesE.get(i) - valuesD.get(i) > max) max = valuesE.get(i) - valuesD.get(i);
                    }
                }
                //total /= valuesD.size();
            } catch (IOException e) {
                System.out.println("READ ERROR");
                return;
            }
        }
        System.out.printf("Average dihedral boost energy dV %f kJ/mol (%f kcal/mol)\n", total/valuesD.size(), total/(valuesD.size()*4.184));
        System.out.printf("Range is %f to %f kJ/mol (%f to %f kcal/mol)\n", min, max, min/4.184, max/4.184);
        double mean = total/valuesD.size();
        double sum = 0.0;
        for (int i = 0; i < valuesD.size(); i++) {
            sum += Math.pow(valuesE.get(i) - valuesD.get(i) - mean, 2.0);
        }
        sum /= (valuesD.size() - 1);
        System.out.printf("The standard deviation is %f kJ/mol (%f kcal/mol)\n", sqrt(sum), sqrt(sum)/4.184);
        System.out.printf("%d points where incorporated", valuesD.size());
    }


    @Test
    public void readJSONArticle() throws IOException {
        BufferedReader in = new BufferedReader(new FileReader("Articles.json"));
        Article a = Article.fromJSON(in.readLine());
        System.out.println(a);
    }

    @Test
    public void CurvesToDegrees() {
        int nsteps = 146, size=3486;
        String fileName = "1KX5.CURVE.coord";

        List<Double> values = new ArrayList<>();
        int line = 0;
        try (FileInputStream input = new FileInputStream(fileName)) {
            Scanner scan = new Scanner(input);
            do {
                values.add(Double.parseDouble(scan.nextLine()));
                values.add(Double.parseDouble(scan.nextLine()));
                values.add(Double.parseDouble(scan.nextLine()));
                values.add(Double.parseDouble(scan.nextLine()));
                values.add(Double.parseDouble(scan.nextLine()));
                values.add(Double.parseDouble(scan.nextLine()));
                line += 6;
                if (line + 6 > size) break;
                scan.nextLine();
                scan.nextLine();
                scan.nextLine();
                scan.nextLine();
                scan.nextLine();
                scan.nextLine();
                line += 6;
            } while (line + 6 <= size);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(values.size() + " total items");
        for (int i = 0; i < nsteps; i++) {
            double[] pairing = {values.get(12*i), values.get(12*i+1), values.get(12*i+2), values.get(12*i+3), values.get(12*i+4), values.get(12*i+5)};
            double[] steps;
            if (i == 0) steps = new double[] {0, 0, 0, 0, 0, 0};
            else steps = new double[] {values.get(12*i-6), values.get(12*i-5), values.get(12*i-4), values.get(12*i-3), values.get(12*i-2), values.get(12*i-1)};
            //System.out.printf("%f %f %f %f %f %f", pairing[3], pairing[4], pairing[5], 36.0/PI*pairing[0], 36.0/PI*pairing[1], 36.0/PI*pairing[2]);
            //System.out.printf("   %f %f %f %f %f %f\n", steps[3], steps[4], steps[5], 36.0/PI*steps[0], 36.0/PI*steps[1], 36.0/PI*steps[2]);
            double factor = scaling(steps[0], steps[1], steps[2]);
            double factor2 = scaling(pairing[0], pairing[1], pairing[2]);
            for (int j = 0; j < 3; j++) {
                steps[j] *= factor;
                pairing[j] *= factor2;
            }
            System.out.printf("%f %f %f %f %f %f", pairing[3], pairing[4], pairing[5], pairing[0], pairing[1], pairing[2]);
            System.out.printf("   %f %f %f %f %f %f\n", steps[3], steps[4], steps[5], steps[0], steps[1], steps[2]);

        }
    }

    double scaling(double x, double y, double z) {
        if (x == 0 && y == 0 && z == 0) return 0;
        double r = sqrt(x*x+y*y+z*z);
        return (36.0/PI)*10.0*atan(r/10.0)/r;
    }

    @Test
    public void convertInt() {
        System.out.println((int)Double.parseDouble("1.2346E7"));
    }

    @Test
    public void testAmino() {
        System.out.println(AminoAcids.buildExpression("VR-BRAF fusion"));
    }

    @Test
    public void testXMLUpdate() throws JAXBException {
        UpdateConfig uc = UpdateConfig.builder().genes(false).mutations(false)
                .cancers(true).drugs(true).page(1).pageSize(500000).build();
        JAXBContext jaxbContext = JAXBContext.newInstance(UpdateConfig.class);
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.marshal(uc, new File("UpdateConfig.xml"));
        marshaller.marshal(uc, System.out);
    }

    @Test
    public void testXMLUpdateInfo() throws JAXBException {
        UpdateItems ui = UpdateItems.builder().pageSize(500000).items(List.of(Item.builder().name("TP53").type("gene")
                .synonyms(Collections.singletonList("P53")).build(), Item.builder().name("q102e").type("mutation").build())).build();
        JAXBContext jaxbContext = JAXBContext.newInstance(UpdateItems.class);
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.marshal(ui, new File("UpdateItems.xml"));
        marshaller.marshal(ui, System.out);
    }

    @Test
    public void testRegex() {
        System.out.println(Pattern.matches("([\\W\\s^_]|^)(hello)([\\W\\s^_]|$)", ":hello;"));
    }

    @Test
    public void testNARegex() {
        Pattern p = Pattern.compile(NucleicAcids.nucleicAcidChain);
        Matcher m = p.matcher("this 5'-AACGATGGACT-3'");
        if (m.find()) {
            System.out.println("Starts at " + m.start());
        }
        m = p.matcher("this 3'-AACGATGGACT-5'");
        if (m.find()) {
            System.out.println("Starts at " + m.start());
        }
        System.out.println(Pattern.matches(NucleicAcids.nucleicAcidChain, "5'-AACGATGGACT-3'"));
        System.out.println(Pattern.matches(NucleicAcids.nucleicAcidChain, "3'-AACGATGGACT-5'"));
    }

    @Test
    public void checkEnvironment() {
        Map<String, String> env = System.getenv();
        for (String key : env.keySet()) {
            System.out.println(key + " : " + env.get(key));
        }
    }

    @Test
    public static Map<String, ArrayList<String>> tsvReader(File f) {
        Map<Integer, String> indexMap = new HashMap<>();
        Map<String, ArrayList<String>> result = new HashMap<>();
        try (BufferedReader TSVReader = new BufferedReader(new FileReader(f))) {
            String line = TSVReader.readLine();
            int i = 0;
            while ((line = TSVReader.readLine()) != null) {
                System.out.println(i++);
                String[] items = line.split("\t");
                ArrayList<String> item = result.get(items[5]);
                if (item == null) {
                    item = new ArrayList<>();
                    item.add(items[7]);
                }
                else item.add(items[7]);
                result.put(items[5], item);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    @Test
    void checkEscape() {
        // WORKS!
        System.out.println(SolrClientTool.escape("**E322*"));
        String text = "This, is an interesting -bit! So BRAF^^(V600E) and? Also G^^(67) are cool!! /Not much to see here V600Eadd/C/D...";
        String transformed = TextServiceImpl.textTransform(text, true);
        System.out.println(transformed);
        System.out.println("Test passed: " + (transformed.length() == text.length()));
    }

    @Test
    void aaTest() {
        AminoAcids.populate();
        System.out.println(AminoAcids.buildExpression("E255K"));
    }

    @Test
    public void testStr() {
        System.out.println(Pattern.compile("([^A-Za-z]{1,3}[0-9]{1,4}[^A-Za-z]{1,3})").matcher(" G12C ").find());
        System.out.println(Pattern.compile("([^A-Za-z]{1,3}[0-9]{1,4}[^A-Za-z]{1,3})").matcher(" 12 ").find());
    }

    @Test void testVariants() {
        File f = new File("allAnnotatedVariants.txt");
        Map<String, ArrayList<String>> indexMap = tsvReader(f);
        for (String gene : indexMap.keySet()) {
            System.out.println(gene);
        }
    }

    @Test
    void readArticle() throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader("test.json"));
        String json = reader.readLine();
        Article a = Article.fromJSON(json);
        System.out.println(InstrumentationAgent.getObjectSize(a));
    }

    @Test
    void readAllArticles() {
        List<Long> pmIds = new ArrayList<>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader("articles.json"));
            pmIds = reader.lines().map(Article::fromJSON).map(a -> Long.parseLong(a.getPmId())).collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(pmIds.size());
    }

    @Test
    public void tweakLingo() throws IOException {
        LanguageComponents languageComponents = LanguageComponents.loader().load().language("English");

        // Tweak Lingo's defaults. Note each attribute comes with JavaDoc documentation
        // and some are constrained to a specific range of values. Also, each algorithm
        // will typically have a different set of attributes to choose from.
        // fragment-start{parameters}
        LingoClusteringAlgorithm algorithm = new LingoClusteringAlgorithm();
        algorithm.desiredClusterCount.set(10);
        algorithm.preprocessing.wordDfThreshold.set(5);
        algorithm.preprocessing.phraseDfThreshold.set(5);
        algorithm.preprocessing.documentAssigner.minClusterSize.set(4);
        // fragment-end{parameters}

        // For attributes that are interfaces, provide concrete implementations of that
        // interface, configuring it separately. Programming editors provide support for listing
        // all interface implementations, use it to inspect the possibilities.
        // fragment-start{complex-parameters}
        var factorizationFactory = new LocalNonnegativeMatrixFactorizationFactory();
        factorizationFactory.factorizationQuality.set(FactorizationQuality.HIGH);

        algorithm.matrixReducer.factorizationFactory = factorizationFactory;
        // fragment-end{complex-parameters}

        List<Cluster<Document>> clusters =
                algorithm.cluster(ExamplesData.documentStream(), languageComponents);
        System.out.println("Clusters from Lingo:");
        ExamplesCommon.printClusters(clusters);
    }

    @Test
    public void patternTest() {
        AminoAcids.populate();
        System.out.println(AminoAcids.buildExpression("E255K"));
        //Graph g = TinkerGraph.open();

    }

    @Test
    public void testTika() {
        try {
            PrintWriter output = new PrintWriter(new FileOutputStream("output.txt"));
            String result = TikaTool.parseDocument("28947956.pdf");
            System.out.println(result);
            output.println(result);
        } catch (IOException e) {
        }
    }

    @Test
    public void tweakStc() throws IOException {
        LanguageComponents languageComponents = LanguageComponents.loader().load().language("English");

        // Tweak Lingo's defaults. Note each attribute comes with JavaDoc documentation
        // and some are constrained to a specific range of values. Also, each algorithm
        // will typically have a different set of attributes to choose from.
        STCClusteringAlgorithm algorithm = new STCClusteringAlgorithm();
        algorithm.maxClusters.set(10);
        algorithm.ignoreWordIfInHigherDocsPercent.set(.8);
        algorithm.preprocessing.wordDfThreshold.set(5);

        List<Cluster<Document>> clusters =
                algorithm.cluster(ExamplesData.documentStream(), languageComponents);
        System.out.println("Clusters from STC:");
        ExamplesCommon.printClusters(clusters);
    }


    @Test
    public void generateIds() {
        for (int i = 0; i < 10; i++) {
            System.out.println(SolrClientTool.randomId());
        }
    }

    @Test
    public void listAllAttributesToJson() {
        Stream.of(
                new LingoClusteringAlgorithm(),
                new STCClusteringAlgorithm(),
                new BisectingKMeansClusteringAlgorithm())
                .forEachOrdered(
                        algorithm -> {
                            System.out.printf(
                                    Locale.ROOT,
                                    "\n# Attributes of %s\n%s",
                                    algorithm.getClass().getSimpleName(),
                                    Attrs.toJson(algorithm, AliasMapper.SPI_DEFAULTS));
                        });
    }

    @Test
    public void walkPaths() {
        String pmid = "16858395";
        try (Stream<Path> walk = Files.walk(Paths.get("PMC/"+pmid))) {
            List<String> result = walk
                    .filter(p -> !Files.isDirectory(p))   // not a directory
                    .map(p -> p.toString().toLowerCase()) // convert path to string
                    .filter(f -> f.endsWith("pdf"))       // check end with
                    .collect(Collectors.toList());        // collect all matched to a List
            for (int i = 0; i < result.size(); i++)
                System.out.println(result.get(i));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}