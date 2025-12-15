package pt.up.edscrum.dto.dashboard;

/**
 * DTO para estatísticas agregadas de prémios (nome do prémio e contagem).
 */
public class AwardStatsDTO {

    private String awardName;
    private long count;

    public AwardStatsDTO(String awardName, long count) {
        this.awardName = awardName;
        this.count = count;
    }

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
