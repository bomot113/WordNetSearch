/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.apache.lucene.TREC;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Bui Minh Tue
 */
public class RandomAccessTrecFile {

    static final int MAX_DOCUMENTS = 1000;
    RandomAccessFile randomFile;
    long currStartLine;
    long[] startLineArray = new long[MAX_DOCUMENTS];
    String filePath;
    public RandomAccessTrecFile(String filePath) throws IOException {
        this.filePath = filePath;
        for (int i = 0; i < MAX_DOCUMENTS; i++) {
            startLineArray[i] = -1;
        }
        openFile();
    }

    private String readUTFLine() throws IOException {
        int LENGTH_INIT = 1024;
        int buffLength = LENGTH_INIT;
        ByteBuffer charInput = ByteBuffer.allocate(buffLength);
        int c;
        int count = 0;
        currStartLine = randomFile.getFilePointer();
        while (((c = randomFile.read()) != -1) && c != '\n' && c != '\r') {
            count++;
            if (count == buffLength) {
                ByteBuffer temp = charInput;    // store old value
                buffLength += LENGTH_INIT;
                charInput = ByteBuffer.allocate(buffLength);
                charInput.put(temp.array());    // restore old value to new place
                charInput.position(count - 1);
            }
            charInput.put((byte) c);
        }
        if (c == -1){
            return null;
        }
        String UTFString = new String(charInput.array(), "UTF-8");
        UTFString = UTFString.trim();
        return UTFString;
    }

    private void TrecParse() throws IOException {
        String REGEX = "\\*TÀI LIỆU \\d+";
        Pattern regex = Pattern.compile(REGEX);
        Pattern num = Pattern.compile("\\d+");
        int docNum = 0;
        String line = null;
        while ((line = readUTFLine()) != null) {
            Matcher match = regex.matcher(line);
            if (match.find()) {
                String header = match.group();
                Matcher nummatch = num.matcher(header);
                if (nummatch.find()) {
                    String temp = nummatch.group();
                    docNum = Integer.parseInt(temp);
                    startLineArray[docNum] = currStartLine;
                }
            }
        }
    }

    public String getFileContent(int docNum) throws IOException {
        if ((docNum < 0) && (docNum >= MAX_DOCUMENTS) && (startLineArray[docNum] == -1)) {
            return null;
        }
        StringBuilder content = new StringBuilder();
        openFile();
        //seek file to the start offset of document
        randomFile.seek(startLineArray[docNum]);
        String line = null;
        while((startLineArray[docNum+1]!= -1)?
            randomFile.getFilePointer() != startLineArray[docNum+1]: endOfFile()){
            if ((line = readUTFLine())!= null){
                content.append(line + '\n');
            } else break;
        }
        close();
        return content.toString();
    }
    private boolean endOfFile() throws IOException {
        return randomFile.length() == randomFile.getFilePointer();
    }
    public void close() throws IOException {
        randomFile.close();
    }
    public void openFile() throws IOException {
        File aFile = new File(filePath);
        if (aFile.exists()) {
            randomFile = new RandomAccessFile(aFile, "r");
            TrecParse();
        }
    }
}
