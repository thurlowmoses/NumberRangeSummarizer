# NumberRangeSummarizer

A small Java library that takes a list of numbers and writes it out as a shorter
summary, grouping any consecutive numbers into a range.

```text
Input:  1,3,6,7,8,12,13,14,15,21,22,23,24,31
Output: 1, 3, 6-8, 12-15, 21-24, 31
```

This was built for the impact.com graduate assessment. The brief gave a single
interface, `NumberRangeSummarizer`, with two methods to implement:

- `collect(String input)` — turn a comma separated string of numbers into a
  `Collection<Integer>`.
- `summarizeCollection(Collection<Integer> input)` — turn that collection back
  into the grouped, human-readable summary string shown above.

## Requirements

- Java 8 (the project is built and tested against a real Java 8 JDK, not just
  configured for it — see **Build & test** below).
- Maven.

## Build & test

```bash
mvn clean test
```

This compiles the project and runs the full test suite. You should see:

```text
Tests run: 39, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## Assumptions

The brief doesn't say what to do in a few situations, so these choices were
made deliberately, and each one has a unit test backing it up:

- The numbers can come in any order; they're sorted before grouping.
- Duplicate numbers are treated as one, e.g. `1,1,2` → `1-2`.
- Two numbers in a row already count as a range, e.g. `1,2` → `1-2`.
- Null or blank input is treated as "nothing to summarize" and returns an
  empty result rather than throwing.
- Extra whitespace and empty entries (e.g. `"1, ,2"`) are ignored rather than
  rejected.
- Anything that isn't a whole number is rejected, with an error message that
  names the exact value that was wrong.
- The collection passed in to `summarizeCollection` is never modified.

## Design

`NumberRangeSummarizerImpl` is the implementation of the provided interface.
It handles the actual logic: parsing the input, sorting and de-duplicating the
numbers, and walking them once to find where each consecutive run starts and
ends.

How a run gets *written out* as text (`"6-8"`, `"31"`, etc.) is a separate
concern from finding the runs in the first place, so that part is pulled out
into its own `RangeFormatter` interface, with `DefaultRangeFormatter` as the
normal implementation. `NumberRangeSummarizerImpl` takes a `RangeFormatter` in
its constructor (defaulting to `DefaultRangeFormatter` if none is given), so
the output format could be changed — a different separator, brackets instead
of a dash, etc. — without touching the grouping logic at all.

One edge case worth calling out: grouping consecutive numbers means comparing
each number to the one before it. Doing that comparison with plain `int`
subtraction breaks right at the boundary between `Integer.MAX_VALUE` and
`Integer.MIN_VALUE`, where the subtraction wraps around and can look like
a difference of `1` when it isn't. The comparison is done as a `long` to avoid
that, and there's a dedicated test for it.

## Tests

39 tests across two test classes, organised by what they're proving rather
than just by method name:

- `NumberRangeSummarizerImplTest` — parsing (`collect`), grouping
  (`summarizeCollection`, including the brief's own example and edge cases
  like unsorted input, duplicates, negatives and the `int` overflow boundary),
  the injected `RangeFormatter`, and one end-to-end test running both methods
  together.
- `DefaultRangeFormatterTest` — the default formatting rules on their own.

## What I'd look at next

The core logic and its edge cases (overflow, unsorted/duplicate input, a
custom formatter) are covered. Given more time, the next thing worth adding
is a small CLI entry point (a `main` method reading from stdin or args), so
the summarizer can be run directly rather than only through code or tests.
