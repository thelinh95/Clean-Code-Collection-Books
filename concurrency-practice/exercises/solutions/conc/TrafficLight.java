package conc;

public class TrafficLight {
    private final Object lock = new Object();
    private int greenRoad = 1;

    public void carArrived(int carId, int roadId, int direction,
                           Runnable turnGreen, Runnable crossCar) {
        synchronized (lock) {
            if (greenRoad != roadId) {
                turnGreen.run();
                greenRoad = roadId;
            }
            crossCar.run();
        }
    }
}
