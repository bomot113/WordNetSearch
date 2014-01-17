package org.apache.lucene.analysis;

import java.io.IOException;
import java.io.Reader;
import java.util.*;
import java.util.ArrayList;
import net.didion.jwnl.JWNLException;
import net.didion.jwnl.data.POS;
import net.didion.jwnl.data.Synset;

/**
 * An abstract base class for simple, character-oriented tokenizers.
 */
enum state {

    END_OF_LINE, END_OF_FILE, WORD_READING
};

public abstract class MyToken extends Tokenizer {

    private String punctuation = ";.!:";
    private int offset = 0, bufferIndex = 0, dataLen = 0;
    private static final int MAX_WORD_LEN = 255;
    private static final int IO_BUFFER_SIZE = 4096;
    private final char[] ioBuffer = new char[IO_BUFFER_SIZE];
    private Token previousToken = null;
    private Token remainOfBigram = null;
    private ArrayList<Token> currentTokenStream = new ArrayList<Token>();
    private int currentWordPosition = 0;
    public static final String TOKEN_TYPE_SYNONYM = "SYNONYM";
    private net.didion.jwnl.dictionary.Dictionary aDict;
    private WSDUtil aUtil;
    private boolean disambiguated;
    private state EOF;

    private void setState(state aState) {
        EOF = aState;
    }

    private state getState() {
        return EOF;
    }

    public MyToken(Reader input) {
        super(input);
    }

    public MyToken(Reader input, net.didion.jwnl.dictionary.Dictionary WNDict, Boolean Disambiguated) {
        super(input);
        aDict = WNDict;
        aUtil = new VnWSDUtil(aDict);
        disambiguated = Disambiguated;
    }

    /**
     * Returns true iff a character should be included in a token.  This
     * tokenizer generates as tokens adjacent sequences of characters which
     * satisfy this predicate.  Characters for which this is false are used to
     * define token boundaries and are not included in tokens.
     */
    protected abstract boolean isTokenChar(char c);

    /**
     * Called on each token character to normalize it before it is added to the
     * token.  The default implementation does nothing.  Subclasses may use this
     * to, e.g., lowercase tokens.
     */
    protected char normalize(char c) {
        return c;
    }

    public int getCurentWordPosition() {
        return currentWordPosition;
    }

    public final Token next(final Token reusableToken) throws IOException {
        if (currentTokenStream.size() == 0) {
            currentWordPosition = 0;
            currentTokenStream = (disambiguated) ? getNextProcessedSentenceStream() : getNextSentenceStream();
        }
        Token result;
        if (currentWordPosition == currentTokenStream.size()) {
            currentTokenStream = (disambiguated) ? getNextProcessedSentenceStream() : getNextSentenceStream();
            if (currentTokenStream.size() == 0) {
                if (getState() == state.END_OF_FILE) {
                    return null;
                } else {
                    return new Token();
                }
            }
            result = currentTokenStream.get(0);
            currentWordPosition = 1;
            copyToken(result, reusableToken);
            return result;
        } else {
            result = currentTokenStream.get(currentWordPosition);
            currentWordPosition++;
            copyToken(result, reusableToken);
            return result;
        }
    }

    private ArrayList<Token> getNextSentenceStream() throws IOException {
        previousToken = null;
        remainOfBigram = null;
        ArrayList<Token> result = new ArrayList<Token>();
        Token currentToken = new Token();
        setState(state.WORD_READING);
//        while ((currState = getNextToken(currentToken)) == state.WORD_READING) {
//            result.add(currentToken);
//            currentToken = new Token();
//        }
//        if (currState == state.END_OF_LINE){
//            result.add(currentToken);
//            if (remainOfBigram != null)
//                result.add(remainOfBigram);
//        }
        while (getState() != state.END_OF_FILE) {
            if (!currentToken.term().isEmpty()) {
                result.add(currentToken);
            }
            currentToken = new Token();
            setState(getNextToken(currentToken));
            if (getState() == state.END_OF_LINE) {
                if (currentToken.term().isEmpty()) {
                    continue;
                } else {
                    result.add(currentToken);
                    if (remainOfBigram != null) {
                        result.add(remainOfBigram);
                    }
                    break;
                }
            }
        }
        return result;
    }

    // in this process, divide words in stream into 2 sub-groups
    // one group contains all wordnet words used for WSD
    // the second contains normal term, used for normal queries
    private ArrayList<Token> getNextProcessedSentenceStream() throws IOException {
        ArrayList<Token> StreamWords = getNextSentenceStream();
        List<Token> WordnetWords = new ArrayList<Token>();
        ArrayList<Token> result = new ArrayList<Token>();
        //get all wordnet words included in the sentence
        for (Token aToken : StreamWords) {
            String aNoun = aToken.term().trim();
            try {
                if (!aNoun.isEmpty()) {
                    if (aDict.getIndexWord(POS.NOUN, aNoun) != null) {
                        WordnetWords.add(aToken);
                    }
                }
                // only add wordnet words, that is business
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }   // result has already contained terms not included in WN database

        //tranverse all these words, then make wordsense-disambiguations
        for (int i = 0; i < WordnetWords.size(); i++) {
            ArrayList<String> senses = new ArrayList<String>();
            Token currToken = WordnetWords.get(i);
            if (WordnetWords.size() > 1) {
                for (int j = 0; j < WordnetWords.size(); j++) {
                    if (j != i) {
                        try {
                            Synset aSense = aUtil.getSense(currToken.term(), WordnetWords.get(j).term());
                            if (aSense == null) {
                                continue;
                            }
                            String identity = aUtil.getSenseIndentity(aSense);
                            if (senses.indexOf(identity) == -1) {
                                senses.add(identity);
                            }
                        } catch (JWNLException ex) {
                            ex.printStackTrace();
                        }
                    }
                }
                // after disambiguation if there's no suitable sense
                // do not add any thing ! so important in here, that's business

                // transform all found senses into token
                if (senses.size() == 0) {
                    result.add(currToken);
                }
                for (int j = 0; j < senses.size(); j++) {
                    Token aToken = new Token(senses.get(j), currToken.startOffset(),
                            currToken.endOffset(), TOKEN_TYPE_SYNONYM);
                    aToken.setPositionIncrement(0);
                    if (j == 0) {
                        aToken.setPositionIncrement(1);
                        aToken.setType(Token.DEFAULT_TYPE);
                    }
                    result.add(aToken);
                }
            } else if (WordnetWords.size() == 1) {
                // donot add senses, just add term
                result.add(currToken);
            }
        }
        return result;
    }

    private state getNextToken(final Token nextToken) throws IOException {
        assert nextToken != null;
        state result = state.WORD_READING;
        nextToken.clear();
        Token reusableToken = new Token();
        reusableToken.clear();
        if (remainOfBigram != null) {
            copyToken(remainOfBigram, nextToken);
            copyToken(remainOfBigram, previousToken);
            remainOfBigram = null;
            return state.WORD_READING;
        }

        int length = 0;
        int start = bufferIndex;
        char[] buffer = reusableToken.termBuffer();
        while (true) {
            if (bufferIndex >= dataLen) {
                offset += dataLen;
                dataLen = input.read(ioBuffer);
                if (dataLen == -1) {
                    if (length > 0) {
                        break;
                    } else {
                        return state.END_OF_FILE;
                    }
                }
                bufferIndex = 0;
            }

            char c = ioBuffer[bufferIndex++];
            if (c == '\r' || c == '\n') {
                c = ' ';
            }
            //assumed new line separator is a ' '
            if (punctuation.indexOf(c) != -1) {
                result = state.END_OF_LINE;
                break;
            } else if (isTokenChar(c)) {               // if it's a token char satisfied our language
                if (length == 0) // start of token
                {
                    start = offset + bufferIndex - 1;
                } else if (length == buffer.length) {
                    buffer = reusableToken.resizeTermBuffer(1 + length);
                }

                buffer[length++] = normalize(c); // buffer it, normalized

                if (length == MAX_WORD_LEN) // buffer overflow!
                {
                    break;
                }

            } else if (length > 0) // at non-Letter w/ chars
            {
                break;                           // return 'em
            }
        }
        reusableToken.setTermLength(length);
        reusableToken.setStartOffset(start);
        reusableToken.setEndOffset(start + length);
        // after creating uni-gram token then bi-gram token is prepair for next move
        if (previousToken == null || previousToken.term().isEmpty()) {
            previousToken = new Token();
            previousToken.clear();
            copyToken(reusableToken, previousToken);
            copyToken(reusableToken, nextToken);
            return result;
        }
        Token bigramToken = new Token();
        bigramToken.clear();

        String tempStr = previousToken.term();
        tempStr += "_" + reusableToken.term();
        setTermString(bigramToken, tempStr);
        bigramToken.setStartOffset(previousToken.startOffset());
        bigramToken.setEndOffset(reusableToken.endOffset());
        bigramToken.setTermLength(tempStr.length());
        remainOfBigram = reusableToken;
        copyToken(bigramToken, nextToken);
        return result;
    }

    private void setTermString(Token aToken, String aStr) {
        aToken.clear();
        aToken.resizeTermBuffer(aStr.length());
        char[] buff = aToken.termBuffer();
        for (int i = 0; i < aStr.length(); i++) {
            buff[i] = aStr.charAt(i);
        }
    }

    private void copyToken(Token source, Token dest) {
        setTermString(dest, source.term());
        dest.setTermLength(source.termLength());
        dest.setStartOffset(source.startOffset());
        dest.setEndOffset(source.endOffset());
    }

    public void reset(Reader input) throws IOException {
        super.reset(input);
        bufferIndex = 0;
        offset = 0;
        dataLen = 0;
    }
}
