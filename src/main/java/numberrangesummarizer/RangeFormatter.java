package numberrangesummarizer;

/**
 * Decides how a single run of numbers is written out.
 *
 * <p>The summarizer works out which numbers belong together, and a formatter
 * decides what that looks like as text. Keeping them separate means you can
 * change the output style without touching the grouping logic.
 *
 * <p>There's only one method, so you can pass in a lambda as well:
 * <pre>{@code
 * RangeFormatter brackets = (start, end) -> "[" + start + ".." + end + "]";
 * }</pre>
 */
@FunctionalInterface
public interface RangeFormatter {

    /**
     * Writes one run of numbers as text.
     *
     * @param start the first number in the run
     * @param end   the last number in the run (the same as {@code start} if it's just one number)
     * @return the text for the run
     */
    String format(int start, int end);
}
