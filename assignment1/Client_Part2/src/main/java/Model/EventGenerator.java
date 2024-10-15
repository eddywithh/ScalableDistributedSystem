package Model;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadLocalRandom;

public class EventGenerator implements Runnable{
    private static final int startResortID = 1;
    private static final int endResortID = 10;
    private static final String seasonID = "2024";
    private static final String dayID = "1";
    private static final int startSkierID = 1;
    private static final int endSkierID = 100000;
    private static final int startLiftID = 1;
    private static final int endLiftID = 40;
    private static final int startTime = 1;
    private static final int endTime = 360;
    private static final int stopResortID = 11;
    private BlockingQueue<LiftRideEvent> events;
    private int total_num;

    public EventGenerator(BlockingQueue<LiftRideEvent> events, int total_num) {
        this.events = events;
        this.total_num = total_num;
    }

    @Override
    public void run() {
        for (int i = 0; i < this.total_num; i++) {
            int resortID = ThreadLocalRandom.current().nextInt(startResortID, endResortID + 1);
            int skierID = ThreadLocalRandom.current().nextInt(startSkierID, endSkierID + 1);
            int liftID = ThreadLocalRandom.current().nextInt(startLiftID, endLiftID + 1);
            int time = ThreadLocalRandom.current().nextInt(startTime, endTime + 1);
            LiftRideEvent curEvent = new LiftRideEvent(resortID, seasonID, dayID, skierID, liftID, time);
            this.events.offer(curEvent);
        }

        int stopSkierID = ThreadLocalRandom.current().nextInt(startSkierID, endSkierID + 1);
        int stopLiftID = ThreadLocalRandom.current().nextInt(startLiftID, endLiftID + 1);
        int stopTime = ThreadLocalRandom.current().nextInt(startTime, endTime + 1);
        LiftRideEvent stopEvent = new LiftRideEvent(this.stopResortID, seasonID, dayID, stopSkierID, stopLiftID, stopTime);
        this.events.offer(stopEvent);
    }
}
