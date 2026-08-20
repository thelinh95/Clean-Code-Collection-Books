package conc;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class DiningPhilosophers {
    private final Lock[] forks;

    public DiningPhilosophers(int n) {
        forks = new Lock[n];
        for (int i = 0; i < n; i++) {
            forks[i] = new ReentrantLock();
        }
    }

    public void eat(int id, Runnable bite) throws InterruptedException {
        int n = forks.length;
        int left = id;
        int right = (id + 1) % n;
        int first = Math.min(left, right);
        int second = Math.max(left, right);
        forks[first].lock();
        try {
            forks[second].lock();
            try {
                bite.run();
            } finally {
                forks[second].unlock();
            }
        } finally {
            forks[first].unlock();
        }
    }
}
