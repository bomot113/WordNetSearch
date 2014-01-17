/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.apache.lucene.TREC;

import java.util.List;

/**
 *
 * @author Tue Bui
 */
public class TrecComparation {

    private List<TrecQueryResult> resultsOfActualSystem;
    private List<TrecQueryResult> resultsOfStandardSystem;
    private double[][] compareResult = new double[11][2];   //11 interpolated points

    public TrecComparation() {
    }

    public TrecComparation(List<TrecQueryResult> expectedResultSet, List<TrecQueryResult> queriedResultSet) {
        resultsOfActualSystem = queriedResultSet;
        resultsOfStandardSystem = expectedResultSet;
        for (int i = 0; i < 11; i++) {
            compareResult[i][0] = i * 0.1;
            compareResult[i][1] = 0;
        }
    }

    /**
     *
     * @param expectedResult: relevant documents of a test collection
     * @param queriedResult: actual results that queried by the system
     * @param PRPairResults: an array double[][2] contain Precision Recall pairs
     *
     */
    public double[][] compare(TrecQueryResult expectedResult, TrecQueryResult queriedResult) {
        int count = 0;  // count the number of expected results which have been queried
        if (expectedResult == null || queriedResult == null) {
            return null;
        }
        int[] expectedDocs = expectedResult.getRelatedDocs();
        int[] queriedDocs = queriedResult.getRelatedDocs();
        int size = queriedResult.getRelatedDocs().length;
        double[][] Results = new double[size][2];
        for (int i = 0; i < size; i++) {
            // the bool value is to check if the current queried result is expected
            for (int j = 0; j < expectedDocs.length; j++) {
                if (queriedDocs[i] == expectedDocs[j]) {
                    count++;
                    break;
                }
            }
            // compute recall and precision for the current document
            Results[i][0] = count * 1.0 / (expectedDocs.length); //recall
            Results[i][1] = count * 1.0 / (i + 1);
        }
        return Results;
    }
    /**
     * Get 11 interpolated points of IR evaluation for the current system
     * @param expectedResult: expected result for a specific query
     * @param queriedResult: result for a specific query has been queried by system
     * @return 11 points(double,double) contain pairs of recall and precision
     */
    public double[][] get11IntePointEvalResult(TrecQueryResult expectedResult, TrecQueryResult queriedResult) {
        double[][] PRInterPolatedResult = new double[11][2];
        double[][] PRResult = compare(expectedResult, queriedResult);
        int sizeOfResult = PRResult.length;
        int currCursor = 10;
        assert sizeOfResult > 0;
        double max = PRResult[sizeOfResult - 1][1];
        // find the max precision of the point which have recall >= i*0.1
        for (int j = PRResult.length - 1; j >= 0; j--) {
            max = Math.max(max, PRResult[j][1]);
            while (currCursor * 0.1 >= PRResult[j][0] && currCursor >= 0) {
                PRInterPolatedResult[currCursor][1] = max;
                PRInterPolatedResult[currCursor][0] = currCursor * 0.1;
                currCursor--;
            }
        }
        return PRInterPolatedResult;
    }
}
