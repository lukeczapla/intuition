package org.magicat.intuition.util;
import org.apache.lucene.analysis.KeywordAnalyzer;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.spell.PlainTextDictionary;
import org.apache.lucene.search.spell.SpellChecker;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

@Component
public class SpellChecking {

    private SpellChecker spellChecker;
    Set<String> cache = new HashSet<>();

    public SpellChecking() {
        try {
            File folder = new File("Index");
            if (!folder.exists()) folder.mkdir();
            Directory directory = FSDirectory.open(folder);
            PlainTextDictionary dictionary = new PlainTextDictionary(new File("eng_dictionary.txt"));
            spellChecker = new SpellChecker(directory);
            spellChecker.indexDictionary(dictionary, new IndexWriterConfig(Version.LUCENE_36, new KeywordAnalyzer()), false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void reload() {
        try {
            File folder = new File("Index");
            if (!folder.exists()) folder.mkdir();
            Directory directory = FSDirectory.open(folder);
            PlainTextDictionary dictionary = new PlainTextDictionary(new File("eng_dictionary.txt"));
            spellChecker = new SpellChecker(directory);
            spellChecker.indexDictionary(dictionary, new IndexWriterConfig(Version.LUCENE_36, new KeywordAnalyzer()), false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void add(String word, boolean addToDictionaryFile) {
        if (addToDictionaryFile)
            try (FileWriter fileWriter = new FileWriter("eng_dictionary.txt", true);) {
                BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
                bufferedWriter.write(word);
                bufferedWriter.newLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
        cache.add(word);
    }

    public void add(String word) {
        try (FileWriter fileWriter = new FileWriter("eng_dictionary.txt", true);) {
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write(word);
            bufferedWriter.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        cache.add(word);
    }

    private String trim(String word) {
        word = word.trim();
        if (!Character.isAlphabetic(word.charAt(word.length()-1))) {
            return word.substring(0, word.length()-1);
        }
        return word;
    }

    public boolean checkSpelling(String word) {
        boolean result;
        try {
            result = spellChecker.exist(trim(word));
            if (result) return true;
            return cache.contains(word);
        } catch (IOException e) {
            e.printStackTrace();
            return cache.contains(word);
        }
    }

}
