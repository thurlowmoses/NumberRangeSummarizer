package numberrangesummarizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class DefaultRangeFormatterTest {

    private final RangeFormatter formatter = new DefaultRangeFormatter();

    @Test
    void format_writesASingleNumberOnItsOwn() {
        assertEquals("31", formatter.format(31, 31));
    }

    @Test
    void format_writesALongerRunAsStartDashEnd() {
        assertEquals("6-8", formatter.format(6, 8));
    }

    // Assumption: two consecutive numbers already count as a range.
    @Test
    void format_treatsTwoConsecutiveNumbersAsARange() {
        assertEquals("1-2", formatter.format(1, 2));
    }

    @Test
    void format_keepsTheMinusSignOfNegativeNumbers() {
        assertEquals("-2-1", formatter.format(-2, 1));
    }

    @Test
    void constructor_acceptsACustomRangeSeparator() {
        RangeFormatter dots = new DefaultRangeFormatter("..");

        assertEquals("6..8", dots.format(6, 8));
    }

    @Test
    void constructor_rejectsANullSeparator() {
        assertThrows(NullPointerException.class, () -> new DefaultRangeFormatter(null));
    }
}
