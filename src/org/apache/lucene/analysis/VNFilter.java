/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.apache.lucene.analysis;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Bui Minh Tue
 */
public class VNFilter extends TokenFilter {
    // Only English now, Chinese to be added later.

    public static final String[] STOP_WORDS = {
        "và", "là", "để", "của", "nên", "vì", "thì", "có"
    };
    private Map stopTable;

    public VNFilter(TokenStream in) {
        super(in);
        stopTable = new HashMap(STOP_WORDS.length);
        for (int i = 0; i < STOP_WORDS.length; i++)
            stopTable.put(STOP_WORDS[i], STOP_WORDS[i]);
    }

    @Override
    public Token next(Token reusableToken) throws IOException {
        assert reusableToken != null;

        for (Token nextToken = input.next(reusableToken); nextToken != null; nextToken = input.next(reusableToken)) {
            String text = nextToken.term();

            // why not key off token type here assuming ChineseTokenizer comes first?
            if (stopTable.get(text) == null) {
                return nextToken;
            }

        }
        return null;
    }
}
