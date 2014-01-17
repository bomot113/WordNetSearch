package org.apache.lucene.analysis;

/**
 * Created by IntelliJ IDEA.
 * User: Bui Minh Tue
 * Date: Sep 1, 2009
 * Time: 10:16:41 AM
 * To change this template use File | Settings | File Templates.
 */
import net.didion.jwnl.JWNL;
import net.didion.jwnl.JWNLException;
import net.didion.jwnl.data.*;
import net.didion.jwnl.dictionary.Dictionary;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

class SynsetPool {

    int NUMBER_OF_CASEs = 12;
    private double[] CaseRanking;
    private List<Synset> PotentialSyns;
    private double[] RankValues;
    double maxRank = 0;
    Synset maxSyn = null;

    public SynsetPool() {
        CaseRanking = new double[NUMBER_OF_CASEs];
        RankValues = new double[NUMBER_OF_CASEs];
        for (int i = 0; i < NUMBER_OF_CASEs; i++) {
            CaseRanking[i] = 1;
            RankValues[i] = 0;
        }
        PotentialSyns = new ArrayList<Synset>();
    }

    public void addSense(Synset aSyn, int case_number) throws Exception {
        //case_number is an integer started at 1
        int case_num = case_number - 1;
        if (case_num >= NUMBER_OF_CASEs || case_num < 0) {
            throw new Exception("Out of case range!");
        }
        if (aSyn == null) {
            return;
        }
        if (PotentialSyns.contains(aSyn)) {
            int currPos = PotentialSyns.indexOf(aSyn);
            RankValues[currPos] += CaseRanking[case_num];
            if (RankValues[currPos] > maxRank) {
                maxSyn = aSyn;
            }
        } else {
            RankValues[PotentialSyns.size()] = CaseRanking[case_num];
            PotentialSyns.add(aSyn);
            if (CaseRanking[case_num] > maxRank) {
                maxSyn = aSyn;
            }
        }
    }

    public Synset getMostSuitableSense() {
        return maxSyn;
    }
}

public abstract class WSDUtil {

    private boolean DebugFlag = false;
    private Dictionary aDict;

    public WSDUtil(String FileConfigPath) {
        try {
            JWNL.initialize(new FileInputStream(FileConfigPath));
            aDict = Dictionary.getInstance();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public WSDUtil(InputStream aStream) {
        try {
            JWNL.initialize(aStream);
            aDict = Dictionary.getInstance();
        } catch (JWNLException ex) {
            ex.printStackTrace();
        }
    }

    public WSDUtil(Dictionary aDict) {
        this.aDict = aDict;
    }

    public Synset getSenseFirstCase(String Target, String Support) {
        try {
            Synset[] TarSynsets = getSynonyms(Target);
            Synset[] SupSynsets = getSynonyms(Support);
            List<String> supGlosses = getListIdentities(SupSynsets);
            for (Synset aSyn : TarSynsets) {
                if (supGlosses.contains(getSenseIndentity(aSyn))) {
                    return aSyn;
                }
            }
        } catch (Exception ex) {
        }
        return null;
    }

    public Synset getSenseSecondCase(String Target, String Support) throws JWNLException {
        Synset[] TarSynsets = getSynonyms(Target);
        Synset[] SupSynsets = getSynonyms(Support);
        for (Synset aSyn : TarSynsets) {
            for (Synset supSyn : SupSynsets) {
                for (Word aW : supSyn.getWords()) {
                    if (LineContainWord(aSyn.getGloss(), aW.getLemma())) {
                        return aSyn;
                    }
                }
            }
        }
        return null;
    }

    /*    there is a common word between Synset's definitions
    of the target word and the support word
     */
//    public Synset getSenseThirdCase(String Target, String Support) throws JWNLException {
//        // the number of words in this case is not a problem
//        // so just compare as a naive way
//        Synset[] TarSynsets = getSynonyms(Target);
//        Synset[] SupSynsets = getSynonyms(Support);
//        int max = 0;
//        Synset result = null;
//        List<String> debugs = new ArrayList<String>();
//        for (Synset aSyn : TarSynsets) {
//            String[] DefWords = normalSplit(aSyn.getGloss());
//            for (Synset supSyn : SupSynsets) {
//                int commWordCount = 0;
//                debugs.clear();
//                for (String aWord : DefWords) {
//                    if (LineContainWord(supSyn.getGloss(), aWord) && isSignificant(aWord, Support)) {
//                        debugs.add(aWord);
//                        commWordCount++;
//                    }
//                }
//                if (max < commWordCount) {
//                    // only print out to console if DebugFlag is set
//                    if (DebugFlag) {
//                        System.out.print(String.format("{0} 'contain' :", supSyn.getGloss()));
//                        System.out.println(debugs.toString());
//                    }
//                    max = commWordCount;
//                    result = aSyn;
//                }
//            }
//        }
//        if (max == 0) {
//            return null;
//        }
//        return result;
//    }

    /*
     * Check if the common word is the hyponym of all the meanings of
     * the target words, return false if the word is the hyponym of all
     * meanings of target word
     */
    private boolean isSignificant(String aWord, String Target) throws JWNLException {
        IndexWord aWordSyns = aDict.getIndexWord(POS.NOUN, aWord);
        IndexWord targetWord = aDict.getIndexWord(POS.NOUN, Target);
        int count = 0;  //------
        for (Synset targetSyn : targetWord.getSenses()) {
            boolean flag = false;   // indicate the current meaning of
            // target is a hypernym of the word
            for (Synset aSyn : aWordSyns.getSenses()) {
                try {
                    count++;//-----
                    if (isHypoNym(aSyn, targetSyn)) {
                        flag = true;
                        break;
                    }
                } catch (Exception ex) {
                    System.out.print(count);
                }
            }
            // up to now, if the flag is true, then there exists an
            // meaning of the target that is not a Hypernym of that word
            if (!flag) {
                return true;
            }
        }
        return false;
    }
    // if there's any Hyponym definitions of the target word contain a Synonym word of
    // the support word then the definition is determined.

    public Synset getSense41thCase(String Target, String Support) throws JWNLException {
        Synset[] TarSynsets = getSynonyms(Target);
        Synset[] SupSynsets = getSynonyms(Support);
        // get all words that are synonyms to Support word
        List<String> allSupWords = new ArrayList<String>();
        if (SupSynsets != null) {
            for (Synset aSyn : SupSynsets) {
                for (Word aW : aSyn.getWords()) {
                    allSupWords.add(aW.getLemma());
                }
            }
        } else {
            allSupWords.add(Support);
        }
        for (Synset aSyn : TarSynsets) {
            List<Synset> HypsOfaSense = getHyponyms(aSyn);
            for (Synset aHyp : HypsOfaSense) {
                for (String aW : allSupWords) {
                    if (LineContainWord(aHyp.getGloss(), aW)) {
                        return aSyn;
                    }
                }
            }
        }
        return null;
    }

    // if there is any synonym words of target that is included in the definition of
    // a hyponyms of support word, the sense of target word is determined
    public Synset getSense42thCase(String Target, String Support) throws JWNLException {
        Synset[] TarSynsets = getSynonyms(Target);
        Synset[] SupSynsets = getSynonyms(Support);
        for (Synset aSyn : TarSynsets) {
            List<Word> allTarWords = new ArrayList<Word>();
            for (Word aW : aSyn.getWords()) {
                allTarWords.add(aW);
            }
            for (Synset aSupSyn : SupSynsets) {
                List<Synset> HypsOfaSense = getHyponyms(aSupSyn);
                for (Synset aHyp : HypsOfaSense) {
                    for (Word aW : allTarWords) {
                        if (aW.getLemma().equals(Target.toLowerCase())) {
                            continue;
                        }
                        if (LineContainWord(aHyp.getGloss(), aW.getLemma())) {
                            return aSyn;
                        }
                    }
                }
            }
        }

        return null;
    }

    // a Hypernym of target word is a sense of support word
    public Synset getSense51thCase(String Target, String Support) throws JWNLException {
        Synset[] TarSynsets = getSynonyms(Target);
        Synset[] SupSynsets = getSynonyms(Support);

        //get all synonym words of the support
        List<String> allSynSupWord = new ArrayList<String>();
        for (Synset aSyn : SupSynsets) {
            for (Word aW : aSyn.getWords()) {
                allSynSupWord.add(aW.getLemma());
            }
        }
        for (Synset aSyn : TarSynsets) {
            // get all Hyponym word of current sense of target
            List<String> allTarWords = new ArrayList<String>();
            List<Synset> allHypOfaSense = getHyponyms(aSyn);
            for (Synset aHyp : allHypOfaSense) {
                for (Word aW : aHyp.getWords()) {
                    allTarWords.add(aW.getLemma());
                }
            }
            // check if the current sense has a Hypernym contain the support
            for (String aLemma : allSynSupWord) {
                if (allTarWords.contains(aLemma)) {
                    return aSyn;
                }
            }
        }
        return null;
    }

    // a Synonym word of target word (not itself) is include in a Hyponym of support word
    public Synset getSense52thCase(String Target, String Support) throws JWNLException {
        Synset[] TarSynsets = getSynonyms(Target);
        Synset[] SupSynsets = getSynonyms(Support);

        for (Synset aSyn : TarSynsets) {
            List<String> allSynTarWord = new ArrayList<String>();
            for (Word aW : aSyn.getWords()) {
                if (!aW.getLemma().toLowerCase().equals(Support.toLowerCase())) {
                    allSynTarWord.add(aW.getLemma());
                }
            }
            // get all Hyponym word of support
            List<String> allHypWords = new ArrayList<String>();
            for (Synset aSupSyn : SupSynsets) {
                List<Synset> allHypOfaSense = getHyponyms(aSupSyn);
                for (Synset aHyp : allHypOfaSense) {
                    for (Word aW : aHyp.getWords()) {
                        allHypWords.add(aW.getLemma());
                    }
                }
            }
            // check if Hypernym of the support contain the target synonym word
            for (String aLemma : allSynTarWord) {
                if (allHypWords.contains(aLemma)) {
                    return aSyn;
                }
            }
        }
        return null;
    }

    // a hyponym of target is included in several hypernym definition of support
    // this case may be not true in some situation
    public Synset getSense61thCase(String Target, String Support) throws JWNLException {
        int max = 0;
        Synset result = null;
        Synset[] TarSynsets = getSynonyms(Target);
        Synset[] SupSynsets = getSynonyms(Support);
        // get all Hyponym definition of support
        List<String> allHypSupDef = new ArrayList<String>();
        for (Synset aSupSyn : SupSynsets) {
            List<Synset> allHypOfaSense = getHyponyms(aSupSyn);
            for (Synset aHyp : allHypOfaSense) {
                allHypSupDef.add(aHyp.getGloss());
            }
        }

        for (Synset aSyn : TarSynsets) {
            List<Synset> Hyps = getHyponyms(aSyn);
            // tranverse all hyponym word
            List<String> HypoWords = new ArrayList<String>();
            for (Synset aHyp : Hyps) {
                for (Word aW : aHyp.getWords()) {
                    HypoWords.add(aW.getLemma());
                }
            }
            for (String aLemma : HypoWords) {
                int count = 0;
                // check if Hypernym definition of the support contain the target hyponym word
                for (String aDef : allHypSupDef) {
                    if (LineContainWord(aDef, aLemma)) {
                        count++;
                    }
                }
                if (count > max) {
                    max = count;
                    result = aSyn;
                }
            }
        }
        return result;
    }

    // several hyponym defintions of target contain a Hyponym word of support
    public Synset getSense62thCase(String Target, String Support) throws JWNLException {
        int max = 0;
        Synset result = null;
        Synset[] TarSynsets = getSynonyms(Target);
        Synset[] SupSynsets = getSynonyms(Support);

        // get all Hyponym words of support
        List<String> allHypSupWord = new ArrayList<String>();
        for (Synset aSupSyn : SupSynsets) {
            List<Synset> allHypOfaSense = getHyponyms(aSupSyn);
            for (Synset aHyp : allHypOfaSense) {
                for (Word aW : aHyp.getWords()) {
                    allHypSupWord.add(aW.getLemma());
                }
            }
        }

        for (Synset aSyn : TarSynsets) {
            //get all hyponym denifition of current sense
            List<String> allHypTarDef = new ArrayList<String>();
            List<Synset> allHypOfaSense = getHyponyms(aSyn);
            for (Synset aHyp : allHypOfaSense) {
                allHypTarDef.add(aHyp.getGloss());
            }

            for (String aLemma : allHypSupWord) {
                int count = 0;
                // check if Hypernym definition of the support contain the target hyponym word
                for (String aDef : allHypTarDef) {
                    if (LineContainWord(aDef, aLemma)) {
                        count++;
                    }
                }
                if (count > max) {
                    max = count;
                    result = aSyn;
                }
            }
        }
        return result;
    }

    //  the hyponym word of target is included in one or more definitions of support senses
    public Synset getSense71thCase(String Target, String Support) throws JWNLException {
        Synset[] TarSynsets = getSynonyms(Target);
        Synset[] SupSynsets = getSynonyms(Support);
        // get all definition of support senses
        List<String> SupGlosses = new ArrayList<String>();
        for (Synset aSyn : SupSynsets) {
            SupGlosses.add(aSyn.getGloss());
        }
        for (Synset aSyn : TarSynsets) {
            List<String> HypoWords = new ArrayList<String>();
            for (Synset aHyp : getHyponyms(aSyn)) {
                for (Word aW : aHyp.getWords()) {
                    for (String gloss : SupGlosses) {
                        if (aW.getLemma().equalsIgnoreCase("teaching")) {
                            int i = 1;
                        }
                        if (LineContainWord(gloss, aW.getLemma())) {
                            return aSyn;
                        }
                    }
                }
            }
        }
        return null;
    }

    // the defintion of a target Sense contain a support Hyponyms
    // if less of than one, do not return
    public Synset getSense72thCase(String Target, String Support) throws JWNLException {
        Synset[] TarSynsets = getSynonyms(Target);
        Synset[] SupSynsets = getSynonyms(Support);
        int count = 0;
        Synset result = null;
        // get all support Hyponym words
        List<String> HypoWords = new ArrayList<String>();
        for (Synset aSyn : SupSynsets) {
            for (Synset aHyp : getHyponyms(aSyn)) {
                for (Word aW : aHyp.getWords()) {
                    HypoWords.add(aW.getLemma());
                }
            }
        }
        for (Synset aSyn : TarSynsets) {
            if (count > 1) {
                return null;
            }
            for (String aSupHypWord : HypoWords) {
                if (LineContainWord(aSyn.getGloss(), aSupHypWord)) {
                    count++;
                    result = aSyn;
                }
            }
        }
        return result;
    }

    // there's a common term t which is a hyponym of both support and target
    public Synset getSense9thCase(String Target, String Support) throws JWNLException {
        Synset[] TarSynsets = getSynonyms(Target);
        Synset[] SupSynsets = getSynonyms(Support);
        // get all hyponym word of support
        List<String> SupHypWords = new ArrayList<String>();
        for (Synset aSyn : SupSynsets) {
            for (Synset aHyp : getHyponyms(aSyn)) {
                for (Word aW : aHyp.getWords()) {
                    SupHypWords.add(aW.getLemma());
                }
            }
        }
        for (Synset aSyn : TarSynsets) {
            //get all hyponym words of current sense
            List<String> TarHypWords = new ArrayList<String>();
            for (Synset aHyp : getHyponyms(aSyn)) {
                for (Word aW : aHyp.getWords()) {
                    TarHypWords.add(aW.getLemma());
                }
            }
            //compare Hyponym lists of Target and Support
            for (String aSupWord : SupHypWords) {
                if (TarHypWords.contains(aSupWord)) {
                    return aSyn;
                }
            }
        }
        return null;
    }

    public Synset getSense(String Target, String Support) throws JWNLException {
        List<Synset> result = new ArrayList<Synset>();
        
        // if the word has only one meaning, don't wait
        // for any seconds, return it immediately
        Synset[] TarSynsets = getSynonyms(Target);
        if (TarSynsets.length == 1) return TarSynsets[0];
        
        // A word cannot support meaning for itself
        if (Target.equalsIgnoreCase(Support)) return null;
        
        
        
        Synset aSyn = null;
        SynsetPool aPool = new SynsetPool();
        try {
            aSyn = getSenseFirstCase(Target, Support);
            aPool.addSense(aSyn, 1);
            aSyn = getSenseSecondCase(Target, Support);
            aPool.addSense(aSyn, 2);
//            aSyn = getSenseThirdCase(Target, Support);
//            aPool.addSense(aSyn, 3);
            aSyn = getSense41thCase(Target, Support);
            aPool.addSense(aSyn, 4);
            aSyn = getSense42thCase(Target, Support);
            aPool.addSense(aSyn, 4);
            aSyn = getSense51thCase(Target, Support);
            aPool.addSense(aSyn, 5);
            aSyn = getSense52thCase(Target, Support);
            aPool.addSense(aSyn, 5);
            aSyn = getSense61thCase(Target, Support);
            aPool.addSense(aSyn, 6);
            aSyn = getSense62thCase(Target, Support);
            aPool.addSense(aSyn, 6);
            aSyn = getSense71thCase(Target, Support);
            aPool.addSense(aSyn, 7);
            aSyn = getSense72thCase(Target, Support);
            aPool.addSense(aSyn, 7);
            aSyn = getSense9thCase(Target, Support);
            aPool.addSense(aSyn, 9);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        // if after disambiguation, there's no suitable synset is chosen
        // we add all synsets of the word
        if (result.size() == 0) {
        }
        return aPool.getMostSuitableSense();
    }

    private List<String> getListIdentities(Synset[] aSynList) {
        List<String> result = new ArrayList<String>();
        for (Synset aSyn : aSynList) {
            result.add(getSenseIndentity(aSyn));
        }
        return result;
    }

    public Synset getSenseAt(String noun, int index) throws JWNLException {
        Synset[] senses = getSynonyms(noun);
        if (senses == null) {
            return null;
        }
        if (index < senses.length && index > -1) {
            return senses[index];
        } else {
            return null;
        }
    }

    public Synset[] getSynonyms(String noun) throws JWNLException {
        // look up first sense of the word "dog"
        IndexWord idxWord = aDict.getIndexWord(POS.NOUN, noun);
        return idxWord != null ? idxWord.getSenses() : null;
    }

    /*
     * Check if a noun is a Hyponym of another word
     */
    public Synset isHypoNym(String wordA, String wordB) throws JWNLException {
        IndexWord idxWord = aDict.getIndexWord(POS.NOUN, wordA);
        Synset result = null;
        for (Synset aSyn : idxWord.getSenses()) {
            result = isHypoNym(aSyn, wordB);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    public boolean isHypoNym(Synset aSynA, Synset aSynB) throws JWNLException {
        Pointer[] Hyponyms = aSynA.getPointers(PointerType.HYPERNYM);
        for (Pointer hyp : Hyponyms) {
            Synset aSynset = hyp.getTargetSynset();
            if (aSynset.equals(aSynB) || isHypoNym(aSynset, aSynB)) {
                return true;
            }
        }
        return false;
    }

    private Synset isHypoNym(Synset aSynA, String wordB) throws JWNLException {
        Pointer[] Hyponyms = aSynA.getPointers(PointerType.HYPERNYM);
        for (Pointer hyp : Hyponyms) {
            Synset aSynset = hyp.getTargetSynset();
            if (aSynset.containsWord(wordB)) {
                return aSynset;
            }
            aSynset = isHypoNym(aSynset, wordB);
            if (aSynset != null) {
                return aSynset;
            }
        }
        return null;
    }

    private List<Synset> getHyponyms(Synset aSense) throws JWNLException {
        List<Synset> result = new ArrayList<Synset>();
        // get the hypernyms
        Pointer[] hypernyms =
                aSense.getPointers(PointerType.HYPONYM);
        // print out each hypernym's id and synonyms
        for (Pointer aPointer : hypernyms) {
            Synset aHypSyn = aPointer.getTargetSynset();
            result.add(aHypSyn);
            result.addAll(getHyponyms(aHypSyn));
        }
        return result;
    }

    /* Make an Indentity for a Synset for which it stands
     * in the index process
     */
    public String getSenseIndentity(Synset aSyn) {
        return String.format("%s_%s", (aSyn.getPOS()).getLabel(), aSyn.getOffset());
    }

    public String getDefFromIdentity(String idenity) {
        String[] pairKey = idenity.split("_");
        if (pairKey.length != 2) {
            return "";
        }
        Synset result = null;
        try {
            result = aDict.getSynsetAt(POS.getPOSForLabel(pairKey[0]), Integer.parseInt(pairKey[1]));
        } catch (JWNLException ex) {
        }
        return (result != null) ? result.getGloss() : "";
    }
    /* return true if the word is a noun in dictionary
     */

    protected boolean isNoun(String aWord) throws JWNLException {
        if (aDict.getIndexWord(POS.NOUN, aWord) != null) {
            return true;
        }
        return false;
    }

    abstract protected boolean LineContainWord(String line, String word);

    abstract protected String[] normalSplit(String aStr) throws JWNLException;
}
