package org.apache.lucene.analysis.ngram;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;

import java.io.Reader;

public class NGramAnalyzer extends Analyzer {
    public NGramAnalyzer (){
    }
    public TokenStream tokenStream(String fieldName, Reader reader) {
        TokenStream result = new NGramTokenizer(reader);
        return result;  //To change body of implemented methods use File | Settings | File Templates.
    }
}