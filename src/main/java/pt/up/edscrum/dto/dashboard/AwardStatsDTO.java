package pt.up.edscrum.dto.dashboard;

public class AwardStatsDTO {
    private String awardName;
    private long count;

    public String getAwardName() {
        return awardName;
    }
    public void setAwardName(String awardName) {
        this.awardName = awardName;
    }
    public long getCount() {
        return count;
    }
    public void setCount(long count) {
        this.count = count;
    }
}