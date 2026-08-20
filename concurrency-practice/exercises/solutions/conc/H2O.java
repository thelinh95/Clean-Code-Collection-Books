package conc;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Semaphore;

public class H2O {
    private final Semaphore h = new Semaphore(2);
    private final Semaphore o = new Semaphore(1);
    private final CyclicBarrier molecule = new CyclicBarrier(3, () -> {
        h.release(2);
        o.release(1);
    });

    public void hydrogen(Runnable releaseHydrogen) throws InterruptedException {
        h.acquire();
        releaseHydrogen.run();
        awaitMolecule();
    }

    public void oxygen(Runnable releaseOxygen) throws InterruptedException {
        o.acquire();
        releaseOxygen.run();
        awaitMolecule();
    }

    private void awaitMolecule() throws InterruptedException {
        try {
            molecule.await();
        } catch (BrokenBarrierException e) {
            Thread.currentThread().interrupt();
            throw new InterruptedException();
        }
    }
}
