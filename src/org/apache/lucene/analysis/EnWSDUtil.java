/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.apache.lucene.analysis;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import net.didion.jwnl.JWNLException;
import net.didion.jwnl.dictionary.Dictionary;

/**
 *
 * @author Bui Minh Tue
 */
public class EnWSDUtil extends WSDUtil {
    private static final String[] STOP_WORDS = {
        "and", "are", "as", "at", "be", "but", "by", "a", "an",
        "for", "if", "in", "into", "is", "it",
        "no", "not", "of", "on", "or", "such",
        "that", "the", "their", "then", "there", "these",
        "they", "this", "to", "was", "will", "with", "their", "her","his","its"
    };
    public EnWSDUtil(InputStream aStream) {
        super(aStream);
    }

    public EnWSDUtil(String ConfigPath) {
        super(ConfigPath);
    }

    public EnWSDUtil(Dictionary aDict) {
        super(aDict);
    }

    protected boolean LineContainWord(String line, String word) {
        // the word can be uni-gram or bi-gram
        String normalizedLine = normalizeString(line);
        String normalizedWord = normalizeString(word);
        String normalizedTransformedWord = normalizeString(NounTransformation(word));
        return normalizedLine.contains(normalizedWord) || normalizedLine.contains(normalizedTransformedWord);
    }
    // nomalize all white-space in string

    private String normalizeString(String aString) {
        String result = aString;
        result = result.replaceAll("[-_]", " ");
        result = result.replaceAll("[,.;!:]", " |||");
        result = result.replaceAll(" +", "_");
        result = result + "_";
        return result;
    }

    private String NounTransformation(String aNoun) {
        String result = "";
        String[] tails = {"s", "sh", "ch"};
        boolean check = false;
        for (String aStr : tails) {
            if (aNoun.endsWith(aStr)) {
                check = true;
            }
        }
        if (check) {
            result = aNoun + "es";
        } else {
            result = aNoun + "s";
        }
        return result;
    }

    private String NounDeTransformation(String aNoun) {
        if (aNoun.endsWith("es")) {
            return aNoun.substring(0, aNoun.length() - 2);
        } else if (aNoun.endsWith("s")) {
            return aNoun.substring(0, aNoun.length() - 1);
        } else {
            return null;
        }
    }
    @Override
    protected String[] normalSplit(String line) throws JWNLException {
        List<String> result = new ArrayList<String>();
        String[] tempArray = normalizeString(line).split("_");
        List<String> stop_words = Arrays.asList(STOP_WORDS);
        for (String str : tempArray) {
            if (!str.isEmpty()) {
                String temp = NounDeTransformation(str); // trailed 's or 'es
                if (isNoun(str) && !stop_words.contains(str)) {
                    result.add(str);
                } else if (temp != null) {
                    if (isNoun(temp) && !stop_words.contains(str)) {
                        result.add(temp);
                    }
                }
            }
        }
        return result.toArray(new String[result.size()]);
    }
}
