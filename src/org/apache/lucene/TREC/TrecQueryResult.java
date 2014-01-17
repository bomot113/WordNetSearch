/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.apache.lucene.TREC;

/**
 *
 * @author Bui Minh Tue
 */
public class TrecQueryResult {

    private String queryString;
    private int queryNumber;
    private int[] relDocs;

    public TrecQueryResult(String queryString, int queryNumber, int[] relDocs) {
        this.queryString = queryString;
        this.queryNumber = queryNumber;
        this.relDocs = relDocs;
    }

    public String getQueryString() {
        return queryString;
    }

    public int getQueryNumber() {
        return queryNumber;
    }

    public int[] getRelatedDocs() {
        return relDocs;
    }
}
