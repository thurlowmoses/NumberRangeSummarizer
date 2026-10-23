package numberrangesummarizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collection;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class NumberRangeSummarizerImplTest {

    private NumberRangeSummarizer summarizer;

    // Runs before every test, so each test gets a fresh object and tests cannot affect each other.
    @BeforeEach
    void setUp() {
        summarizer = new NumberRangeSummarizerImpl();
    }

    // ------------------------------------------------------------------ collect

    @Test
    void collect_parsesCommaSeparatedNumbersInOrder() {
        Collection<Integer> result = summarizer.collect("1,3,6,7,8");

        assertEquals(Arrays.asList(1, 3, 6, 7, 8), result);
    }

    @Test
    void collect_trimsWhitespaceAroundValues() {
        assertEquals(Arrays.asList(1, 2, 3), summarizer.collect("  1 ,2,   3 "));
    }

    // Assumption: null or blank input is "nothing to parse", not an error.
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    void collect_returnsEmptyCollectionForNullOrBlankInput(String input) {
        assertTrue(summarizer.collect(input).isEmpty());
    }

    // Assumption: empty entries such as "1,,2," are harmless formatting noise and are skipped.
    @Test
    void collect_ignoresEmptyEntries() {
        assertEquals(Arrays.asList(1, 2), summarizer.collect("1,,2,"));
    }

    // collect only parses; sorting and removing duplicates is summarizeCollection's job.
    @Test
    void collect_keepsDuplicatesAndOrder() {
        assertEquals(Arrays.asList(5, 1, 5), summarizer.collect("5,1,5"));
    }

    @Test
    void collect_handlesNegativeNumbers() {
        assertEquals(Arrays.asList(-3, -2, 4), summarizer.collect("-3,-2,4"));
    }

    // Assumption: anything that is not a whole int is rejected (letters, decimals,
    // a different separator, and a value just above Integer.MAX_VALUE).
    @ParameterizedTest
    @ValueSource(strings = {"1,abc,3", "1.5", "1;2", "2147483648"})
    void collect_throwsForValuesThatAreNotValidIntegers(String input) {
        assertThrows(IllegalArgumentException.class, () -> summarizer.collect(input));
    }

    @Test
    void collect_errorMessageNamesTheBadValue() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> summarizer.collect("1,abc,3"));

        assertTrue(e.getMessage().contains("abc"));
    }

    // ------------------------------------------------------------ summarize

    @Test
    void summarize_matchesTheExampleFromTheBrief() {
        Collection<Integer> input = summarizer.collect("1,3,6,7,8,12,13,14,15,21,22,23,24,31");

        assertEquals("1, 3, 6-8, 12-15, 21-24, 31", summarizer.summarizeCollection(input));
    }
}
