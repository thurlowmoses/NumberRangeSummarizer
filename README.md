# NumberRangeSummarizer

A small Java library that takes a list of numbers and writes it out as a shorter
summary, grouping any consecutive numbers into a range.

```text
Input:  1,3,6,7,8,12,13,14,15,21,22,23,24,31
Output: 1, 3, 6-8, 12-15, 21-24, 31
```

## The exercise

This is an assignment I was given. I was handed one Java interface,
`NumberRangeSummarizer`, and asked to implement it without changing it,
submit it as a pull request on GitHub, and back it with unit tests. The
interface has two methods:

- `collect(String input)` — turn a comma separated string of numbers into a
  `Collection<Integer>`.
- `summarizeCollection(Collection<Integer> input)` — turn that collection back
  into the grouped, human-readable summary string shown above.

The brief itself doesn't say much more than that — the worked example at the
top of this README is the one example it gives. Everything else (what counts
as bad input, what happens with duplicates, unsorted numbers, and so on) was
left for me to decide, so those decisions are written out below, each backed
by a test.

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

## The solution: how it works, and how I got there

**Starting point.** Two methods on the interface means two separate jobs:
turning text into numbers, and turning numbers into a summary. Splitting them
means each one can be tested (and reasoned about) completely on its own —
if a test fails, it's immediately obvious which half broke.

**`collect(String)` — the easy half.** This is a straight pipeline: split the
string on commas, trim whitespace from each piece, drop anything left empty,
then parse what's left as an integer. Java 8 streams fit this well because
the problem genuinely *is* a pipeline:

```java
Arrays.stream(input.split(","))
      .map(String::trim)
      .filter(token -> !token.isEmpty())
      .map(NumberRangeSummarizerImpl::parseInteger)
      .collect(Collectors.toList());
```

The one design decision here: blank input isn't an *error* (there's simply
nothing to summarize), but a token that isn't a number is an error — and the
exception message names the exact bad value, because a caller debugging
`"1,abc,3"` needs to know it was `abc`, not just that something failed.

**`summarizeCollection(Collection<Integer>)` — the actual problem.** You
can't tell what's "consecutive" in an unordered bag of numbers, so the first
step is always to **sort and de-duplicate**. After that, it's a single pass
over the sorted list carrying two values: where the current run of numbers
started (`rangeStart`), and the last number seen (`previous`). At each step,
if the next number isn't exactly `previous + 1`, the run that just ended is
closed off and a new one begins.

The one bug this shape invites, and the reason there's a test for it: the
loop only closes a run when it sees a *break* in the sequence. The very last
run has no break after it to trigger on — the loop just ends. So there's one
extra line after the loop to flush that final run. Skip it, and the last
range silently vanishes from the output.

The other bug this shape invites is at the numeric boundary. Checking
"is `current` one more than `previous`" with plain `int` subtraction breaks
exactly at `Integer.MAX_VALUE` → `Integer.MIN_VALUE`, because that subtraction
wraps around and can come out as `1` when the two numbers aren't actually
neighbours. Doing the subtraction as a `long` first avoids the wraparound.
There's a dedicated test using both boundary values.

**Why formatting was pulled into its own class.** Once grouping worked, the
only remaining "opinion" left in the code was *how* a range gets printed —
`"6-8"` versus some other style. That's a different concern from finding the
runs in the first place, and the brief could change it (brackets, a
different separator) without the grouping logic needing to change at all.
So it was split out into its own `RangeFormatter` interface, with
`DefaultRangeFormatter` as the normal implementation, and
`NumberRangeSummarizerImpl` takes a `RangeFormatter` through its constructor
(defaulting to `DefaultRangeFormatter` if none is given). This is the Strategy
pattern, and it's the part of the solution that's there to demonstrate design,
not just to make the brief's example pass — the grouping algorithm and the
formatting rule can now change independently of each other.

## Tests: what each one checks, and why it's there

39 tests in total (some test methods are parameterised and run several times
with different inputs — the count below is test *executions*, not methods).
Every test is there to guard against one specific, distinct way the code
could be wrong; none of them repeat another test with a trivial variation.

### `collect()` — parsing (15 executions)

| Test | What it checks |
|---|---|
| `collect_parsesCommaSeparatedNumbersInOrder` | The basic case works at all: a plain comma separated string becomes a list of numbers in the same order. |
| `collect_trimsWhitespaceAroundValues` | `" 3 "` is read as `3`, not rejected for having spaces around it. |
| `collect_returnsEmptyCollectionForNullOrBlankInput` *(4 cases: null, `""`, `"   "`, `"\t"`)* | Null or blank input means "nothing to parse," not an error — checked for every way "blank" can show up. |
| `collect_ignoresEmptyEntries` | A stray comma, like in `"1,,2,"`, is skipped rather than treated as a parse failure. |
| `collect_keepsDuplicatesAndOrder` | `collect()` only parses — it does **not** sort or remove duplicates. That's `summarizeCollection()`'s job, so this proves the two responsibilities stay separate. |
| `collect_handlesNegativeNumbers` | A leading `-` is read as part of the number, not mistaken for a separator. |
| `collect_throwsForValuesThatAreNotValidIntegers` *(4 cases: letters, a decimal, the wrong separator, one past `Integer.MAX_VALUE`)* | Anything that isn't a whole `int` is rejected — checked across the different ways input can be malformed. |
| `collect_errorMessageNamesTheBadValue` | The exception thrown for bad input actually names the bad value, so the error is useful for debugging, not just "something broke." |

### `summarizeCollection()` — grouping (16 executions)

| Test | What it checks |
|---|---|
| `summarize_matchesTheExampleFromTheBrief` | The exact worked example given in the brief, end to end. |
| `summarize_groupsConsecutiveNumbersIntoRanges` *(10 rows)* | Every distinct shape a run of numbers can take: a single number, a pair, the whole list as one run, no runs at all, a mix of runs and singles, unsorted input, duplicates, negative numbers, and both `int` overflow boundaries (`MAX_VALUE - 1, MAX_VALUE` and `MAX_VALUE, MIN_VALUE`). |
| `summarize_returnsEmptyStringForNullInput` | A null collection returns `""` rather than throwing. |
| `summarize_returnsEmptyStringForEmptyInput` | An empty (but non-null) collection also returns `""` — checked separately from null because it's a different code path. |
| `summarize_ignoresNullElements` | A `null` sitting *inside* the collection is skipped, rather than crashing the whole summary — distinct from the collection itself being null. |
| `summarize_acceptsAnyCollectionNotJustLists` | The method's signature takes a `Collection`, not a `List` — this proves it actually works with something else, like a `HashSet`, whose iteration order isn't guaranteed. |
| `summarize_doesNotModifyTheCallersCollection` | Calling `summarizeCollection()` doesn't sort or otherwise mutate the collection the caller passed in — the sorting happens on a copy. |

### Dependency injection / design (3 tests)

| Test | What it checks |
|---|---|
| `constructor_rejectsANullFormatter` | Passing `null` as the `RangeFormatter` fails fast with a clear exception, instead of causing a confusing `NullPointerException` later when a range is actually formatted. |
| `summarize_usesTheInjectedFormatter` | A custom `RangeFormatter` (a lambda producing `"[1..3]"` style output) is genuinely used instead of the default — proving the formatting really is swappable, not just a config no-op. |
| `collectThenSummarize_worksTogether` | An end-to-end test: feed a raw string into `collect()`, pipe the result straight into `summarizeCollection()`, and check the final string — since that's how the two methods are actually meant to be used together. |

### `DefaultRangeFormatter` — the default formatting rules on their own (6 tests)

| Test | What it checks |
|---|---|
| `format_writesASingleNumberOnItsOwn` | A run of exactly one number prints as just that number (`"31"`), not `"31-31"`. |
| `format_writesALongerRunAsStartDashEnd` | A genuine range prints as `start-end` (`"6-8"`). |
| `format_treatsTwoConsecutiveNumbersAsARange` | Two numbers count as a range too, not a special "pair" case (`"1-2"`, not `"1, 2"`). |
| `format_keepsTheMinusSignOfNegativeNumbers` | Negative numbers print correctly (`"-2-1"` for the range from `-2` to `1`) rather than the minus sign getting lost or misread as the separator. |
| `constructor_acceptsACustomRangeSeparator` | The separator between start and end can be configured (e.g. `".."` instead of `"-"`), proving the class isn't hard-coded to one format. |
| `constructor_rejectsANullSeparator` | A `null` separator fails fast in the constructor, rather than quietly printing `"6null8"` the first time a range is formatted. |

## What I'd look at next

The core logic and its edge cases (overflow, unsorted/duplicate input, a
custom formatter) are covered. Given more time, the next thing worth adding
is a small CLI entry point (a `main` method reading from stdin or args), so
the summarizer can be run directly rather than only through code or tests.
