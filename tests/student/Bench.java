import java.util.*;

/**
 * Search comparison for naive / KMP / exact Rabin-Karp (base 31, mod 101).
 * Timing scope: one call to Algorithms.naive/kmp/rk on an already-built request map
 * (no JSON parsing, no compilation). Warm-up: 30 untimed calls per algorithm/case.
 * One "run" = mean time per call over REPS consecutive calls, in microseconds; 3 runs, median reported.
 * Correctness: all three outputs are compared with an independent String.startsWith oracle first.
 * Usage: java Bench [seed]     (default seed 20261108)
 */
public class Bench {
    static final int REPS = 200, WARM = 30, RUNS = 3;

    static Map<String, Object> req(String t, String p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("text", t); m.put("pattern", p);
        return m;
    }
    static List<Integer> oracle(String t, String p) {
        List<Integer> r = new ArrayList<>();
        for (int i = 0; i + p.length() <= t.length(); i++) if (t.startsWith(p, i)) r.add(i);
        return r;
    }
    static String rand(Random r, int n, int sig) {
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) sb.append((char) ('a' + r.nextInt(sig)));
        return sb.toString();
    }
    static String rep(char c, int n) { char[] a = new char[n]; Arrays.fill(a, c); return new String(a); }

    interface Algo { Map<String, Object> run(Map<String, Object> in); }

    public static void main(String[] args) {
        long seed = args.length > 0 ? Long.parseLong(args[0]) : 20261108L;
        String[] names = {"naive", "kmp", "rk"};
        Algo[] algos = {Algorithms::naive, Algorithms::kmp, Algorithms::rk};
        int[][] sizes = {{500, 50}, {1500, 150}, {3000, 300}};
        System.out.println("| Case | n | m | Seed/generator | Algorithm | Run 1 (us) | Run 2 (us) | Run 3 (us) | Median (us) | Correct outputs checked |");
        System.out.println("|---|---|---|---|---|---|---|---|---|---|");
        for (String kind : new String[]{"random", "repetitive"}) {
            for (int[] nm : sizes) {
                int n = nm[0], m = nm[1];
                Random rnd = new Random(seed + n);
                String t, p, gen;
                if (kind.equals("random")) {
                    t = rand(rnd, n, 4);                           // text: uniform over {a,b,c,d}
                    int at = rnd.nextInt(n - m + 1);               // pattern: a random window of the text
                    p = t.substring(at, at + m);                   // (guarantees at least one match)
                    gen = "Random(" + (seed + n) + "), alphabet {a,b,c,d}, pattern=text window";
                } else {
                    t = rep('a', n);                               // text: a^n
                    p = rep('a', m - 1) + "b";                     // pattern: a^(m-1) b  -> no match, long partial matches
                    gen = "fixed: text=a^n, pattern=a^(m-1)b";
                }
                List<Integer> exp = oracle(t, p);
                Map<String, Object> in = req(t, p);
                for (int a = 0; a < 3; a++) {
                    boolean ok = algos[a].run(in).get("positions").equals(exp);
                    for (int i = 0; i < WARM; i++) algos[a].run(in);
                    double[] us = new double[RUNS];
                    for (int r = 0; r < RUNS; r++) {
                        long t0 = System.nanoTime();
                        for (int i = 0; i < REPS; i++) algos[a].run(in);
                        us[r] = (System.nanoTime() - t0) / 1000.0 / REPS;
                    }
                    double[] s = us.clone(); Arrays.sort(s);
                    System.out.printf(Locale.ROOT, "| %s | %d | %d | %s | %s | %.1f | %.1f | %.1f | %.1f | %s (%d starts) |%n",
                            kind, n, m, gen, names[a], us[0], us[1], us[2], s[1], ok ? "yes" : "NO", exp.size());
                }
            }
        }
    }
}
