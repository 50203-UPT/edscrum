package pt.up.edscrum.model;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import pt.up.edscrum.enums.ProjectStatus;
import pt.up.edscrum.enums.SprintStatus;

/**
 * Representa um projeto de um curso, contendo sprints, equipas, datas
 * e utilitários de progresso.
 */
@Entity
public class Project {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String sprintGoals;

    private LocalDate startDate;
    private LocalDate endDate;

    @ManyToOne
    @JoinColumn(name = "course_id")
    @JsonIgnoreProperties({"projects", "enrollments", "teams"})
    private Course course;

    @OneToMany(mappedBy = "project")
    @JsonIgnoreProperties({"project", "course"})
    private List<Team> teams;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL)
    @JsonIgnoreProperties("project")
    private List<Sprint> sprints;

    @Enumerated(EnumType.STRING)
    private ProjectStatus status = ProjectStatus.PLANEAMENTO;

    
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

    public String getSprintGoals() {
        return sprintGoals;
    }

    public void setSprintGoals(String sprintGoals) {
        this.sprintGoals = sprintGoals;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public List<Team> getTeams() {
        return teams;
    }

    public void setTeams(List<Team> teams) {
        this.teams = teams;
    }

    public List<Sprint> getSprints() {
        return sprints;
    }

    public void setSprints(List<Sprint> sprints) {
        this.sprints = sprints;
    }

    public ProjectStatus getStatus() {
        return status;
    }

    public void setStatus(ProjectStatus status) {
        this.status = status;
    }

    /**
     * Calcula o progresso total do projeto como percentagem de sprints
     * concluídos.
     *
     * @return progresso do projeto (0-100)
     */
    public int getProgress() {
        if (sprints == null || sprints.isEmpty()) {
            return 0;
        }
        long doneSprints = sprints.stream().filter(s -> s.getStatus() == SprintStatus.CONCLUIDO).count();
        return (int) ((doneSprints * 100.0) / sprints.size());
    }
}
