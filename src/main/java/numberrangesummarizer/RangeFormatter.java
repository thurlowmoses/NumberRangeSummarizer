package numberrangesummarizer;

/**
 * Strategy for writing one run of consecutive numbers as text.
 *
 * <p>The summarizer decides <em>which</em> numbers belong together; a
 * {@code RangeFormatter} decides <em>how</em> a run is written. Keeping the two apart
 * means the output format can change without touching the grouping algorithm.
 *
 * <p>This is a functional interface (exactly one abstract method), so a lambda can be
 * used as a formatter:
 * <pre>{@code
 * RangeFormatter brackets = (start, end) -> "[" + start + ".." + end + "]";
 * }</pre>
 */
@FunctionalInterface
public interface RangeFormatter {

    /**
     * Formats one run of consecutive numbers.
     *
     * @param start the first number of the run
     * @param end   the last number of the run; equal to {@code start} for a single number
     * @return the text for that run
     */
    String format(int start, int end);
}
