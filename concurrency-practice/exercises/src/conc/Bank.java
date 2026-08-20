package conc;

/**
 * Ngân hàng: mở tài khoản, chuyển tiền. Tổng tiền không đổi, không deadlock.
 */
public class Bank {
    public void open(String id, long initial) {
        throw new UnsupportedOperationException("TODO Bank.open");
    }

    public void transfer(String fromId, String toId, long amount) {
        throw new UnsupportedOperationException("TODO Bank.transfer");
    }

    public long balance(String id) {
        throw new UnsupportedOperationException("TODO Bank.balance");
    }

    public long total() {
        throw new UnsupportedOperationException("TODO Bank.total");
    }
}
