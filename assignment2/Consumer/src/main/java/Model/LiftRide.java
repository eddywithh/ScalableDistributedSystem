package Model;

public class LiftRide {
    private Integer resortID;
    private Integer seasonID;
    private Integer dayID;
    private Integer skierID;
    private String time;
    private String liftID;

    public LiftRide(Integer resortID, Integer seasonID, Integer dayID, Integer skierID, String time, String liftID) {
        this.resortID = resortID;
        this.seasonID = seasonID;
        this.dayID = dayID;
        this.skierID = skierID;
        this.time = time;
        this.liftID = liftID;
    }

    public Integer getResortID() {
        return this.resortID;
    }

    public Integer getSeasonID() {
        return this.seasonID;
    }

    public Integer getDayID() {
        return this.dayID;
    }

    public Integer getSkierID() {
        return this.skierID;
    }

    public void setResortID(Integer resortID) {
        this.resortID = resortID;
    }

    public void setSeasonID(Integer seasonID) {
        this.seasonID = seasonID;
    }

    public void setDayID(Integer dayID) {
        this.dayID = dayID;
    }

    public void setSkierID(Integer skierID) {
        this.skierID = skierID;
    }
    public String getTime() {
        return this.time;
    }

    public String getLiftID() {
        return this.liftID;
    }

    public String toString() {
        return "Time: " + this.time + ", " + "LiftID: " + this.liftID;
    }
}
