package Model;

public class LiftRide {
    private String resortID;
    private String seasonID;
    private String dayID;
    private String skierID;
    private String time;
    private String liftID;

    public LiftRide(String resortID, String seasonID, String dayID, String skierID, String time, String liftID) {
        this.resortID = resortID;
        this.seasonID = seasonID;
        this.dayID = dayID;
        this.skierID = skierID;
        this.time = time;
        this.liftID = liftID;
    }

    public String getResortID() {
        return this.resortID;
    }

    public String getSeasonID() {
        return this.seasonID;
    }

    public String getDayID() {
        return this.dayID;
    }

    public String getSkierID() {
        return this.skierID;
    }

    public void setResortID(String resortID) {
        this.resortID = resortID;
    }

    public void setSeasonID(String seasonID) {
        this.seasonID = seasonID;
    }

    public void setDayID(String dayID) {
        this.dayID = dayID;
    }

    public void setSkierID(String skierID) {
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
