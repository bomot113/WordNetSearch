package org.apache.lucene.analysis;

import java.io.IOException;
import net.didion.jwnl.JWNLException;
import net.didion.jwnl.dictionary.*;
import net.didion.jwnl.data.*;

/**
 * Created by IntelliJ IDEA.
 * User: Bui Minh Tue
 * Date: Jul 29, 2009
 * Time: 1:05:58 PM
 * To change this template use File | Settings | File Templates.
 */
public final class WNWordFilter extends TokenFilter {

    public static final String[] STOP_WORDS = {
        "la", "co", "va", "voi", "nhung",
        "and", "are", "as", "at", "be", "but", "by",
        "for", "if", "in", "into", "is", "it",
        "no", "not", "of", "on", "or", "such",
        "that", "the", "their", "then", "there", "these",
        "they", "this", "to", "was", "will", "with"
    };
    Dictionary aDict;

    public WNWordFilter(TokenStream in, Dictionary WNDict, boolean ignoreCase) {
        super(in);
        this.aDict = WNDict;
    }

    public Token next(final Token reusableToken) throws IOException {
        assert reusableToken != null;
        // return the first non-stop word found
        for (Token nextToken = input.next(reusableToken); nextToken != null; nextToken = input.next(reusableToken)) {
        }
        // reached EOS -- return null
        return null;
    }
}
