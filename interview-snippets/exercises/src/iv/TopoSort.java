package iv;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

/**
 * Kahn topological sort. Cycle → list rỗng (hoặc ném). Build system / course schedule.
 */
public final class TopoSort {
    private TopoSort() {
    }

    public static List<Integer> kahn(int n, int[][] edges) {
        List<List<Integer>> adj = new ArrayList<List<Integer>>(n);
        int[] indeg = new int[n];
        for (int i = 0; i < n; i++) {
            adj.add(new ArrayList<Integer>());
        }
        for (int[] e : edges) {
            adj.get(e[0]).add(e[1]);
            indeg[e[1]]++;
        }
        Deque<Integer> q = new ArrayDeque<Integer>();
        for (int i = 0; i < n; i++) {
            if (indeg[i] == 0) {
                q.addLast(i);
            }
        }
        List<Integer> order = new ArrayList<Integer>(n);
        while (!q.isEmpty()) {
            int u = q.removeFirst();
            order.add(u);
            for (int v : adj.get(u)) {
                indeg[v]--;
                if (indeg[v] == 0) {
                    q.addLast(v);
                }
            }
        }
        return order.size() == n ? order : Collections.<Integer>emptyList();
    }
}
