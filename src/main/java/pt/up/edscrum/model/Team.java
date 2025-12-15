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

/**
 * Entidade que representa uma equipa de desenvolvimento dentro de um
 * projeto/curso, com membros, papéis (scrum master, product owner) e
 * informação de configuração (tamanho máximo, estado encerrado).
 */
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

    
    @ManyToOne
    @JoinColumn(name = "scrum_master_id")
    private User scrumMaster;

    @ManyToOne
    @JoinColumn(name = "product_owner_id")
    private User productOwner;

    
    @ManyToMany
    @JoinTable(
            name = "team_developers",
            joinColumns = @JoinColumn(name = "team_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private List<User> developers;

    
    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean isClosed = false;

    @Column(nullable = false, columnDefinition = "INT DEFAULT 10")
    private Integer maxMembers = 10;
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
     * Retorna o número total de membros atuais da equipa (PO + SM + Developers).
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
     * Indica se a equipa pode aceitar novos membros.
     */
    public boolean canAcceptMembers() {
        return !isClosed && getCurrentMemberCount() < maxMembers;
    }

    /**
     * Indica se a equipa atingiu a sua capacidade máxima.
     */
    public boolean isFull() {
        return getCurrentMemberCount() >= maxMembers;
    }
}
