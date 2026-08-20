package conc;

import java.util.concurrent.ConcurrentHashMap;

public class Bank {
    private static final Object TIE_LOCK = new Object();

    private static final class Account {
        long bal;

        Account(long bal) {
            this.bal = bal;
        }
    }

    private final ConcurrentHashMap<String, Account> accounts = new ConcurrentHashMap<>();

    public void open(String id, long initial) {
        Account prev = accounts.putIfAbsent(id, new Account(initial));
        if (prev != null) {
            throw new IllegalStateException("exists: " + id);
        }
    }

    public void transfer(String fromId, String toId, long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("amount");
        }
        if (fromId.equals(toId)) {
            return;
        }
        Account from = accounts.get(fromId);
        Account to = accounts.get(toId);
        if (from == null || to == null) {
            throw new IllegalArgumentException("unknown account");
        }
        int hf = System.identityHashCode(from);
        int ht = System.identityHashCode(to);
        if (hf < ht) {
            synchronized (from) {
                synchronized (to) {
                    move(from, to, amount);
                }
            }
        } else if (hf > ht) {
            synchronized (to) {
                synchronized (from) {
                    move(from, to, amount);
                }
            }
        } else {
            synchronized (TIE_LOCK) {
                synchronized (from) {
                    synchronized (to) {
                        move(from, to, amount);
                    }
                }
            }
        }
    }

    private static void move(Account from, Account to, long amount) {
        from.bal -= amount;
        to.bal += amount;
    }

    public long balance(String id) {
        Account a = accounts.get(id);
        if (a == null) {
            throw new IllegalArgumentException("unknown account");
        }
        synchronized (a) {
            return a.bal;
        }
    }

    public long total() {
        long sum = 0;
        for (Account a : accounts.values()) {
            synchronized (a) {
                sum += a.bal;
            }
        }
        return sum;
    }
}
