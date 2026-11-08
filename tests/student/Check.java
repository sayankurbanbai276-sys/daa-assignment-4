import java.util.*;

/** Randomised check of Algorithms against independent brute-force oracles. */
public class Check {
    static int fails = 0;
    static void eq(boolean ok, String what) { if (!ok) { fails++; System.out.println("FAIL " + what); } }

    static Map<String,Object> m(Object... kv) {
        Map<String,Object> r = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) r.put((String) kv[i], kv[i + 1]);
        return r;
    }
    static String rs(Random r, int n, int sig) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append((char) ('a' + r.nextInt(sig)));
        return sb.toString();
    }
    static boolean isSub(String s, String t) {
        int j = 0;
        for (int i = 0; i < t.length() && j < s.length(); i++) if (t.charAt(i) == s.charAt(j)) j++;
        return j == s.length();
    }
    static int lcsLen(String a, String b) {
        int[][] d = new int[a.length() + 1][b.length() + 1];
        for (int i = a.length() - 1; i >= 0; i--) for (int j = b.length() - 1; j >= 0; j--)
            d[i][j] = a.charAt(i) == b.charAt(j) ? 1 + d[i + 1][j + 1] : Math.max(d[i + 1][j], d[i][j + 1]);
        return d[0][0];
    }
    static List<Integer> occ(String t, String p) {
        List<Integer> r = new ArrayList<>();
        for (int i = 0; i <= t.length() - p.length(); i++) if (t.startsWith(p, i)) r.add(i);
        return r;
    }
    // memoised recursion oracle for merge: min cost of merging l..r
    static long mergeOracle(long[] s, int l, int r, Map<Long,Long> memo) {
        if (l == r) return 0;
        long key = l * 1000L + r;
        Long v = memo.get(key); if (v != null) return v;
        long sum = 0; for (int i = l; i <= r; i++) sum += s[i];
        long best = Long.MAX_VALUE;
        for (int k = l; k < r; k++) best = Math.min(best, mergeOracle(s, l, k, memo) + mergeOracle(s, k + 1, r, memo));
        memo.put(key, best + sum); return best + sum;
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] a) {
        Random rnd = new Random(12345);
        // ---- LCS
        for (int it = 0; it < 2000; it++) {
            String x = rs(rnd, rnd.nextInt(9), 3), y = rs(rnd, rnd.nextInt(9), 3);
            Map<String,Object> r = Algorithms.lcs(m("a", x, "b", y));
            String q = (String) r.get("sequence");
            eq((int) r.get("length") == lcsLen(x, y) && q.length() == lcsLen(x, y) && isSub(q, x) && isSub(q, y), "lcs " + x + "," + y);
        }
        // ---- merge
        for (int it = 0; it < 500; it++) {
            int n = rnd.nextInt(8);
            List<Object> sz = new ArrayList<>(); long[] s = new long[n];
            for (int i = 0; i < n; i++) { s[i] = rnd.nextInt(6); sz.add((int) s[i]); }
            Map<String,Object> r = Algorithms.merge(m("sizes", sz));
            long exp = n <= 1 ? 0 : mergeOracle(s, 0, n - 1, new HashMap<>());
            eq(((Number) r.get("cost")).longValue() == exp, "merge cost " + sz);
            // replay plan
            List<List<Integer>> plan = (List<List<Integer>>) r.get("plan");
            eq(plan.size() == Math.max(0, n - 1), "merge plan size " + sz);
            long cost = 0; boolean ok = true;
            Map<Integer,Integer> blocks = new TreeMap<>(); // l -> r
            for (int i = 0; i < n; i++) blocks.put(i, i);
            for (List<Integer> st : plan) {
                int l = st.get(0), k = st.get(1), rr = st.get(2);
                if (!Integer.valueOf(k).equals(blocks.get(l)) || !Integer.valueOf(rr).equals(blocks.get(k + 1))) { ok = false; break; }
                for (int i = l; i <= rr; i++) cost += s[i];
                blocks.remove(k + 1); blocks.put(l, rr);
            }
            eq(ok && cost == exp, "merge replay " + sz);
        }
        // ---- tree
        for (int it = 0; it < 1000; it++) {
            int n = rnd.nextInt(11);
            List<Object> w = new ArrayList<>(), edges = new ArrayList<>();
            long[] wt = new long[n]; int[] par = new int[n];
            for (int i = 0; i < n; i++) { wt[i] = rnd.nextInt(21) - 10; w.add((int) wt[i]); }
            boolean[][] adj = new boolean[n][n];
            for (int i = 1; i < n; i++) {
                int p = rnd.nextInt(i);
                int u = rnd.nextBoolean() ? p : i, v = u == p ? i : p;   // random orientation
                edges.add(Arrays.asList(u, v)); adj[p][i] = adj[i][p] = true;
            }
            // shuffle vertex labels? keep simple; also root-label independence via relabel below
            long best = 0;
            for (int mask = 0; mask < (1 << n); mask++) {
                boolean ok = true; long sum = 0;
                for (int i = 0; i < n && ok; i++) if ((mask >> i & 1) == 1) {
                    sum += wt[i];
                    for (int j = i + 1; j < n; j++) if ((mask >> j & 1) == 1 && adj[i][j]) { ok = false; break; }
                }
                if (ok) best = Math.max(best, sum);
            }
            Map<String,Object> r = Algorithms.tree(m("n", n, "weights", w, "edges", edges));
            List<Integer> sel = (List<Integer>) r.get("selected");
            long sum = 0; boolean ok = new HashSet<>(sel).size() == sel.size();
            for (int v : sel) { sum += wt[v]; for (int u : sel) if (adj[u][v]) ok = false; }
            eq(((Number) r.get("weight")).longValue() == best && sum == best && ok, "tree n=" + n + " w=" + w + " e=" + edges);
        }
        // deep chain must not overflow stack
        {
            int n = 100000; List<Object> w = new ArrayList<>(), e = new ArrayList<>();
            for (int i = 0; i < n; i++) { w.add(1); if (i > 0) e.add(Arrays.asList(i - 1, i)); }
            Map<String,Object> r = Algorithms.tree(m("n", n, "weights", w, "edges", e));
            eq(((Number) r.get("weight")).longValue() == 50000, "tree chain 100000");
        }
        // ---- search
        for (int it = 0; it < 3000; it++) {
            String t = rs(rnd, rnd.nextInt(30), 1 + rnd.nextInt(3)), p = rs(rnd, rnd.nextInt(6), 1 + rnd.nextInt(3));
            List<Integer> exp;
            if (p.isEmpty()) { exp = new ArrayList<>(); for (int i = 0; i <= t.length(); i++) exp.add(i); }
            else exp = occ(t, p);
            eq(Algorithms.naive(m("text", t, "pattern", p)).get("positions").equals(exp), "naive " + t + "/" + p);
            eq(Algorithms.kmp(m("text", t, "pattern", p)).get("positions").equals(exp), "kmp " + t + "/" + p);
            eq(Algorithms.rk(m("text", t, "pattern", p)).get("positions").equals(exp), "rk " + t + "/" + p);
        }
        eq(Algorithms.rk(m("text", "abdj", "pattern", "ab")).get("positions").equals(Arrays.asList(0)), "rk ab/dj collision");
        eq(Algorithms.rk(m("text", "abdj", "pattern", "dj")).get("positions").equals(Arrays.asList(2)), "rk dj");
        // ---- suffix
        for (int it = 0; it < 2000; it++) {
            String s = rs(rnd, rnd.nextInt(20), 1 + rnd.nextInt(3));
            int n = s.length();
            Integer[] idx = new Integer[n]; for (int i = 0; i < n; i++) idx[i] = i;
            Arrays.sort(idx, (x, y) -> s.substring(x).compareTo(s.substring(y)));
            Map<String,Object> r = Algorithms.suffix(m("text", s));
            int[] sa = (int[]) r.get("sa"), lcp = (int[]) r.get("lcp");
            boolean ok = sa.length == n && lcp.length == n; int best = 0;
            for (int i = 0; ok && i < n; i++) {
                ok &= sa[i] == idx[i];
                int h = 0;
                if (i > 0) { String u = s.substring(idx[i - 1]), v = s.substring(idx[i]); while (h < u.length() && h < v.length() && u.charAt(h) == v.charAt(h)) h++; }
                ok &= lcp[i] == h; best = Math.max(best, h);
            }
            String rep = (String) r.get("repeated");
            ok &= rep.length() == best && (best == 0 || occ(s, rep).size() >= 2);
            eq(ok, "suffix " + s);
        }
        eq(Arrays.equals((int[]) Algorithms.suffix(m("text", "banana")).get("sa"), new int[]{5,3,1,0,4,2}), "suffix banana");
        eq(((int[]) Algorithms.suffix(m("text", "")).get("sa")).length == 0, "suffix empty");
        // ---- aho
        for (int it = 0; it < 1500; it++) {
            int p = rnd.nextInt(6), q = rnd.nextInt(4);
            List<Object> pats = new ArrayList<>(), texts = new ArrayList<>();
            for (int i = 0; i < p; i++) pats.add(rs(rnd, 1 + rnd.nextInt(4), 2));
            for (int i = 0; i < q; i++) texts.add(rs(rnd, rnd.nextInt(15), 2));
            List<List<Long>> got = (List<List<Long>>) Algorithms.aho(m("patterns", pats, "texts", texts)).get("counts");
            boolean ok = got.size() == q;
            for (int i = 0; ok && i < q; i++) for (int j = 0; j < p; j++)
                ok &= got.get(i).get(j) == (long) occ((String) texts.get(i), (String) pats.get(j)).size();
            eq(ok, "aho " + pats + texts);
        }
        eq(Algorithms.aho(m("patterns", Arrays.asList("he","she","he"), "texts", Arrays.asList("sheshe","sheshe",""))).get("counts")
                .toString().equals("[[2, 2, 2], [2, 2, 2], [0, 0, 0]]"), "aho spec example");
        System.out.println(fails == 0 ? "ALL CHECKS PASSED" : fails + " FAILURES");
    }
}
