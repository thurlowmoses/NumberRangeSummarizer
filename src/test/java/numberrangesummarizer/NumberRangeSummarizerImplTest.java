package numberrangesummarizer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collection;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NumberRangeSummarizerImplTest {

    private NumberRangeSummarizer summarizer;

    // Runs before every test, so each test gets a fresh object and tests cannot affect each other.
    @BeforeEach
    void setUp() {
        summarizer = new NumberRangeSummarizerImpl();
    }

    @Test
    void summarize_matchesTheExampleFromTheBrief() {
        Collection<Integer> input = summarizer.collect("1,3,6,7,8,12,13,14,15,21,22,23,24,31");

        assertEquals("1, 3, 6-8, 12-15, 21-24, 31", summarizer.summarizeCollection(input));
    }
}
