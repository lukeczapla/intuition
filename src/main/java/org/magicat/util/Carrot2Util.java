package org.magicat.util;

import lombok.extern.slf4j.Slf4j;
import org.carrot2.clustering.Cluster;
import org.carrot2.clustering.Document;
import org.carrot2.clustering.stc.STCClusteringAlgorithm;
import org.carrot2.language.LanguageComponents;
import org.magicat.model.Article;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Slf4j
public class Carrot2Util {

    private static void runConcurrentClustering(List<Article> articles, Function<Stream<Document>, List<Cluster<Document>>> processor)
            throws InterruptedException, ExecutionException {
        // Let's say we have 50 clustering requests to process with all available CPU cores (in the
        // default fork-join pool).
        Collection<Callable<List<Cluster<Document>>>> tasks = IntStream.range(0, 10)
        .mapToObj(ord -> (Callable<List<Cluster<Document>>>)() -> {
            // Shuffle input documents for each request.
            Collections.shuffle(articles, new Random(ord));

            Stream<Document> documentStream = articles.stream().filter(item -> item.getTitle() != null &&
                    item.getFulltext() != null && item.getPubAbstract() != null).map(item ->
                    (itemVisitor) -> {
                        itemVisitor.accept("title", item.getTitle());
                        itemVisitor.accept("content", item.getPubAbstract()+"\n"+item.getFulltext());
                        itemVisitor.accept("id", item.getPmId());
                    });

            long start = System.nanoTime();
            List<Cluster<Document>> clusters = processor.apply(documentStream);
            long end = System.nanoTime();
            System.out.println(String.format(Locale.ROOT,"Done clustering request: %d [%.2f sec.], %d cluster(s)", ord, (end - start) / (double) TimeUnit.SECONDS.toNanos(1), clusters.size()));
            return clusters;
        }).collect(Collectors.toList());

        ExecutorService service = ForkJoinPool.commonPool();
        for (Future<List<Cluster<Document>>> future : service.invokeAll(tasks)) {
            // Consume the output of all tasks.
            List<Cluster<Document>> value = future.get();
        }
    }

    public static void runConcurrentProcess(List<Article> articles) {
        LanguageComponents english;
        try {
            english = LanguageComponents.loader().load().language("English");
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        Function<Stream<Document>, List<Cluster<Document>>> processor =
                (documentStream) -> {
                    STCClusteringAlgorithm algorithm = new STCClusteringAlgorithm();
                    algorithm.maxClusters.set(16);
                    algorithm.ignoreWordIfInHigherDocsPercent.set(.8);
                    algorithm.preprocessing.wordDfThreshold.set(10);
                    return algorithm.cluster(documentStream, english);
                };
        try {
            runConcurrentClustering(articles, processor);
        } catch (InterruptedException|ExecutionException e) {
            e.printStackTrace();
        }
    }

    public static String splitKeywordsMeshTerms(Article item) {
        String keywords = item.getKeywords();
        String meshTerms = item.getMeshTerms();
        String result = "";
        if (keywords != null) {
            String[] kwarray = keywords.split(";");
            for (String kw : kwarray) {
                String token;
                if (kw.split(":").length > 1) token = kw.split(":")[1];
                else token = kw;
                if (result.equals("")) result += token;
                else result += ", " + token;
            }
        }
        if (meshTerms == null || meshTerms.equals("")) return result;
        String[] meshArray = meshTerms.split(";");
        for (String mesh: meshArray) {
            String token = "";
            if (mesh.split(":").length > 2 && mesh.split(":")[1].equals("Y")) {
                token = mesh.split(":")[2];
            }
 //           else if (mesh.split(":").length > 1) token = mesh.split(":")[1];
//            else token = mesh;
            if (result.equals("")) result += token;
            else result += ", " + token;

        }
        //System.out.println(result);
        return result;

    }

    public static List<Cluster<Document>> carrot2ClusterArticles(List<Article> articles) {

        LanguageComponents languageComponents;
        try {
            languageComponents = LanguageComponents.loader().load().language("English");
        } catch (IOException e) {
            log.error("Cannot load English language component: {}", e.getMessage());
            return null;
        }
        log.info("Language: {}", languageComponents.components());

        STCClusteringAlgorithm algorithm = new STCClusteringAlgorithm();
        algorithm.maxClusters.set(30);
        algorithm.maxBaseClusters.set(150);
        algorithm.maxPhrasesPerLabel.set(3);
        algorithm.maxPhraseOverlap.set(0.3);
        algorithm.maxPhrasesPerLabel.set(3);
        algorithm.maxWordsPerLabel.set(4);
        algorithm.mergeThreshold.set(0.6);
        algorithm.singleTermBoost.set(0.5);
        algorithm.minBaseClusterScore.set(2.0);
        algorithm.minBaseClusterSize.set(2);
        algorithm.documentCountBoost.set(1.0);
        algorithm.mostGeneralPhraseCoverage.set(0.5);
        algorithm.ignoreWordIfInHigherDocsPercent.set(0.9);
        algorithm.optimalPhraseLength.set(3);
        algorithm.queryHint.set("BRAF");
        algorithm.optimalPhraseLengthDev.set(2.0);
        algorithm.preprocessing.wordDfThreshold.set(1);
        algorithm.scoreWeight.set(0.0);
        algorithm.mergeThreshold.set(0.6);
        algorithm.mergeStemEquivalentBaseClusters.set(true);

        /*
        LingoClusteringAlgorithm algorithm = new LingoClusteringAlgorithm();
        algorithm.desiredClusterCount.set(10);
        algorithm.preprocessing.wordDfThreshold.set(5);
        algorithm.preprocessing.phraseDfThreshold.set(5);
        algorithm.preprocessing.documentAssigner.minClusterSize.set(4);
        */

        Stream<Document> documentStream = articles.stream().filter(item -> item.getTitle() != null && item.getPubAbstract() != null &&
                item.getFulltext() != null).map(item ->
                (itemVisitor) -> {
                    itemVisitor.accept("title", item.getTitle());
                    itemVisitor.accept("content", item.getPubAbstract() + "\n" + item.getFulltext());
                    itemVisitor.accept("id", item.getPmId());
                });

        log.info("Total articles to cluster: " + articles.stream().filter(item -> item.getTitle() != null &&
                item.getPubAbstract() != null || item.getFulltext() != null).count());

        List<Cluster<Document>> clusters;

        clusters = algorithm.cluster(documentStream, languageComponents);

        log.info("Items: " + clusters.size());
        for (Cluster<Document> c: clusters) {
            log.info(""+c);
            for (int i = 0; i < c.getDocuments().size(); i++) {
                c.getDocuments().get(i).visitFields((s, s2) -> {
                    if (s.equals("id")) log.info(s2+",");
                });
            }
            System.out.println();
        }
        log.info("SHORT ANSWER WITHOUT PMIDs:");
        printClusters(clusters);

        return clusters;
    }

    public static List<Cluster<Document>> carrot2Cluster(List<Article> articles) {
        LanguageComponents languageComponents;
        try {
            languageComponents = LanguageComponents.loader().load().language("English");
        } catch (IOException e) {
            return null;
        }
        STCClusteringAlgorithm algorithm = new STCClusteringAlgorithm();
/*        algorithm.maxClusters.set(30);
        algorithm.maxBaseClusters.set(150);
        algorithm.maxPhrasesPerLabel.set(3);
        algorithm.maxPhraseOverlap.set(0.3);
        algorithm.maxPhrasesPerLabel.set(3);
        algorithm.maxWordsPerLabel.set(4);
        algorithm.mergeThreshold.set(0.6);
        algorithm.singleTermBoost.set(0.5);
        algorithm.minBaseClusterScore.set(2.0);
        algorithm.minBaseClusterSize.set(2);
        algorithm.documentCountBoost.set(1.0);
        algorithm.mostGeneralPhraseCoverage.set(0.5);
        algorithm.ignoreWordIfInHigherDocsPercent.set(0.9);
        algorithm.optimalPhraseLength.set(3);
        algorithm.queryHint.set("BRAF");
        algorithm.optimalPhraseLengthDev.set(2.0);
        algorithm.preprocessing.wordDfThreshold.set(1);
        algorithm.scoreWeight.set(0.0);
        algorithm.mergeThreshold.set(0.6);
        algorithm.mergeStemEquivalentBaseClusters.set(true);*/
        //algorithm.maxBaseClusters.set(150);
        //algorithm.minBaseClusterSize.set(10);
        algorithm.maxClusters.set(16);
        algorithm.ignoreWordIfInHigherDocsPercent.set(0.85);
        algorithm.preprocessing.wordDfThreshold.set(5);

        /*
        LingoClusteringAlgorithm algorithm = new LingoClusteringAlgorithm();
        algorithm.desiredClusterCount.set(10);
        algorithm.preprocessing.wordDfThreshold.set(5);
        algorithm.preprocessing.phraseDfThreshold.set(5);
        algorithm.preprocessing.documentAssigner.minClusterSize.set(4);
        */

        Stream<Document> documentStream = articles.stream().filter(item -> item.getTitle() != null).map(item ->
                (itemVisitor) -> {
                    itemVisitor.accept("URL", "https://aimlcoe.mskcc.org/knowledge/getPDF/" + item.getPmId() + ".pdf");
                    itemVisitor.accept("title", item.getTitle());
                    itemVisitor.accept("snippet", item.getPubAbstract() + (item.getFulltext() != null ? "\n\n"+item.getFulltext():""));//Article.toText(item));

                });

        log.info("Total articles to cluster: " + articles.stream().filter(item -> item.getTitle() != null ||
                (item.getPubAbstract() != null && item.getFulltext() != null)).count());

        List<Cluster<Document>> clusters;

        clusters = algorithm.cluster(documentStream, languageComponents);

        log.info("Items: " + clusters.size());
        for (Cluster<Document> c: clusters) {
            System.out.println(c.toString());
            for (int i = 0; i < c.getDocuments().size(); i++) {
                final int val = i;
                c.getDocuments().get(i).visitFields((s, s2) -> {
                    //if (s.equals("title") && val < c.getDocuments().size()-1) System.out.print(s2+", ");
                    if (s.equals("URL") && val == c.getDocuments().size()-1) System.out.println(s2);
                });
            }
            System.out.println();
        }
        log.info("SHORT ANSWER WITHOUT PMIDs:");
        printClusters(clusters);

        return clusters;
    }


    public static <T> void printClusters(List<Cluster<T>> clusters) {
        printClusters(clusters, "");
    }

    private static <T> void printClusters(List<Cluster<T>> clusters, String indent) {
        for (Cluster<T> c : clusters) {
            log.info(indent + c);
            printClusters(c.getClusters(), indent + "  ");
        }
    }

}
