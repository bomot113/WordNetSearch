/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.apache.lucene.analysis;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import net.didion.jwnl.JWNLException;
import net.didion.jwnl.dictionary.Dictionary;
import java.util.Arrays;

/**
 *
 * @author Bui Minh Tue
 */
public class VnWSDUtil extends WSDUtil {

    private static final String[] STOP_WORDS = {
        "no", "anh", "toi", "co"
    };

    public VnWSDUtil(String ConfigPath) {
        super(ConfigPath);
    }

    public VnWSDUtil(InputStream aStream) {
        super(aStream);
    }

    public VnWSDUtil(Dictionary aDict) {
        super(aDict);
    }

    protected boolean LineContainWord(String line, String word) {
        // the word can be uni-gram or bi-gram
        String normalizedLine = normalizeString(line);
        String normalizedWord = normalizeString(word);
        return normalizedLine.contains(normalizedWord);
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

    @Override
    protected String[] normalSplit(String line) throws JWNLException {
        List<String> result = new ArrayList<String>();
        String[] tempArray = normalizeString(line).split("_");
        List<String> stop_words = Arrays.asList(STOP_WORDS);
        //get all uni, bi-gram word from line
        String prefix = "";
        List<String> allVnWords = new ArrayList<String>();
        for (String aUniword : tempArray) {
            if (!aUniword.equals("|||")) {
                allVnWords.add(aUniword);
                if (!prefix.isEmpty()) {
                    allVnWords.add(String.format("%s_%s", prefix, aUniword));
                }
                prefix = aUniword;
            } else {
                prefix = "";
            }
        }
        for (String str : allVnWords) {
            if (!str.isEmpty()) {
                if (isNoun(str) && !stop_words.contains(str)) {
                    result.add(str);
                }
            }
        }
        return result.toArray(new String[result.size()]);

    }
}
