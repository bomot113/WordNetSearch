package net.didion.jwnl.util;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by IntelliJ IDEA.
 * User: Bui Minh Tue
 * Date: Sep 7, 2009
 * Time: 8:35:47 PM
 * To change this template use File | Settings | File Templates.
 */
public class UnicodeUtil {

    private String WordViForWordnet = "[_ -/.'\"$0123456789aáàăắằẵẳặâấầẫẩậãảạbcdđeéèêếềễểệẽẻẹghiíìĩỉịklmnoóòôốồỗổộõỏọơớờỡởợpqrstuúùũủụưứừữửựvxyýỳỹỷỵ]";

    private String[] getUTFChars(String aUTFString) {
        Pattern UTFCharPattern = Pattern.compile(WordViForWordnet);
        Matcher CharsMatched = UTFCharPattern.matcher(aUTFString);
        List<String> ListChars = new ArrayList<String>();
        while (CharsMatched.find()) {
            String aChar = CharsMatched.group();
            ListChars.add(aChar);
        }
        return (String[]) ListChars.toArray(new String[ListChars.size()]);
    }

    public int compare2VietString(String wordA, String wordB) {
        String[] CharsA = getUTFChars(wordA);
        String[] CharsB = getUTFChars(wordB);
        int lenA = CharsA.length;
        int lenB = CharsB.length;
        int lenMin = (lenA > lenB) ? lenB : lenA;
        for (int i = 0; i < lenMin; i++) {
            int cmpVal = compare2VietChar(CharsA[i], CharsB[i]);
            if (cmpVal != 0) {
                return cmpVal;
            }
        }
        return lenA > lenB ? 1 : (lenA == lenB ? 0 : -1);
    }

    public int compare2ISOString(String wordA, String wordB) {
        int result = -1;
        try {
            byte[] bytesA = wordA.getBytes("UTF-8");
            byte[] bytesB = wordB.getBytes("UTF-8");
            String a = new String(bytesA,"ASCII");
            byte[] abyte = a.getBytes();
             String b = new String(bytesB,"ASCII");
            byte[] bbyte = b.getBytes();
            int lengthMin = (bytesA.length > bytesB.length) ? bytesB.length : bytesA.length;
            for (int i = 0; i < lengthMin; i++) {
                if (bytesA[i] > bytesB[i]) {
                    return 1;
                } else if (bytesA[i] < bytesB[i]) {
                    return -1;
                } else {
                    return 0;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    private int getOrdinaryOfViChar(String aChar) {
        int result = WordViForWordnet.indexOf(aChar);
        return result;
    }

    private int compare2VietChar(String source, String target) {
        int subtraction = getOrdinaryOfViChar(source) - getOrdinaryOfViChar(target);
        if (subtraction > 0) {
            return 1;
        } else if (subtraction < 0) {
            return -1;
        } else {
            return 0;
        }
    }
}
