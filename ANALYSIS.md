# Assignment 4 — analysis

> 

## Environment and commands

- Language/runtime: Java openjdk-25.0.1
- OS / CPU / RAM: Windows 11 pro, CPU model AMD RYZeN 7 7435hs, 8 cores, 16 GB RAM
- Setup: `python tools/run.py --setup`
- Public checks: `python tools/run.py --check` — result: Python
- Own tests: `javac -d out Java/source/Algorithms.java tests/student/Check.java && java -cp out Check` — result: ALL CHECKS PASSED
- Benchmark: `javac -d out Java/source/Algorithms.java tests/student/Bench.java && java -cp out Bench` — raw output in the "Search comparison" section.
- Final commit URL (also recorded in Moodle): **[FILL: https://github.com/<user>/daa-assignment-4/commit/<40-char SHA>]**

## Algorithm explanations

### lcs
- **State:** `dp[i][j]` = length of an LCS of `a[0..i)` and `b[0..j)`; `i` = number of characters of `a` used, `j` = of `b`.
- **Base / transition:** `dp[0][*] = dp[*][0] = 0` (an empty prefix has no common subsequence). If `a[i-1] == b[j-1]`: `dp[i][j] = dp[i-1][j-1] + 1`, otherwise `max(dp[i-1][j], dp[i][j-1])`.
- **Order:** rows `i = 1..n`, columns `j = 1..m`; every cell needs only the cell above, left and diagonal, which are already computed.
- **Correctness:** if the last characters match, some optimal solution uses them as its last pair (an exchange argument), so we extend the diagonal optimum. If they differ, at least one of them is unused, so the answer is the better of dropping one.
- **Reconstruction / ties:** start at `(n, m)`. On a match, emit the character and move diagonally. Otherwise move to `i-1` if `dp[i-1][j] >= dp[i][j-1]` (ties go up), else to `j-1`. The characters are collected in reverse and the buffer is reversed at the end.
- **Cost:** `O(nm)` time; `(n+1)(m+1)` `int`s of memory (about 4 MB for the largest allowed 1000x1000). Input size `n+m`, output size `O(min(n,m))`.
- **Boundary test:** `a="tea_time", b="team"` expects length 4 and `"team"`. An implementation that computes the longest common *substring* would return 3 or less.

### merge
- **State:** `dp[l][r]` = minimum cost to merge original blocks `l..r` into one block; `sp[l][r]` = the split `k` of the last merge, where `[l,k]` and `[k+1,r]` are joined.
- **Base / transition:** `dp[l][l] = 0`. `dp[l][r] = min over k in [l, r-1] of dp[l][k] + dp[k+1][r] + sum(l..r)`, with the sum taken from prefix sums `pre`.
- **Order:** by interval length `2..n`; both sub-intervals are shorter, so they are ready.
- **Correctness:** the last merge of any plan joins two adjacent intervals, so trying every split gives the minimum; the interval cost is independent of how the parts were built.
- **Reconstruction / ties:** `buildPlan` recurses into `[l,k]`, then `[k+1,r]`, then appends `[l,k,r]`. Children come before parents, so the plan is chronological. A tie keeps the smallest `k` (strict `<`). Recursion depth is at most `n <= 200`.
- **Cost:** `O(n^3)` time, `O(n^2)` memory (`long[n][n]` plus `int[n][n]`); `n-1` plan entries. `long` is used for costs, which can reach about 200 * 1e9 * log-depth.
- **Boundary test:** `[2,3,4]` gives cost 14 with plan `[[0,0,1],[0,1,2]]`. A greedy "merge the two smallest" would give the wrong value on non-adjacent cases; merging `1,2` first costs 16.

### tree
- **State:** `take[u]` = best weight in the subtree of `u` with `u` chosen; `skip[u]` = best weight with `u` not chosen. Vertices are numbered `0..n-1`; the root is 0.
- **Base / transition:** `take[u] = w[u] + sum(skip[child])`, `skip[u] = sum(max(take[child], skip[child]))`. A leaf gets `take = w`, `skip = 0`.
- **Order:** an iterative DFS gives a preorder array; processing it backwards handles every child before its parent. No recursion, so a 100000-vertex chain does not overflow the stack.
- **Correctness:** a chosen vertex forbids chosen neighbours, so its children must be skipped. Skipped vertices let children choose freely. The empty set is allowed, so negative weights are never forced (the answer is `max(take[0], skip[0])`, and negatives lose to `skip`).
- **Reconstruction / ties:** go through the preorder array. `u` is taken only if its parent is not taken and `take[u] > skip[u]`; ties skip. Selected IDs are sorted.
- **Cost:** `O(n)` time and memory (adjacency stored in arrays `head/nxt/to` of size `2n`, plus `parent/order/stack/take/skip`).
- **Boundary test:** `n=2, weights=[-1,-2]` expects weight 0 and an empty selection; a DP that forces the root to be taken would return -1.

### naive
- **Invariant:** at start `i`, the window `t[i..i+m)` is compared with `p` left to right; `j` is the number of characters matched so far.
- **Base cases:** `m = 0` returns all `n+1` boundaries `0..n`; `m > n` produces an empty list because the loop `i + m <= n` never runs.
- **Order / correctness:** every start `0..n-m` is tried in increasing order, so all occurrences (including overlaps) are reported, sorted.
- **Cost:** worst case `O(nm)` time (for example `a^n` against `a^(m-1)b`), `O(1)` auxiliary memory, output size `z`.
- **Boundary test:** `aaaaa` / `aaa` expects `[0,1,2]`, which detects code that skips ahead by `m` after a match and loses overlaps.

### kmp
- **State:** `pi[i]` = length of the longest proper prefix of `p[0..i]` that is also its suffix. In the scan, `k` = number of pattern characters currently matched.
- **Transition:** on mismatch with `k > 0` set `k = pi[k-1]` until a match or `k = 0`; on match increment `k`. When `k == m`, report `i-m+1` and set `k = pi[k-1]` so that overlapping occurrences remain.
- **Base cases:** `pi[0] = 0`; `m = 0` returns all `n+1` boundaries before any indexing; `m > n` returns an empty list.
- **Correctness:** `k` always equals the longest prefix of `p` that is a suffix of the text read so far; this is preserved by following the failure chain.
- **Cost:** `O(n + m + z)` time (each increment of `k` is paid for by at most one decrement), `O(m)` auxiliary memory.
- **Boundary test:** `abababa` / `aba` expects `[0,2,4]`; resetting `k` to 0 after a full match would miss starts 2 and 4.

### rk
- **State:** `hp` = hash of the pattern, `ht` = hash of the current window `t[i..i+m)`, `high = 31^(m-1) mod 101`. Hash is the polynomial with base 31, modulus 101, the leftmost character having the highest power.
- **Update:** `ht = ((ht - t[i]*high % 101 + 101) % 101 * 31 + t[i+m]) % 101`. The `+ 101` normalises a negative remainder.
- **Base cases:** `m = 0` returns `n+1` starts; `m > n` returns an empty list before any window is built.
- **Correctness:** equal hashes are only *candidates*; each candidate is verified character by character before reporting, so a collision can cost time but never produces a wrong answer. Unequal hashes cannot belong to a real match.
- **Cost:** `O(n + m + c*m)` where `c` is the number of equal-hash windows; this can reach `O(nm)`, so no linear worst-case is claimed. Auxiliary memory is `O(1)` plus output.
- **Boundary / counterexample test:** `abdj` with pattern `ab`: both `ab` and `dj` hash to 75 (97*31+98 = 3105 and 100*31+106 = 3206, both = 75 mod 101). The result must be `[0]` only; skipping verification would also report 2.

### suffix
- **Prefix doubling.** State: after round `k`, `rnk[i]` is the equivalence class of the first `2k` characters of suffix `i`, and `sa` is sorted by it. Start with classes by single characters (counting sort). In each round, the second key of suffix `i` is `rnk[i+k]` (or -1 if past the end). Suffixes with no second half are placed first, then `sa[i]-k` in the existing order gives order by the second key; a stable counting sort by `rnk` (first key) completes the pair sort. New classes are assigned by comparing adjacent pairs. Rounds stop when `classes == n` or `k >= n`. Each round is `O(n)`, there are `O(log n)` rounds: `O(n log n)` time, `O(n)` memory.
- **Kasai.** `rank[sa[i]] = i`. Process suffixes in text order; `h` is the LCP of suffix `i` with its predecessor in `sa`. After processing `i`, the next suffix `i+1` has LCP at least `h-1`, so `h` is decremented (not reset) and extended. Total increments are bounded by `2n`, giving `O(n)`. `lcp[0] = 0`.
- **Repeated substring:** the longest substring occurring at two distinct starts equals the maximum adjacent LCP, because any repeat is a common prefix of two suffixes and common prefixes of non-adjacent suffixes are bounded by the minimum over the adjacent ones. The code keeps the first maximum found (ties arbitrary; overlaps allowed). `""` if all LCPs are 0.
- **Cost:** `O(n log n)` time, `O(n)` memory; no sentinel and no empty suffix.
- **Boundary tests:** `banana` expects `sa=[5,3,1,0,4,2]`, `lcp=[0,1,3,0,0,2]`, `repeated="ana"` (overlapping occurrences); `aaaa` expects `sa=[3,2,1,0]`, `repeated="aaa"`. A buggy tie handling of the missing second key (-1) would order `a` and `aa` incorrectly.

### aho
- **Trie:** states are numbered in insertion order; `next[state*95 + (c-32)]` is the child. `patNode[i]` is the terminal state of dictionary position `i`; duplicates share a state.
- **Failure links / automaton:** a BFS over the trie sets `fail[v]` for a child `v` of `u` to `next[fail[u]][c]`, and a missing transition of `u` is filled with `next[fail[u]][c]` (root's missing transitions point to the root). After BFS every state has a full transition for all 95 characters, so scanning needs no fallback loops.
- **Counting:** for each text, `visits[state]` counts how often the scan ends in each state. Processing the BFS order in reverse, `visits[fail[v]] += visits[v]` pushes every visit to all of its suffix states. Then the count of pattern `i` is `visits[patNode[i]]`. This counts overlaps, patterns that are suffixes of other patterns (`he` inside `she`), and gives duplicates equal counts.
- **Reset:** `visits` is allocated fresh for each text, so texts are independent and an empty text gives zeros; an empty dictionary gives one empty row per text and an empty texts array gives `[]`.
- **Cost:** construction `O(L + 95*S + p)`; each text `O(|text| + S + p)`; memory `O(95*S + p)` plus output `q*p`. Reverse propagation avoids per-match work, which would be too slow on dense matches.
- **Boundary test:** patterns `["he","she","he"]`, texts `["sheshe","sheshe",""]` expect `[[2,2,2],[2,2,2],[0,0,0]]`. Missing the suffix propagation would give 0 for `he`; not resetting `visits` would corrupt the second row.

## Own tests

Fixtures are in `tests/student/cases.jsonl` (requests, one per line) and `tests/student/cases_expected.jsonl` (hand-derived expected results, same order; these cases have a unique optimum so exact comparison is valid). `tests/student/Check.java` compares the implementation against independent brute-force oracles on random inputs. The oracles are: brute-force LCS length plus subsequence check, memoised interval recursion plus replay of the plan, bitmask enumeration for the tree, `startsWith` scan for search, sorting of `substring` suffixes for the suffix array, and `startsWith` counting for Aho–Corasick.

| Command | Input or fixture path | Expected result | Actual result / log | What it checks |
|---|---|---|---|---|
| `java -cp out Check` | 2000 random LCS pairs (alphabet 3, length 0–8) | length equals oracle, sequence is a common subsequence | **[FILL: ALL CHECKS PASSED]** | length, subsequence property, empty strings |
| same | 500 random merge inputs (n 0–7) | cost equals recursion oracle; plan replays to the same cost with valid adjacency | **[FILL]** | cost, chronological order, adjacency |
| same | 1000 random trees (n 0–10) plus a 100000-vertex chain | weight equals bitmask optimum; selection independent and no duplicates | **[FILL]** | take/skip, reconstruction, deep chain (no stack overflow) |
| same | 3000 random text/pattern pairs for naive, kmp, rk | identical to the `startsWith` oracle (empty pattern = n+1 starts) | **[FILL]** | overlaps, empty pattern, m > n |
| same | `abdj` with `ab` and `dj` | `[0]` and `[2]` | **[FILL]** | known RK collision (both hash to 75) |
| same | 2000 random texts (length 0–19), `banana`, empty text | sa/lcp equal sorted-suffix oracle; `repeated` has max LCP length and occurs twice | **[FILL]** | doubling, Kasai, missing second key |
| same | 1500 random dictionaries and texts, the `he/she/he` example | counts equal `startsWith` counting | **[FILL]** | failure links, suffix patterns, duplicates, reset |
| `tests/student/cases.jsonl` | 20 requests mixing all tasks in one batch | `tests/student/cases_expected.jsonl` | **[FILL: result of running them through the launcher, see below]** | boundary values, stale state between tasks |

To run the fixtures through the launcher, use the local `--input` option described in the template's `docs/Assignment_4_EN.md` (for example `python tools/run.py --input tests/student/cases.jsonl`) and compare the output with `cases_expected.jsonl`. **[FILL: the exact command you used and whether the 20 results matched.]**

## Search comparison

Common domain (`n, m <= 3000`). Case "random": text uniform over `{a,b,c,d}`, pattern is a random window of the text, seed `20261108 + n` (generator `java.util.Random`). Case "repetitive": text `a^n`, pattern `a^(m-1)b`. Sizes `(n,m)` = (500,50), (1500,150), (3000,300). Before timing, each algorithm's output is compared with an independent `String.startsWith` oracle (column "Correct outputs checked").

Timing scope: one call to `Algorithms.naive/kmp/rk` on an already built request map (no JSON parsing, no compilation). Warm-up: 30 untimed calls per algorithm and case (JIT). A "run" is the mean time per call over 200 consecutive calls, in microseconds; 3 runs per cell, median reported. Command: `javac -d out Java/source/Algorithms.java tests/student/Bench.java && java -cp out Bench`.

**[FILL: paste the table printed by `Bench` on your own machine here. The "Run 1/2/3" columns are the raw times; do not edit the numbers.]**

### Observations
Fill these in after you have your numbers; use only what the table actually shows. Points to check against your data:
- Random case: the three algorithms are close, since mismatches usually occur at the first or second character, so naive is nearly linear there. KMP and RK carry setup cost (prefix table, hashing).
- Repetitive case: naive does about `(n-m+1)*m` comparisons, so its time grows much faster than KMP and RK as `n` and `m` grow.
- These timings show behaviour for these inputs only and are not a proof of asymptotic complexity; the JIT, caches and timer resolution add noise (see the spread between runs).
- RK with modulus 101: only 101 distinct hash values exist, so unrelated windows collide often (about 1 in 101 on random data). Each collision forces a character comparison, which is why `c*m` is part of the bound. This is specific to this deliberately small modulus, not to production-grade hashes with large moduli. In the repetitive case, the pattern `a^(m-1)b` hashes differently from most windows, so few candidates appear; **[FILL: check whether your RK is close to KMP there]**.

## Sources and AI assistance

- Course material: lecture notes and the interactive lesson and `docs/guide/CONTRACTS.md` of the assignment template.
- AI assistance: **[FILL and edit truthfully]** I used Claude (Anthropic) to help write and review the Java implementation, the test harness and a draft of this analysis. My own verification: I read and ran the code, ran `python tools/run.py --check` and the Public checks workflow in GitHub Actions, ran `Check.java` against brute-force oracles, and ran `Bench.java` on my own machine; the numbers above are from my runs.
