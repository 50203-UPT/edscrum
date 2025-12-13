package pt.up.edscrum.dto.dashboard;

public class MemberWithRoleDTO {
    private Long id;
    private String name;
    private String role;
    private int awardsCount;
    private int individualXP;

    public MemberWithRoleDTO() {
    }

    public MemberWithRoleDTO(Long id, String name, String role) {
        this.id = id;
        this.name = name;
        this.role = role;
        this.awardsCount = 0;
        this.individualXP = 0;
    }

    public MemberWithRoleDTO(Long id, String name, String role, int awardsCount, int individualXP) {
        this.id = id;
        this.name = name;
        this.role = role;
        this.awardsCount = awardsCount;
        this.individualXP = individualXP;
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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public int getAwardsCount() {
        return awardsCount;
    }

    public void setAwardsCount(int awardsCount) {
        this.awardsCount = awardsCount;
    }

    public int getIndividualXP() {
        return individualXP;
    }

    public void setIndividualXP(int individualXP) {
        this.individualXP = individualXP;
    }
}
