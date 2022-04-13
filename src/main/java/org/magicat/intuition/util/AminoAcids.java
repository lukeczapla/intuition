package org.mskcc.knowledge.util;

import com.google.gson.*;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AminoAcids {
    final static String jsonAA = "[{\"A\": [\"Ala\", Alanine\"]}, {\"C\": [\"Cys\", \"Cysteine\"]}, {\"D\" : [\"Asp\", \"Aspartic Acid/Aspartate\"]}, {\"E\" : [\"Glu\", \"Glutamic Acid/Glutamate\"]}, " +
            "{\"F\" : [\"Phe\", \"Phenylalanine\"]}, {\"G\" : [\"Gly\", \"Glycine\"]}, {\"H\": [\"His\", \"Histidine\"]}, {\"I\": [\"Ile\", \"Isoleucine\"]}, {\"K\": [\"Lys\", \"Lysine\"]}, {\"L\": [\"Leu\", \"Leucine\"]}, {\"M\" : [\"Met\", \"Methionine\"]}, " +
            "{\"N\": [\"Asn\", \"Asparagine\"]}, {\"P\": [\"Pro\", \"Proline\"]}, {\"Q\": [\"Gln\", \"Glutamine\"]}, {\"R\": [\"Arg\", \"Arginine\"]}, {\"S\": [\"Ser\", \"Serine\"]}, {\"T\": [\"Thr\", \"Threonine\"]}, " +
            "{\"V\" : [\"Val\", \"Valine\"]}, {\"W\": [\"Trp\", \"Tryptophan\"]}, {\"Y\": [\"Tyr\", \"Tyrosine\"]}]";
    public final static Map<String, List<String>> letterMap = new HashMap<>();
    public final static Map<String, String> codeToLetter = new HashMap<>();
    public final static String letters = "ACDEFGHIKLMNPQRSTVWY";

    public static Pattern pattern;
    public static Matcher matcher;

    public static String aminoAcidSequence = "[ACDEFGHIKLMNPQRSTVWY]{8}[ACDEFGHIKLMNPQRSTVWY]*";
    public static String missenseMutation = "\\b[ACDEFGHIKLMNPQRSTVWY][1-9]\\d{0,4}[ACDEFGHIKLMNPQRSTVWY]\\b";


    public static String buildExpression(String expression) {
        if (letterMap.size() == 0) populate();
        expression = expression.trim();
        String result1 = "[";
        for (int i = 0; i < letters.length(); i++) result1 += letters.charAt(i);
        result1 += "]";
        String result = result1 + "\\d{1,4}" + result1;
        pattern = Pattern.compile(result);
        matcher = pattern.matcher(expression);
        if (matcher.find() && matcher.end()-matcher.start() == expression.length() && letterMap.get(expression.substring(0, 1)) != null && letterMap.get(expression.substring(expression.length()-1)) != null) {
            return (letterMap.get(expression.substring(0, 1)).get(0) + expression.substring(1, expression.length()-1)
                    + letterMap.get(expression.substring(expression.length()-1)).get(0));
        } else {
            return buildExpressionLong(expression);
        }
    }

    public static String buildExpressionLong(String expression) {
        if (letterMap.size() == 0) populate();
        expression = expression.trim();
        String result1 = "[";
        for (int i = 0; i < letters.length(); i++) result1 += letters.charAt(i);
        result1 += "]";
        String result = result1 + "\\d{1,4}";
        pattern = Pattern.compile(result);
        matcher = pattern.matcher(expression);
        if (!expression.contains("-") && matcher.find() && matcher.start() == 0) {
            expression = letterMap.get(expression.substring(0, 1)).get(0) + expression.substring(1);
            matcher = pattern.matcher(expression);
            while (matcher.find()) {
                expression = expression.substring(0, matcher.start()) + letterMap.get(expression.substring(matcher.start(), matcher.start()+1)).get(0) + expression.substring(matcher.start()+1);
                matcher = pattern.matcher(expression);
            }
            return expression;
        } else if (expression.toLowerCase().endsWith(" fusion")) {
            expression = expression.toLowerCase().substring(0, expression.toLowerCase().indexOf(" fusion"));
        } else if (expression.toLowerCase().endsWith("fusions")) {
            return expression.toLowerCase();
        }
        if (expression.contains("-") && expression.indexOf("-") == expression.lastIndexOf("-")) {
            return (expression.substring(expression.indexOf("-")+1) + "-" + expression.substring(0, expression.indexOf("-"))).toUpperCase();
        }
        return expression;
    }

    public static void populate() {
        JsonArray data = JsonParser.parseString(jsonAA).getAsJsonArray();
        for (JsonElement e : data) {
            JsonObject j = e.getAsJsonObject();
            for (String key : j.keySet()) {
                JsonArray names = j.get(key).getAsJsonArray();
                List<String> list = new ArrayList<>(2);
                String codeKey = "";
                for (JsonElement e2: names) {
                    if (e2.getAsString().length() == 3) codeKey = e2.getAsString().toUpperCase();
                    list.add(e2.getAsString());
                }
                letterMap.put(key, list);
                codeToLetter.put(codeKey, key);
            }
        }
    }


    public static String mutationSynonym(String expression) {
        if (letterMap.size() == 0) populate();
        expression = expression.trim();
        String result1 = "[";
        for (int i = 0; i < letters.length(); i++) result1 += letters.charAt(i);
        result1 += "]";
        String result = result1 + "\\d{1,4}" + result1;   // examples like G12C to Gly12Cys
        pattern = Pattern.compile(result);
        matcher = pattern.matcher(expression);
        if (matcher.find() && matcher.end()-matcher.start() == expression.length() && letterMap.get(expression.substring(0, 1)) != null && letterMap.get(expression.substring(expression.length()-1)) != null) {
            return letterMap.get(expression.substring(0, 1)).get(0) + expression.substring(1, expression.length()-1)
                    + letterMap.get(expression.substring(expression.length()-1)).get(0);
        } else {
            return mutationSynonym2(expression);
        }
    }

    public static String mutationSynonym2(String expression) {
        if (letterMap.size() == 0) populate();
        expression = expression.trim();
        String result1 = "[";
        for (int i = 0; i < letters.length(); i++) result1 += letters.charAt(i);
        result1 += "]";
        String result = result1 + "\\d{1,4}";   // expressions like V240
        pattern = Pattern.compile(result);
        matcher = pattern.matcher(expression);
        if (!expression.contains("-") && matcher.find() && matcher.start() == 0) {
            expression = letterMap.get(expression.substring(0, 1)).get(0) + expression.substring(1);
            matcher = pattern.matcher(expression);
            while (matcher.find()) {
                expression = expression.substring(0, matcher.start()) + letterMap.get(expression.substring(matcher.start(), matcher.start()+1)).get(0) + expression.substring(matcher.start()+1);
                matcher = pattern.matcher(expression);
            }
            return expression;
        } else if (expression.toLowerCase().endsWith(" fusion")) {
            expression = expression.substring(0, expression.toLowerCase().indexOf(" fusion")).toUpperCase();
        } else if (expression.toLowerCase().endsWith("fusions")) {
            return expression;
        }
        if (expression.contains("-") && expression.indexOf("-") == expression.lastIndexOf("-")) {
            return expression.substring(expression.indexOf("-")+1) + "-" + expression.substring(0, expression.indexOf("-"));
        }
        return expression;
    }

    // generate all possible hotspot (e.g. Q70) substitutions like Q70A, Q70C, Q70D, etc...
    public static List<String> hotspotSubstitution(String expression) {
        String result1 = "[";
        for (int i = 0; i < letters.length(); i++) result1 += letters.charAt(i);
        result1 += "]\\d{1,4}";
        pattern = Pattern.compile(result1);
        matcher = pattern.matcher(expression);
        if (matcher.find() && matcher.start() == 0 && matcher.end() == expression.length()) {
            List<String> result = new ArrayList<>();
            for (int i = 0; i < letters.length(); i++) {
                result.add(expression + letters.charAt(i));
                result.add(mutationSynonym(expression + letters.charAt(i)));
            }
            return result;
        }
        return null;
    }


}
