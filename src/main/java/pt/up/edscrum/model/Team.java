package pt.up.edscrum.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;

@Entity
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @ManyToOne
    @JoinColumn(name = "project_id")
    @JsonIgnoreProperties({"teams", "sprints", "course"})
    private Project project;

    @ManyToOne
    @JoinColumn(name = "course_id")
    @JsonIgnoreProperties({"projects", "enrollments", "teams"})
    private Course course;

    // Papéis Scrum
    @ManyToOne
    @JoinColumn(name = "scrum_master_id")
    private User scrumMaster;

    @ManyToOne
    @JoinColumn(name = "product_owner_id")
    private User productOwner;

    // Developers (lista de membros)
    @ManyToMany
    @JoinTable(
            name = "team_developers",
            joinColumns = @JoinColumn(name = "team_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private List<User> developers;

    // Team closure and capacity management
    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean isClosed = false;

    @Column(nullable = false, columnDefinition = "INT DEFAULT 10")
    private Integer maxMembers = 10; // Default max is 10 (1 PO + 1 SM + 8 Devs)

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

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public User getScrumMaster() {
        return scrumMaster;
    }

    public void setScrumMaster(User scrumMaster) {
        this.scrumMaster = scrumMaster;
    }

    public User getProductOwner() {
        return productOwner;
    }

    public void setProductOwner(User productOwner) {
        this.productOwner = productOwner;
    }

    public List<User> getDevelopers() {
        return developers;
    }

    public void setDevelopers(List<User> developers) {
        this.developers = developers;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public boolean isClosed() {
        return isClosed;
    }

    public void setClosed(boolean closed) {
        isClosed = closed;
    }

    public Integer getMaxMembers() {
        return maxMembers;
    }

    public void setMaxMembers(Integer maxMembers) {
        this.maxMembers = maxMembers;
    }

    /**
     * Gets the total number of current team members (PO + SM + Developers)
     */
    public int getCurrentMemberCount() {
        int count = 0;
        if (this.productOwner != null) {
            count++;
        }
        if (this.scrumMaster != null) {
            count++;
        }
        if (this.developers != null) {
            count += this.developers.size();
        }
        return count;
    }

    /**
     * Checks if team can accept new members
     */
    public boolean canAcceptMembers() {
        return !isClosed && getCurrentMemberCount() < maxMembers;
    }

    /**
     * Checks if team is full (equal to max members)
     */
    public boolean isFull() {
        return getCurrentMemberCount() >= maxMembers;
    }
}
