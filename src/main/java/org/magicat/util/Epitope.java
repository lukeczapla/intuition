package org.magicat.util;


import org.magicat.model.Antibody;
import org.magicat.repository.AntibodyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

@Service
public class Epitope {

    private static Logger log = LoggerFactory.getLogger(Epitope.class);
    private AntibodyRepository antibodyRepository;

    Map<String, Integer> letterCode = new HashMap<>();


    @Autowired
    public Epitope(AntibodyRepository antibodyRepository) {
        this.antibodyRepository = antibodyRepository;
        buildMap();
        //fire();
    }

    public void buildMap() {
        String letters = "ACDEFGHIKLMNPQRSTVWY";
        for (int i = 0; i < letters.length(); i++) {
            letterCode.put(letters.substring(i,i+1), i);
        }
    }

    //[ACDEFGHIKLMNPQRSTVWY]{8}[ACDEFGHIKLMNPQRSTVWY]*
    public Integer translateSequence(char A) {
        if (letterCode.get(A+"") == null) log.info("WTF? {}", A);
        return letterCode.get(A+"");
    }

    public int[][] bin(List<String> input, boolean leftLonger) {

        int[][] result = new int[20][4];
        /*for (int i = 0; i < 10; i++) {
            System.out.println(input.get(i));
        }*/
        input.parallelStream().forEach(s -> {
            int x = s.length() / 2 - 2;
            if (s.length() % 2 == 1 && leftLonger) {
                x++;
            }
            for (int i = x; i < x+4; i++) {
                synchronized(result) {
                    result[translateSequence(s.charAt(i))][i - x]++;
                }
            }
        });
        return result;
    }

    public int[][] average(int[][] bin1, int[][] bin2) {
        return IntStream.range(0, bin1.length).parallel()
                .mapToObj(i -> add(bin1[i], bin2[i])) // int[] is object
                .toArray(int[][]::new);
    }

    public int[] add(int[] a, int[] b) {
        return IntStream.range(0, a.length).parallel()
                .map(i -> a[i] + b[i])
                .toArray();
    }

    public int[] slidingWindowHistogram(List<String> input) {

        int[] result = new int[160000];

        input.parallelStream().forEach(s -> {

            for (int i = 2; i < s.length()-6; i++) {
                synchronized (result) {
                    result[translateSequence(s.charAt(i))*8000 + translateSequence(s.charAt(i+1))*400 + translateSequence(s.charAt(i+2))*20 + translateSequence(s.charAt(i+3))]++;
                }
            }
        });
        return result;
    }

    public int[][] slidingWindow(List<String> input) {

        int[][] result = new int[20][4];

        input.parallelStream().forEach(s -> {

            for (int i = 2; i < s.length()-6; i++) {
                for (int j = 0; j < 4; j++){
                    synchronized (result) {
                        result[translateSequence(s.charAt(i+j))][j]++;
                    }
                }
            }
        });
        return result;
    }


    public void fire() {
        List<Antibody> epitopes = antibodyRepository.getEpitope().stream().filter(a -> !a.getEpitope().contains("terminal His-Tag")).collect(Collectors.toList());
        log.info("We have {} epitopes", epitopes.size());
        /*epitopes.forEach(e -> {
            if (e.getEpitope().contains("-")) System.out.println(e.getEpitope().replaceAll(" ", "") + " " + e.getTargetSymbol().replaceAll(" ", ""));
        });*/
        List<Antibody> antigenSequences = antibodyRepository.getAntigenSequences();
        log.info("We have {} antigens", antigenSequences.size());
        log.info("let's use: {}", AminoAcids.aminoAcidSequence);
        Pattern p = Pattern.compile(AminoAcids.aminoAcidSequence);
        List<String> epitopeInput = epitopes.parallelStream().map(a -> a.getEpitope().replaceAll(" ", "")).flatMap(line -> StreamSupport.stream(new MatchItr(p.matcher(line)), true)).collect(Collectors.toList());
        log.info("number of separate epitopes, length {}", epitopeInput.size());
        List<String> antigenSequenceInput = antigenSequences.parallelStream().map(a -> a.getAntigenSequence().replaceAll(" ", "")).flatMap(line -> StreamSupport.stream(new MatchItr(p.matcher(line)), true)).collect(Collectors.toList());
        log.info("number of separate antigen sequences, length {}", antigenSequenceInput.size());
        //log.info("The items really add to {}")
        log.info("The items add to {}:", epitopes.stream().map(Antibody::getEpitope).flatMap(line -> StreamSupport.stream(new MatchItr(p.matcher(line)), true)).count());
        int[][] epitopeResult1 = bin(epitopeInput, true);
        int[][] epitopeResult2 = bin(epitopeInput, false);
        int[][] antigenResult1 = bin(antigenSequenceInput, true);
        int[][] antigenResult2 = bin(antigenSequenceInput, false);
        int[][] epitope = average(epitopeResult1, epitopeResult2);
        int[][] antigenSequence = average(antigenResult1, antigenResult2);

        int[][] epitopeWindow = slidingWindow(epitopeInput);
        int[][] antigenSequenceWindow = slidingWindow(antigenSequenceInput);

        try {
            PrintWriter out = new PrintWriter("composition.txt");
            PrintWriter out2 = new PrintWriter("tetramerHistograms.txt");
            double sum = 0, sum2 = 0;
            for (int i = 0; i < 4; i++) {
                sum = 0;
                for (int j = 0; j < 20; j++) {
                    sum += epitope[j][i]/2.0;
                    out.print(epitope[j][i]/2.0 + " ");
                }
                out.println(sum);
            }

            out.println();

            for (int i = 0; i < 4; i++) {
                sum2 = 0;
                for (int j = 0; j < 20; j++) {
                    sum2 += epitope[j][i]/(2.0*sum);
                    out.print(epitope[j][i]/(2.0*sum) + " ");
                }
                out.println(sum2);
            }

            out.println();

            for (int i = 0; i < 4; i++) {
                sum = 0;
                for (int j = 0; j < 20; j++) {
                    sum += epitopeWindow[j][i];
                    out.print(epitopeWindow[j][i] + " ");
                }
                out.println(sum);
            }

            out.println();

            for (int i = 0; i < 4; i++) {
                sum2 = 0;
                for (int j = 0; j < 20; j++) {
                    sum2 += epitopeWindow[j][i]/sum;
                    out.print(epitopeWindow[j][i]/sum + " ");
                }
                out.println(sum2);
            }


            out.println();

            int[] histogram = slidingWindowHistogram(epitopeInput);
            int sumH = 0;
            for (int i = 0; i < 160000; i++) {
                sumH += histogram[i];
                out2.print(histogram[i] + " ");
            }
            out2.println(sumH);

            out.println();
            out.println();

            for (int i = 0; i < 4; i++) {
                sum = 0;
                for (int j = 0; j < 20; j++) {
                    sum = sum + antigenSequence[j][i]/2.0;
                    out.print(antigenSequence[j][i]/2.0 + " ");
                }
                out.println(sum);
            }

            out.println();

            for (int i = 0; i < 4; i++) {
                sum2 = 0;
                for (int j = 0; j < 20; j++) {
                    sum2 = sum2 + antigenSequence[j][i]/(2.0*sum);
                    out.print(antigenSequence[j][i]/(2.0*sum) + " ");
                }
                out.println(sum2);
            }

            out.println();

            for (int i = 0; i < 4; i++) {
                sum = 0;
                for (int j = 0; j < 20; j++) {
                    sum += antigenSequenceWindow[j][i];
                    out.print(antigenSequenceWindow[j][i] + " ");
                }
                out.println(sum);
            }

            out.println();

            for (int i = 0; i < 4; i++) {
                sum2 = 0;
                for (int j = 0; j < 20; j++) {
                    sum2 += antigenSequenceWindow[j][i]/sum;
                    out.print(antigenSequenceWindow[j][i]/sum + " ");
                }
                out.println(sum2);
            }

            out.println();

            histogram = slidingWindowHistogram(antigenSequenceInput);
            sumH = 0;
            for (int i = 0; i < 160000; i++) {
                sumH += histogram[i];
                out2.print(histogram[i] + " ");
            }
            out2.println(sumH);

            int totalEpitope = epitopeInput.parallelStream().map(s -> s.length()-8).reduce(0, Integer::sum);
            int totalAntigen = antigenSequenceInput.parallelStream().map(s -> s.length()-8).reduce(0, Integer::sum);

            log.info("Total epitope windows {}, total antigen sequence windows {}", totalEpitope, totalAntigen);

            out.close();
            out2.close();

        } catch (IOException e) {
            log.error(e.getMessage());
            e.getStackTrace();
        }

    }

    final class MatchItr extends Spliterators.AbstractSpliterator<String> {
        private final Matcher matcher;
        MatchItr(Matcher m) {
            super(m.regionEnd()-m.regionStart(), ORDERED|NONNULL);
            matcher=m;
        }
        public boolean tryAdvance(Consumer<? super String> action) {
            if(!matcher.find()) return false;
            action.accept(matcher.group());
            return true;
        }
    }
}
