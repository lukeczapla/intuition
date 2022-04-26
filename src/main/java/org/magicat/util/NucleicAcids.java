package org.magicat.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NucleicAcids {
    final static String jsonNA = "[{\"A\": [\"ADE\", Adenine\"]}, {\"C\": [\"CYT\", \"Cytosine\"]}, {\"T\" : [\"THY\", \"Thymine\"]}, {\"G\" : [\"GUA\", \"Guanine\"]}, {\"U\" : [\"URA\", \"Uracil\"]}]";
    public final static Map<String, List<String>> letterMap = new HashMap<>();
    public final static String letters = "ATGC";

    public static String nucleicAcidSequence = "[ATGC]{2}[ATGC]*";
    public static String nucleicAcidChain = "((5'-)" + nucleicAcidSequence + "(-3'))" + "|" + "((3'-)" + nucleicAcidSequence + "(-5'))";
    //public static String nucleicAcidChain2 = "(3'-)" + nucleicAcidSequence + "(-5')";

    public static void populate() {
        JsonArray data = JsonParser.parseString(jsonNA).getAsJsonArray();
        for (JsonElement e : data) {
            JsonObject j = e.getAsJsonObject();
            for (String key : j.keySet()) {
                JsonArray names = j.get(key).getAsJsonArray();
                List<String> list = new ArrayList<>(2);
                for (JsonElement e2: names) {
                    list.add(e2.getAsString());
                }
                letterMap.put(key, list);
            }
        }
    }
}
