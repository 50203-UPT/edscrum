package pt.up.edscrum.dto.dashboard;

public class RankingDTO {

    private Long id;
    private String name;
    private Long totalPoints;

    public RankingDTO(Long id, String name, Long totalPoints) {
        this.id = id;
        this.name = name;
        this.totalPoints = totalPoints;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getTotalPoints() {
        return totalPoints;
    }

    public void setTotalPoints(Long totalPoints) {
        this.totalPoints = totalPoints;
    }
}
