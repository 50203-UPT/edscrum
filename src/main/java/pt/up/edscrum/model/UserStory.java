package pt.up.edscrum.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import pt.up.edscrum.enums.UserStoryPriority;
import pt.up.edscrum.enums.UserStoryStatus;

@Entity
public class UserStory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String description;
    private Integer storyPoints; // Esforço estimado

    @Enumerated(EnumType.STRING)
    private UserStoryPriority priority;

    @Enumerated(EnumType.STRING)
    private UserStoryStatus status = UserStoryStatus.TODO;

    @ManyToOne
    @JoinColumn(name = "sprint_id")
    private Sprint sprint;

    @ManyToOne
    @JoinColumn(name = "assignee_id")
    private User assignee; // Pode ser null (Unassigned)

    // Getters e Setters
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getStoryPoints() {
        return storyPoints;
    }

    public void setStoryPoints(Integer storyPoints) {
        this.storyPoints = storyPoints;
    }

    public UserStoryPriority getPriority() {
        return priority;
    }

    public void setPriority(UserStoryPriority priority) {
        this.priority = priority;
    }

    public UserStoryStatus getStatus() {
        return status;
    }

    public void setStatus(UserStoryStatus status) {
        this.status = status;
    }

    public Sprint getSprint() {
        return sprint;
    }

    public void setSprint(Sprint sprint) {
        this.sprint = sprint;
    }

    public User getAssignee() {
        return assignee;
    }

    public void setAssignee(User assignee) {
        this.assignee = assignee;
    }
}
