package CCDetect.lsp.datastructures;

import java.util.logging.Logger;

import CCDetect.lsp.datastructures.editdistance.EditOperation;
import CCDetect.lsp.datastructures.rankselect.WaveletMatrix;
import CCDetect.lsp.utils.Printer;

/**
 * DynamicSA
 */
public class DynamicSACA {
    private static final Logger LOGGER = Logger.getLogger(
            Logger.GLOBAL_LOGGER_NAME);

    int EXTRA_SIZE_INCREASE = 200;

    DynamicPermutation permutation;
    DynamicLCP lcp;

    CharacterCount charCounts;
    WaveletMatrix waveletMatrix;

    // Assume initial arrays are of same size
    // Creates a dynamic suffix array datastructure with initialSize potential size
    public DynamicSACA(int[] initialText, int[] initialSA, int[] initialISA, int[] initialLCP) {
        charCounts = new CharacterCount(initialText);
        permutation = new DynamicPermutation(initialSA);
        lcp = new DynamicLCP(initialLCP);

        int[] l = calculateL(initialSA, initialText, initialText.length);
        waveletMatrix = new WaveletMatrix(l, l.length + 100);
    }

    public DynamicSACA(int[] initialText, ExtendedSuffixArray initialESuff) {
        this(initialText, initialESuff.getSuffix(), initialESuff.getInverseSuffix(), initialESuff.getLcp());
    }

    public DynamicPermutation getPermutation() {
        return permutation;
    }

    public ExtendedSuffixArray getESuffFromPermutation(int[] fingerprint) {
        int[] sa = permutation.toArray();
        int[] isa = permutation.inverseToArray();
        int[] lcp = new SAIS().buildLCPArray(fingerprint, sa, isa);

        return new ExtendedSuffixArray(sa, isa, lcp);
    }

    public DynamicLCP getDynLCP() {
        return lcp;
    }

    // Inserts a factor into the suffix array at position [start, end] (inclusive)
    public void insertFactor(EditOperation edit) {
        int[] newText = edit.getChars().stream().mapToInt(i -> i).toArray();
        int position = edit.getPosition();

        int end = newText.length - 1;

        // Stage 2, replace in L
        int posFirstModified = permutation.getInverse(position);
        int previousCS = getLF(posFirstModified);

        int storedLetter = waveletMatrix.get(posFirstModified);
        substituteInL(newText[end], posFirstModified);

        int pointOfInsertion = getLF(posFirstModified);
        // Number of smaller characters is one off if the char we have stored is less
        // than the one we inserted
        pointOfInsertion += storedLetter < newText[end] ? 1 : 0;

        // Stage 3, Insert new rows in L
        for (int i = end - 1; i >= 0; i--) {

            insertInL(newText[i], pointOfInsertion);

            // Increment previousCS and/or posFirstModified if we inserted before them
            previousCS += pointOfInsertion <= previousCS ? 1 : 0;
            posFirstModified += pointOfInsertion <= posFirstModified ? 1 : 0;

            // Insert new rows
            permutation.insert(pointOfInsertion, position);
            lcp.insertNewValue(pointOfInsertion);

            int oldPOS = pointOfInsertion;
            pointOfInsertion = getLF(pointOfInsertion);
            // Again number of smaller characters is one off potentially
            pointOfInsertion += storedLetter < newText[i] ? 1 : 0;

            // Rank is one off if the character is the same and inserted after the stored
            // char
            if (posFirstModified < oldPOS && newText[i] == storedLetter) {
                pointOfInsertion++;
            }

        }
        // Inserting final character that we substituted before

        insertInL(storedLetter, pointOfInsertion);
        permutation.insert(pointOfInsertion, position);
        lcp.insertNewValue(pointOfInsertion);

        previousCS += pointOfInsertion <= previousCS ? 1 : 0;
        posFirstModified += pointOfInsertion <= posFirstModified ? 1 : 0;

        // Stage 4
        int pos = previousCS;
        int expectedPos = getLF(pointOfInsertion);

        while (pos != expectedPos) {
            int newPos = getLF(pos);
            moveRow(pos, expectedPos);
            pos = newPos;
            expectedPos = getLF(expectedPos);
        }

        updateLCP(pos);
    }

    public void deleteFactor(EditOperation edit) {
        int position = edit.getPosition();
        int length = edit.getChars().size();

        // Stage 2, replace in L (but not actually)
        int posFirstModified = permutation.getInverse(position + length);
        int deletedLetter = waveletMatrix.get(posFirstModified);

        int pointOfDeletion = getLF(posFirstModified);

        // Stage 3, Delete rows in L
        for (int i = 0; i < length - 1; i++) {

            int currentLetter = waveletMatrix.get(pointOfDeletion);
            int tmp_rank = getWaveletRank(pointOfDeletion);
            // Rank could be one off since T[i-1] is in L twice at this point
            if (posFirstModified < pointOfDeletion && deletedLetter == currentLetter) {
                tmp_rank--;
            }

            // Delete rows
            deleteInL(pointOfDeletion);
            permutation.delete(pointOfDeletion);
            lcp.deleteValue(pointOfDeletion);

            // Update posFirstModified if it has moved because of deletion
            posFirstModified -= pointOfDeletion <= posFirstModified ? 1 : 0;

            pointOfDeletion = tmp_rank + getCharsBefore(currentLetter);
            // Rank is one off potentially since T[i-1] is twice in L at this point
            pointOfDeletion -= deletedLetter < currentLetter ? 1 : 0;
        }
        int currentLetter = waveletMatrix.get(pointOfDeletion);
        int tmp_rank = getWaveletRank(pointOfDeletion);
        if (posFirstModified < pointOfDeletion && deletedLetter == currentLetter) {
            tmp_rank--;
        }

        deleteInL(pointOfDeletion);
        permutation.delete(pointOfDeletion);
        lcp.deleteValue(pointOfDeletion);
        System.out.println("Deleting at " + pointOfDeletion);

        posFirstModified -= pointOfDeletion <= posFirstModified ? 1 : 0;

        int previousCS = tmp_rank + getCharsBefore(currentLetter);
        previousCS -= deletedLetter < currentLetter ? 1 : 0;

        // Substitute last character
        pointOfDeletion = posFirstModified;
        substituteInL(currentLetter, pointOfDeletion);

        pointOfDeletion = getLF(pointOfDeletion);

        // Stage 4
        int pos = previousCS;
        int expectedPos = pointOfDeletion;

        while (pos != expectedPos) {
            int newPos = getLF(pos);
            System.out.println("moveRow " + pos + " " + expectedPos);
            moveRow(pos, expectedPos);
            pos = newPos;
            expectedPos = getLF(expectedPos);
        }

        updateLCP(pos);
    }

    // Returns L in an array with custom extra size
    public static int[] calculateL(int[] suff, int[] text, int size) {
        int[] l = new int[size];

        for (int i = 0; i < suff.length; i++) {
            l[i] = text[Math.floorMod(suff[i] - 1, suff.length)];
        }
        return l;
    }

    public int getLF(int index) {

        int charsBefore = getCharsBefore(waveletMatrix.get(index));
        int rank = getWaveletRank(index);
        return charsBefore + rank;
    }

    public int getInverseLF(int index) {
        int charsBefore = getCharsBefore(waveletMatrix.get(index));
        int rank = getWaveletRank(index);
        return charsBefore + rank;
    }

    private int getCharsBefore(int ch) {
        return charCounts.getNumberOfSmallerChars(ch);
    }

    public int getWaveletRank(int position) {
        return waveletMatrix.rank(position);
    }

    private void moveRow(int i, int j) {
        int lValue = waveletMatrix.get(i);
        waveletMatrix.delete(i);
        waveletMatrix.insert(j, lValue);

        int permValue = permutation.get(i);
        permutation.delete(i);
        permutation.insert(j, permValue);

        lcp.deleteValue(i);
        lcp.insertNewValue(j);

    }

    private void insertInL(int ch, int position) {
        charCounts.addChar(ch);
        waveletMatrix.insert(position, ch);
    }

    private void deleteInL(int position) {
        charCounts.deleteChar(waveletMatrix.get(position));
        waveletMatrix.delete(position);
    }

    private void substituteInL(int ch, int position) {
        charCounts.deleteChar(waveletMatrix.get(position));
        charCounts.addChar(ch);
        waveletMatrix.delete(position);
        waveletMatrix.insert(position, ch);
    }

    private void updateLCP(int startPos) {

        // System.out.println("sa: " + Printer.print(permutation.toArray()));
        // System.out.println("inverse: " +
        // Printer.print(permutation.inverseToArray()));
        for (int pos : lcp.getPositionsToUpdate()) {
            if (pos >= permutation.size()) {
                continue;
            }
            int[] newSuffix = getSuffixAt(permutation.get(pos));
            int[] previous = getSuffixAt(permutation.get(pos - 1));
            int lcpValue = getLCPValue(newSuffix, previous);
            // System.out.println("New lcp value " + lcpValue);
            lcp.setValue(pos, lcpValue);
        }

        int pos = startPos;
        while (true) {

            if (pos != 0) {
                int oldLCP = lcp.get(pos);
                int[] newSuffix = getSuffixAt(permutation.get(pos));
                int[] previous = getSuffixAt(permutation.get(pos - 1));
                int lcpValue = getLCPValue(newSuffix, previous);
                if (oldLCP != lcpValue) {
                }
                lcp.setValue(pos, lcpValue);
            }
            if (pos < lcp.tree.size() - 1) {

                int oldLCP = lcp.get(pos + 1);
                int[] newSuffix = getSuffixAt(permutation.get(pos + 1));
                int[] previous = getSuffixAt(permutation.get(pos));
                int lcpValue = getLCPValue(newSuffix, previous);
                if (oldLCP != lcpValue) {
                }
                lcp.setValue(pos + 1, lcpValue);
            }

            if (permutation.get(pos) == 0) {
                break;
            }
            // System.out.println("New lcp value " + lcpValue);
            pos = getLF(pos);

        }

        // LCP[0] should always be 0, maybe we could avoid this assignment, but not too
        // important
        lcp.setValue(0, 0);

    }

    private int[] getSuffixAt(int index) {
        int current = permutation.getInverse(0);
        int[] out = new int[permutation.size() - index];
        for (int i = out.length - 1; i >= 0; i--) {
            out[i] = waveletMatrix.get(current);
            current = getLF(current);
        }
        return out;
    }

    private int getLCPValue(int[] suff1, int[] suff2) {
        for (int i = 0; i < Math.min(suff1.length, suff2.length); i++) {
            if (suff1[i] != suff2[i]) {
                return i;
            }
        }
        // Unreachable
        return Math.min(suff1.length, suff2.length);
    }

}
