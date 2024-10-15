package cs6650.servlet.Model;

public class LiftRide {
    private int liftID;
    private int time;

    public LiftRide(int liftID, int time) {
        this.liftID = liftID;
        this.time = time;
    }

    public int getLiftID() {
        return liftID;
    }

    public void setLiftID(int liftID) {
        this.liftID = liftID;
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    @Override
    public String toString() {
        return "LiftRide{" +
                "liftId=" + liftID +
                ", time=" + time +
                '}';
    }
}

