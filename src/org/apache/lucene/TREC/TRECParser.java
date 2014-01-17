/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.apache.lucene.TREC;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Bui Minh Tue
 */
public class TRECParser {

    BufferedReader TrecDocsFile;
    BufferedReader TrecRelFile;
    BufferedReader TrecQueryFile;
    private StringReader currFile;
    private int currDocNum;
    private String currQuery;
    private int currQueryNum;
    private String currResult;
    private int currResultNum;
    String DoclineStack = ""; // contain the previous line of DOCs
    public TRECParser(String FolderPath) throws IOException {
        String DocsPath = FolderPath + "\\DOC";
        InputStreamReader stream = new InputStreamReader(new FileInputStream(DocsPath), "UTF-8");
        TrecDocsFile = new BufferedReader(stream);

        String RelPath = FolderPath + "\\REL";
        stream = new InputStreamReader(new FileInputStream(RelPath), "UTF-8");
        TrecRelFile = new BufferedReader(stream);

        String QueryPath = FolderPath + "\\QUERY";
        stream = new InputStreamReader(new FileInputStream(QueryPath), "UTF-8");
        TrecQueryFile = new BufferedReader(stream);

    }

    public boolean nextDoc() throws IOException {
        String REGEX = "\\*TÀI LIỆU \\d+";
        Pattern regex = Pattern.compile(REGEX);
        Pattern num = Pattern.compile("\\d+");
        StringBuilder aBuilder = new StringBuilder();
        int docNum = 0;
        
        boolean readFlag = false;
        boolean startFlag = true;
        while ((DoclineStack) != null) {
            Matcher match = regex.matcher(DoclineStack);
            if (match.find()) {
                if (!startFlag) {
                    break;
                }
                startFlag = !startFlag;
                readFlag = !readFlag;
                String header = match.group();
                Matcher nummatch = num.matcher(header);
                if (nummatch.find()) {
                    String temp = nummatch.group();
                    docNum = Integer.parseInt(temp);

                }
            }
            if (!readFlag) {
                DoclineStack = TrecDocsFile.readLine();
                continue;
            }
            // this line is the header of document
            aBuilder.append(DoclineStack + " ");
            DoclineStack = TrecDocsFile.readLine();
        }
        if (!aBuilder.toString().isEmpty()) {
            currFile = new StringReader(aBuilder.toString());
            currDocNum = docNum;
            return true;
        } else {
            return false;
        }
    }

    public int getCurrentDoc() {
        return currDocNum;
    }

    private boolean nextQuery() throws IOException {
        String REGEX = "\\*TRUY VẤN +\\d+";
        Pattern regex = Pattern.compile(REGEX);
        Pattern num = Pattern.compile("\\d+");
        String line = "";
        boolean readFlag = false;
        String queryString = null;
        int queryNum = 0;
        while ((line) != null) {
            Matcher match = regex.matcher(line);
            if (match.find()) {
                String header = match.group();
                match = num.matcher(header);
                if (match.find()) {
                    queryNum = Integer.parseInt(match.group());
                    readFlag = !readFlag;
                }
                line = TrecQueryFile.readLine();
                continue;
            }
            if (!line.trim().isEmpty()) {
                queryString = line;
                break;
            }
            line = TrecQueryFile.readLine();
        }
        if (queryString != null) {
            currQuery = queryString;
            currQueryNum = queryNum;
            return true;
        } else {
            return false;
        }
    }

    private boolean nextResult() throws IOException {
        String line = null;
        while ((line = TrecRelFile.readLine()) != null) {
            if (!line.contains("|")) {
                continue;
            }
            String[] spliter = line.split("\\|");
            this.currResultNum = Integer.parseInt(spliter[0]);
            this.currResult = spliter[1];
            return true;
        }
        return false;
    }

    public List<TrecQueryResult> getQueryResult() throws IOException {
        List<TrecQueryResult> queryResults = new ArrayList<TrecQueryResult>();
        while (true) {
            if (nextQuery() && nextResult()) {
                assert currQueryNum == currResultNum;
                String[] results = currResult.trim().split(" +");
                int[] relDocs = new int[results.length];
                for (int i = 0; i < results.length; i++) {
                    relDocs[i] = Integer.parseInt(results[i]);
                }
                queryResults.add(new TrecQueryResult(currQuery, currQueryNum, relDocs));
            } else {
                break;
            }
        }
        return queryResults;
    }

    public StringReader getCurrFileContent() {
        return currFile;
    }
}
