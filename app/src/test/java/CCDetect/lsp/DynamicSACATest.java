package CCDetect.lsp;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.logging.Logger;
import java.util.stream.IntStream;

import org.junit.Test;

import CCDetect.lsp.datastructures.DynamicSACA;
import CCDetect.lsp.datastructures.ExtendedSuffixArray;
import CCDetect.lsp.datastructures.SAIS;
import CCDetect.lsp.utils.Printer;
import CCDetect.lsp.utils.Timer;

/**
 * DynamicSATest
 */
public class DynamicSACATest {

    private static final Logger LOGGER = Logger.getLogger(
            Logger.GLOBAL_LOGGER_NAME);

    @Test
    public void testLF() {
        String input = "pneumonoultramicroscopicsilicovolcanoconiosis";
        int[] inputArr = stringToIntArrayWithTerminator(input);
        SAIS sais = new SAIS();
        ExtendedSuffixArray suff = sais.buildExtendedSuffixArray(inputArr);

        int[] l = DynamicSACA.calculateL(suff.getSuffix(), inputArr, suff.getSuffix().length);

        int current = 0;
        boolean found = false;
        for (int i = 0; i < inputArr.length; i++) {
            if (DynamicSACA.getLFDynamic(i, l, l.length) == 0) {
                current = i;
                found = true;
            }
        }
        assertTrue(found);
        System.out.println(current);
        for (int i = inputArr.length - 1; i >= 0; i--) {
            assertEquals(inputArr[i], l[current]);
            current = DynamicSACA.getLFDynamic(current, l, l.length);
        }
    }

    @Test
    public void testShortSingleInsert() {
        testDynamicSuffixInsertFactor("cacgacg", "a", 3);
        testDynamicSuffixInsertFactor("ab", "a", 0);
        testDynamicSuffixInsertFactor("ab", "a", 1);
        testDynamicSuffixInsertFactor("dc", "b", 1);
        testDynamicSuffixInsertFactor("ab", "x", 1);
        testDynamicSuffixInsertFactor("abc", "x", 1);
        testDynamicSuffixInsertFactor("axbc", "d", 4);
        testDynamicSuffixInsertFactor("axbcd", "a", 5);

        testDynamicSuffixInsertFactor("mississippi", "i", 4);
        testDynamicSuffixInsertFactor("mississippi", "i", 5);
        testDynamicSuffixInsertFactor("pneumonoultramicroscopicsilicovolcanoconiosis",
                "x", 10);
    }

    @Test
    public void testInsertSingleCharOnAllIndices() {
        testInsertOnAllIndices("helloworld", "a");
        testInsertOnAllIndices("pneumonoultramicroscopicsilicovolcanoconiosis", "a");
        testInsertOnAllIndices("pneumonoultramicroscopicsilicovolcanoconiosis", "x");
        testInsertOnAllIndices("pneumonoultramicroscopicsilicovolcanoconiosis", "h");
    }

    public void testDynamicSuffixInsertFactor(String input, String edit, int position) {
        // Build arrays
        SAIS sais = new SAIS();
        int[] originalArray = stringToIntArrayWithTerminator(input);
        ExtendedSuffixArray eSuffBanana = sais.buildExtendedSuffixArray(originalArray);
        int[] editArray = stringToIntArray(edit);
        int[] resultArray = stringToIntArrayWithTerminator(getStringWithEdit(input, edit, position));

        // Build expected result suffix array
        Timer linearTimer = new Timer();
        linearTimer.start();
        ExtendedSuffixArray expected = sais.buildExtendedSuffixArray(resultArray);
        linearTimer.stop();

        // Dynamically update original
        Timer incrementalTimer = new Timer();
        incrementalTimer.start();
        DynamicSACA dynSACA = new DynamicSACA(originalArray, eSuffBanana, eSuffBanana.size() + 100);
        dynSACA.insertFactor(editArray, position);
        ExtendedSuffixArray eSuffUpdated = dynSACA.getExtendedSuffixArray(resultArray);
        incrementalTimer.stop();

        // linearTimer.log("Linear time");
        // incrementalTimer.log("Incremental time");

        // int[] l = DynamicSACA.calculateL(expected.getSuffix(), resultArray,
        // expected.getSuffix().length);
        // System.out.println("Expected L " + Printer.print(l));
        //
        // System.out.println("Expected SA: " + Printer.print(expected.getSuffix()));
        // System.out.println("Actual SA: " + Printer.print(eSuffUpdated.getSuffix()));
        assertArrayEquals(expected.getSuffix(), eSuffUpdated.getSuffix());

        // System.out.println("Expected ISA: " +
        // Printer.print(expected.getInverseSuffix()));
        // System.out.println("Actual ISA: " +
        // Printer.print(eSuffUpdated.getInverseSuffix()));

        assertArrayEquals(expected.getInverseSuffix(),
                eSuffUpdated.getInverseSuffix());

    }

    public void testInsertOnAllIndices(String input, String edit) {
        for (int i = 0; i <= input.length(); i++) {
            testDynamicSuffixInsertFactor(input, edit, i);
        }
    }

    @Test
    public void testInsertSmallFactor() {
        testDynamicSuffixInsertFactor("b", "a", 0);
        testDynamicSuffixInsertFactor("ctctgc", "g", 2);
        testDynamicSuffixInsertFactor("b", "ac", 1);
        testDynamicSuffixInsertFactor("b", "acd", 1);
        testDynamicSuffixInsertFactor("b", "a", 0);
        testDynamicSuffixInsertFactor("b", "ab", 0);
        testDynamicSuffixInsertFactor("ac", "da", 1);
        testDynamicSuffixInsertFactor("ab", "ab", 2);
        testDynamicSuffixInsertFactor("b", "bb", 0);
        testDynamicSuffixInsertFactor("b", "asldkjsalkdj", 0);
        testDynamicSuffixInsertFactor("bcd", "bcad", 1);
        testDynamicSuffixInsertFactor("bcd", "xxx", 3);
        testDynamicSuffixInsertFactor("bcd", "xadpx", 1);
        testDynamicSuffixInsertFactor("bcd", "cd", 2);
        testInsertOnAllIndices("bcd", "hx");
        testInsertOnAllIndices("bcd", "aj");
    }

    @Test
    public void testInsertFactorOnAllIndices() {
        testInsertOnAllIndices("pneumonoultramicroscopicsilicovolcanoconiosis",
                "habc");
        testInsertOnAllIndices("pneumonoultramicroscopicsilicovolcanoconiosis",
                "xxxxxasldkjsadoiqw");
    }

    @Test
    public void deleteSmallFactor() {
        testDynamicSuffixDeleteFactor("abc", 1, 2);
    }

    public void testDynamicSuffixDeleteFactor(String input, int position, int length) {
        SAIS sais = new SAIS();
        int[] originalArray = stringToIntArrayWithTerminator(input);
        ExtendedSuffixArray eSuffOriginal = sais.buildExtendedSuffixArray(originalArray);
        int[] resultArray = stringToIntArrayWithTerminator(getStringWithDelete(input, position, length));
        LOGGER.info("Result array " + Printer.print(resultArray));
        System.out.println(Printer.print(originalArray));
        System.out.println(Printer.print(resultArray));

        Timer linearTimer = new Timer();
        linearTimer.start();
        ExtendedSuffixArray expected = sais.buildExtendedSuffixArray(resultArray);
        linearTimer.stop();

        int[] l = DynamicSACA.calculateL(expected.getSuffix(), resultArray,
                expected.getSuffix().length);
        LOGGER.info("Expected L " + Printer.print(l));

        DynamicSACA dynSACA = new DynamicSACA(originalArray, eSuffOriginal, eSuffOriginal.size() + 100);
        Timer incrementalTimer = new Timer();
        incrementalTimer.start();
        dynSACA.deleteFactor(originalArray, position, length);
        ExtendedSuffixArray eSuffUpdated = dynSACA.getExtendedSuffixArray(resultArray);
        incrementalTimer.stop();

    }

    public int[] stringToIntArrayWithTerminator(String input) {
        return IntStream.concat(input.chars().map(c -> {
            return (int) c - ('a' - 1);
        }), IntStream.of(0)).toArray();
    }

    public int[] stringToIntArray(String input) {
        return input.chars().map(c -> {
            return (int) c - ('a' - 1);
        }).toArray();
    }

    public String getStringWithEdit(String input, String edit, int position) {
        return input.substring(0, position) + edit + input.substring(position);
    }

    public String getStringWithDelete(String input, int position, int length) {
        return input.substring(0, position) + input.substring(position + length);
    }
}
