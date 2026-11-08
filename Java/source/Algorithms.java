import java.util.*;

/** Implementation of all A4 algorithms. docs/guide/CONTRACTS.md defines each field and limit. */
public final class Algorithms {
    private static final int SIGMA = 95;   // printable ASCII 32..126
    private static final int BASE = 32;

    private static Map<String, Object> out() { return new LinkedHashMap<>(); }

    // ---------------------------------------------------------------- 1. LCS
    /** dp[i][j] = LCS length of a[0..i) and b[0..j). */
    static Map<String, Object> lcs(Map<String, Object> input) {
        String a = (String) input.get("a"), b = (String) input.get("b");
        int n = a.length(), m = b.length();
        int[][] dp = new int[n + 1][m + 1];
        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {
                if (a.charAt(i - 1) == b.charAt(j - 1)) dp[i][j] = dp[i - 1][j - 1] + 1;
                else dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
            }
        }
        StringBuilder sb = new StringBuilder();
        int i = n, j = m;
        while (i > 0 && j > 0) {
            if (a.charAt(i - 1) == b.charAt(j - 1)) { sb.append(a.charAt(i - 1)); i--; j--; }
            else if (dp[i - 1][j] >= dp[i][j - 1]) i--;
            else j--;
        }
        Map<String, Object> r = out();
        r.put("length", dp[n][m]);
        r.put("sequence", sb.reverse().toString());
        return r;
    }

    // ------------------------------------------------------------- 2. Merge
    /** dp[l][r] = min cost to merge blocks l..r into one; last merge splits at k. */
    @SuppressWarnings("unchecked")
    static Map<String, Object> merge(Map<String, Object> input) {
        List<Object> raw = (List<Object>) input.get("sizes");
        int n = raw.size();
        long[] pre = new long[n + 1];
        for (int i = 0; i < n; i++) pre[i + 1] = pre[i] + ((Number) raw.get(i)).longValue();
        List<List<Integer>> plan = new ArrayList<>();
        long cost = 0;
        if (n > 1) {
            long[][] dp = new long[n][n];
            int[][] sp = new int[n][n];
            for (int len = 2; len <= n; len++) {
                for (int l = 0; l + len - 1 < n; l++) {
                    int r = l + len - 1;
                    long best = Long.MAX_VALUE;
                    int bk = l;
                    for (int k = l; k < r; k++) {
                        long c = dp[l][k] + dp[k + 1][r];
                        if (c < best) { best = c; bk = k; }
                    }
                    dp[l][r] = best + pre[r + 1] - pre[l];
                    sp[l][r] = bk;
                }
            }
            cost = dp[0][n - 1];
            buildPlan(0, n - 1, sp, plan);
        }
        Map<String, Object> r = out();
        r.put("cost", cost);
        r.put("plan", plan);
        return r;
    }

    /** Children are emitted before their parent, so the plan is chronological. */
    private static void buildPlan(int l, int r, int[][] sp, List<List<Integer>> plan) {
        if (l == r) return;
        int k = sp[l][r];
        buildPlan(l, k, sp, plan);
        buildPlan(k + 1, r, sp, plan);
        plan.add(Arrays.asList(l, k, r));
    }

    // -------------------------------------------------------------- 3. Tree
    /** take[u]/skip[u] = best weight in subtree of u with u taken / not taken. */
    @SuppressWarnings("unchecked")
    static Map<String, Object> tree(Map<String, Object> input) {
        int n = ((Number) input.get("n")).intValue();
        List<Integer> selected = new ArrayList<>();
        long total = 0;
        if (n > 0) {
            List<Object> w = (List<Object>) input.get("weights");
            List<Object> edges = (List<Object>) input.get("edges");
            long[] wt = new long[n];
            for (int i = 0; i < n; i++) wt[i] = ((Number) w.get(i)).longValue();
            int[] head = new int[n];
            Arrays.fill(head, -1);
            int[] nxt = new int[2 * n], to = new int[2 * n];
            int ec = 0;
            for (Object eo : edges) {
                List<Object> e = (List<Object>) eo;
                int u = ((Number) e.get(0)).intValue(), v = ((Number) e.get(1)).intValue();
                to[ec] = v; nxt[ec] = head[u]; head[u] = ec++;
                to[ec] = u; nxt[ec] = head[v]; head[v] = ec++;
            }
            int[] parent = new int[n], order = new int[n], stack = new int[n];
            boolean[] seen = new boolean[n];
            int sp = 0, cnt = 0;
            parent[0] = -1; seen[0] = true; stack[sp++] = 0;
            while (sp > 0) {                       // iterative DFS, no recursion
                int u = stack[--sp];
                order[cnt++] = u;
                for (int e = head[u]; e != -1; e = nxt[e]) {
                    int v = to[e];
                    if (!seen[v]) { seen[v] = true; parent[v] = u; stack[sp++] = v; }
                }
            }
            long[] take = new long[n], skip = new long[n];
            for (int i = n - 1; i >= 0; i--) {     // children before parents
                int u = order[i];
                take[u] += wt[u];
                int p = parent[u];
                if (p >= 0) { take[p] += skip[u]; skip[p] += Math.max(take[u], skip[u]); }
            }
            boolean[] taken = new boolean[n];
            for (int i = 0; i < n; i++) {          // parents before children
                int u = order[i], p = parent[u];
                boolean t = (p < 0 || !taken[p]) && take[u] > skip[u];
                taken[u] = t;
                if (t) selected.add(u);
            }
            total = Math.max(take[0], skip[0]);
            Collections.sort(selected);
        }
        Map<String, Object> r = out();
        r.put("weight", total);
        r.put("selected", selected);
        return r;
    }

    // -------------------------------------------------------------- 4. Search
    static Map<String, Object> naive(Map<String, Object> input) {
        String t = (String) input.get("text"), p = (String) input.get("pattern");
        int n = t.length(), m = p.length();
        List<Integer> pos = new ArrayList<>();
        if (m == 0) {
            for (int i = 0; i <= n; i++) pos.add(i);
        } else {
            for (int i = 0; i + m <= n; i++) {
                int j = 0;
                while (j < m && t.charAt(i + j) == p.charAt(j)) j++;
                if (j == m) pos.add(i);
            }
        }
        Map<String, Object> r = out();
        r.put("positions", pos);
        return r;
    }

    /** pi[i] = length of the longest proper prefix of p[0..i] that is also its suffix. */
    static Map<String, Object> kmp(Map<String, Object> input) {
        String t = (String) input.get("text"), p = (String) input.get("pattern");
        int n = t.length(), m = p.length();
        List<Integer> pos = new ArrayList<>();
        if (m == 0) {
            for (int i = 0; i <= n; i++) pos.add(i);
        } else if (m <= n) {
            int[] pi = new int[m];
            for (int i = 1, k = 0; i < m; i++) {
                while (k > 0 && p.charAt(i) != p.charAt(k)) k = pi[k - 1];
                if (p.charAt(i) == p.charAt(k)) k++;
                pi[i] = k;
            }
            for (int i = 0, k = 0; i < n; i++) {
                while (k > 0 && t.charAt(i) != p.charAt(k)) k = pi[k - 1];
                if (t.charAt(i) == p.charAt(k)) k++;
                if (k == m) { pos.add(i - m + 1); k = pi[k - 1]; }
            }
        }
        Map<String, Object> r = out();
        r.put("positions", pos);
        return r;
    }

    /** Polynomial rolling hash, base 31, modulus 101; equal hashes are verified. */
    static Map<String, Object> rk(Map<String, Object> input) {
        String t = (String) input.get("text"), p = (String) input.get("pattern");
        int n = t.length(), m = p.length();
        final long B = 31, MOD = 101;
        List<Integer> pos = new ArrayList<>();
        if (m == 0) {
            for (int i = 0; i <= n; i++) pos.add(i);
        } else if (m <= n) {
            long high = 1;
            for (int i = 1; i < m; i++) high = high * B % MOD;
            long hp = 0, ht = 0;
            for (int i = 0; i < m; i++) {
                hp = (hp * B + p.charAt(i)) % MOD;
                ht = (ht * B + t.charAt(i)) % MOD;
            }
            for (int i = 0; i + m <= n; i++) {
                if (hp == ht) {
                    int j = 0;
                    while (j < m && t.charAt(i + j) == p.charAt(j)) j++;
                    if (j == m) pos.add(i);
                }
                if (i + m < n) {
                    ht = ((ht - t.charAt(i) * high % MOD + MOD) % MOD * B + t.charAt(i + m)) % MOD;
                }
            }
        }
        Map<String, Object> r = out();
        r.put("positions", pos);
        return r;
    }

    // ----------------------------------------------------------- 5. Suffix
    /** Prefix doubling with counting sort of rank pairs (O(n log n)) + Kasai LCP. */
    static Map<String, Object> suffix(Map<String, Object> input) {
        String s = (String) input.get("text");
        int n = s.length();
        int[] sa = new int[n], lcp = new int[n];
        String repeated = "";
        if (n > 0) {
            int[] rnk = new int[n], tmp = new int[n], nr = new int[n];
            int[] cnt = new int[Math.max(n, 128) + 2];
            for (int i = 0; i < n; i++) cnt[s.charAt(i) + 1]++;
            for (int i = 1; i < 130 && i < cnt.length; i++) cnt[i] += cnt[i - 1];
            for (int i = 0; i < n; i++) sa[cnt[s.charAt(i)]++] = i;
            rnk[sa[0]] = 0;
            for (int i = 1; i < n; i++)
                rnk[sa[i]] = rnk[sa[i - 1]] + (s.charAt(sa[i]) != s.charAt(sa[i - 1]) ? 1 : 0);
            int classes = rnk[sa[n - 1]] + 1;
            for (int k = 1; k < n && classes < n; k <<= 1) {
                int p = 0;
                for (int i = n - k; i < n; i++) tmp[p++] = i;          // second key = -1
                for (int i = 0; i < n; i++) if (sa[i] >= k) tmp[p++] = sa[i] - k;
                int[] c = new int[classes + 1];
                for (int i = 0; i < n; i++) c[rnk[i] + 1]++;
                for (int i = 1; i <= classes; i++) c[i] += c[i - 1];
                for (int i = 0; i < n; i++) sa[c[rnk[tmp[i]]]++] = tmp[i];
                nr[sa[0]] = 0;
                for (int i = 1; i < n; i++) {
                    int a = sa[i - 1], b = sa[i];
                    int sa2 = a + k < n ? rnk[a + k] : -1, sb2 = b + k < n ? rnk[b + k] : -1;
                    nr[b] = nr[a] + ((rnk[a] == rnk[b] && sa2 == sb2) ? 0 : 1);
                }
                int[] sw = rnk; rnk = nr; nr = sw;
                classes = rnk[sa[n - 1]] + 1;
            }
            int[] rank = new int[n];
            for (int i = 0; i < n; i++) rank[sa[i]] = i;
            int h = 0, best = 0, bestAt = 0;
            for (int i = 0; i < n; i++) {                              // Kasai
                if (rank[i] > 0) {
                    int j = sa[rank[i] - 1];
                    while (i + h < n && j + h < n && s.charAt(i + h) == s.charAt(j + h)) h++;
                    lcp[rank[i]] = h;
                    if (h > best) { best = h; bestAt = sa[rank[i]]; }
                    if (h > 0) h--;
                } else {
                    h = 0;
                }
            }
            if (best > 0) repeated = s.substring(bestAt, bestAt + best);
        }
        Map<String, Object> r = out();
        r.put("sa", sa);
        r.put("lcp", lcp);
        r.put("repeated", repeated);
        return r;
    }

    // --------------------------------------------------------- 6. Aho-Corasick
    /** One automaton; visit counts are pushed along failure links in reverse BFS order. */
    @SuppressWarnings("unchecked")
    static Map<String, Object> aho(Map<String, Object> input) {
        List<Object> pats = (List<Object>) input.get("patterns");
        List<Object> texts = (List<Object>) input.get("texts");
        int p = pats.size();
        int total = 0;
        for (Object o : pats) total += ((String) o).length();
        int maxStates = total + 1;
        int[] next = new int[maxStates * SIGMA];
        Arrays.fill(next, -1);
        int states = 1;
        int[] patNode = new int[p];
        for (int i = 0; i < p; i++) {
            String s = (String) pats.get(i);
            int node = 0;
            for (int j = 0; j < s.length(); j++) {
                int c = s.charAt(j) - BASE;
                if (next[node * SIGMA + c] == -1) next[node * SIGMA + c] = states++;
                node = next[node * SIGMA + c];
            }
            patNode[i] = node;
        }
        int[] fail = new int[states], bfs = new int[states];
        int qh = 0, qt = 0;
        for (int c = 0; c < SIGMA; c++) {
            int v = next[c];
            if (v == -1) next[c] = 0;
            else { fail[v] = 0; bfs[qt++] = v; }
        }
        while (qh < qt) {
            int u = bfs[qh++];
            for (int c = 0; c < SIGMA; c++) {
                int v = next[u * SIGMA + c];
                if (v == -1) next[u * SIGMA + c] = next[fail[u] * SIGMA + c];
                else { fail[v] = next[fail[u] * SIGMA + c]; bfs[qt++] = v; }
            }
        }
        List<List<Long>> counts = new ArrayList<>();
        for (Object to : texts) {
            String t = (String) to;
            long[] visits = new long[states];
            int node = 0;
            for (int i = 0; i < t.length(); i++) {
                node = next[node * SIGMA + (t.charAt(i) - BASE)];
                visits[node]++;
            }
            for (int i = qt - 1; i >= 0; i--) visits[fail[bfs[i]]] += visits[bfs[i]];
            List<Long> row = new ArrayList<>(p);
            for (int i = 0; i < p; i++) row.add(visits[patNode[i]]);
            counts.add(row);
        }
        Map<String, Object> r = out();
        r.put("counts", counts);
        return r;
    }
}
