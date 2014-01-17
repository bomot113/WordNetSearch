/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package wnsearchdemo;

import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.swing.JTextArea;
import net.didion.jwnl.dictionary.Dictionary;
import org.apache.lucene.TREC.RandomAccessTrecFile;
import org.apache.lucene.TREC.TRECParser;
import org.apache.lucene.TREC.TrecQueryResult;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.analysis.VNWordNetAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

/**
 *
 * @author Bui Minh Tue
 */
public class AppCode {

    private String TestFolder;
    private String ConfigFile;
    private JTextArea output;
    private boolean WSDMode;
    private RandomAccessTrecFile docTrecFile;

    public void search(String Query) {
        try {
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void setWSDMode(boolean isWSD) {
        WSDMode = isWSD;
    }

    public void setConfigFilePath(String aPath) {
        ConfigFile = aPath;
    }

    public void setTestFolder(String aPath) {
        File aFile = new File(aPath);
        if (aFile.exists()) {
            TestFolder = aPath;
        }
    }

    private void setOutput(JTextArea output) {
        this.output = output;
    }

    private boolean ConfigCheck() {
        boolean check = true;
        if (TestFolder == null) {
            output.append("Folder contained documents is invalid!");
            check = false;
        }

        if (ConfigFile == null) {
            output.append("file configuration for lucene is invalid!");
            check = false;
        }
        return check;
    }

    private void compare(TrecQueryResult expectedResult, TrecQueryResult queriedResult) {
        int count = 0;
        int[] expectedDocs = expectedResult.getRelatedDocs();
        output.append("recall , precision" + System.getProperty("line.separator"));
        for (int i = 0; i < queriedResult.getRelatedDocs().length; i++) {
            boolean check = false;
            for (int j = 0; j < expectedDocs.length; j++) {
                if (queriedResult.getRelatedDocs()[i] == expectedDocs[j]) {
                    count++;
                    // the current queried result is expected
                    check = true;
                    break;
                }
            }
            // now calculate the precision and recall value
            if (check) {
                double recall = count * 1.0 / (expectedDocs.length);
                double precision = count * 1.0 / (i + 1);
                output.append(String.format("%s, %s" + System.getProperty("line.separator"),
                        recall,precision));
            }
        }
    }

    private String normalize(String aStr) {
        String result = aStr;
        result = result.toLowerCase();
        result = result.replace("Đ".toLowerCase(), "đ");
        return result;
    }

    private void search(File indexDir, String q)
            throws Exception {
        Directory fsDir = FSDirectory.getDirectory(indexDir, false);
        IndexSearcher is = new IndexSearcher(fsDir);
        Query query;
        if (WSDMode) {
            query = getWSDQuery(q);
        } else {
            query = getNoWSDQuery(q);
        }
        System.out.println("Query: " + query.toString("contents"));
        Hits hits = is.search(query);
        for (int i = 0; i < hits.length(); i++) {
            Document doc = hits.doc(i);
            output.append(doc.get("filename") + System.getProperty("line.separator"));
        }
    }

    private Query getWSDQuery(String q) throws Exception {
        String queryString = String.format("\"%s\"", q);
        Dictionary aDict = GlobalResource.aDict;
        Analyzer anAnalyzer = new VNWordNetAnalyzer(aDict);
        QueryParser parser = new QueryParser("contents", anAnalyzer);
        Query query = parser.parse(queryString);
        anAnalyzer = new SimpleAnalyzer();
        parser = new QueryParser("contents", anAnalyzer);
        query = parser.parse(query.toString().replace("\"", ""));
        return query;
    }

    private Query getNoWSDQuery(String q) throws Exception {
        Analyzer anAnalyzer = new SimpleAnalyzer();
        QueryParser parser = new QueryParser("contents", anAnalyzer);
        return parser.parse(q);
    }

    public void calculate(JTextArea outputArea) throws Exception {
        setOutput(outputArea);
        output.setText("");
        if (!ConfigCheck()) {
            return;
        }
        TRECParser aParser = new TRECParser(TestFolder);
        List<TrecQueryResult> result = aParser.getQueryResult();
        File indexDir = getIndexedFolder(TestFolder);
        if (!indexDir.exists() || !indexDir.isDirectory()) {
            throw new Exception(indexDir +
                    "No place for indexing folder!");
        }
        for (int i = 0; i < result.size(); i++) {
            output.append(String.format("%d: %s" + System.getProperty("line.separator"),
                    result.get(i).getQueryNumber(), result.get(i).getQueryString()));
            TrecQueryResult queriedResult = search(indexDir, result.get(i));
            compare(result.get(i), queriedResult);
        }
    }

    public TrecQueryResult search(File indexDir, TrecQueryResult queryItem)
            throws Exception {
        Directory fsDir = FSDirectory.getDirectory(indexDir, false);
        IndexSearcher is = new IndexSearcher(fsDir);
        Query query;
        if (WSDMode) {
            query = getWSDQuery(queryItem.getQueryString());
        } else {
            query = getNoWSDQuery(queryItem.getQueryString());
        }
        Hits hits = is.search(query);
        int[] relDocs = new int[hits.length()];
        for (int i = 0; i < hits.length(); i++) {
            Document doc = hits.doc(i);
            relDocs[i] = Integer.parseInt(doc.get("filename"));
        }
        TrecQueryResult aQueryResult = new TrecQueryResult(queryItem.getQueryString(), queryItem.getQueryNumber(), relDocs);
        return aQueryResult;
    }

    public void index(JTextArea outputArea) throws Exception {
        setOutput(outputArea);
        output.setText("");
        if (!ConfigCheck()) {
            return;
        }
        Dictionary aDict = GlobalResource.aDict;
        Analyzer anAnalyzer = (WSDMode) ? new VNWordNetAnalyzer(aDict) : new SimpleAnalyzer();
        indexTrecDocFile(anAnalyzer);
    }

    private File getIndexedFolder (String FolderPath){
        File indexDir = null;
        if (WSDMode) {
            indexDir = new File(FolderPath + "\\indexed-WSD");
        } else {
            indexDir = new File(FolderPath + "\\indexed-NoWSD");
        }
        return indexDir;
    }
    public int indexTrecDocFile(Analyzer anAnalyzer) throws IOException {
        File indexDir = null;
        indexDir = getIndexedFolder(TestFolder);
        String dataDir = TestFolder;
        TRECParser aParser = new TRECParser(dataDir);
        IndexWriter writer = new IndexWriter(indexDir, anAnalyzer, true);
        writer.setUseCompoundFile(false);
        while (aParser.nextDoc()) {
            output.append("Indexing document: " + aParser.getCurrentDoc() + System.getProperty("line.separator"));
            Document doc = new Document();
            doc.add(new Field("contents", aParser.getCurrFileContent()));
            doc.add(new Field("filename", Integer.toString(aParser.getCurrentDoc()), Field.Store.YES, Field.Index.NO));
            writer.addDocument(doc);
        }
        int numIndexed = writer.docCount();
        writer.optimize();
        writer.close();
        return numIndexed;
    }

    public void search(String queryStr, JTextArea outputArea) throws IOException {
        setOutput(outputArea);
        output.setText("");
        File IndexedFolder = getIndexedFolder(TestFolder);
        String query = normalize(queryStr);
        try {
            search(IndexedFolder, query);
        } catch (Exception ex) {
            output.setText(ex.getMessage());
        }
    }

    public void parseTrecForRead() throws IOException {
        if (docTrecFile == null) {
            docTrecFile = new RandomAccessTrecFile(TestFolder + "\\DOC");
        }
    }

    public String getFileContent(int docNum) throws IOException {
        return docTrecFile.getFileContent(docNum);
    }

    public String getConfigFilePath() {
        return ConfigFile;
    }

    public void viewDocument(int docNum, JTextArea outputArea) {
        setOutput(outputArea);
        this.output.setText("");

    }
}
