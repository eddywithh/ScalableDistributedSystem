package Model;

public class LiftRideEvent {
    private int resortId;
    private String seasonId;
    private String dayId;
    private int skierId;
    private int liftID;
    private int time;

    public LiftRideEvent(int resortId, String seasonId, String dayId, int skierId, int liftID, int time) {
        this.resortId = resortId;
        this.seasonId = seasonId;
        this.dayId = dayId;
        this.skierId = skierId;
        this.liftID = liftID;
        this.time = time;
    }

    public int getResortId() {
        return this.resortId;
    }

    public String getSeasonId() {
        return this.seasonId;
    }

    public String getDayId() {
        return this.dayId;
    }

    public int getSkierId() {
        return this.skierId;
    }

    public int getLiftID() {
        return this.liftID;
    }

    public int getTime() {
        return this.time;
    }
}
