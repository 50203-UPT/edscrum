package pt.up.edscrum.service;

import java.util.List;

import org.springframework.stereotype.Service;

import pt.up.edscrum.model.Team;
import pt.up.edscrum.model.Project;
import pt.up.edscrum.model.User;
import pt.up.edscrum.exception.TeamValidationException;
import pt.up.edscrum.repository.TeamRepository;
import pt.up.edscrum.repository.ProjectRepository;

@Service
public class TeamService {

    private final TeamRepository teamRepository;
    private final ProjectRepository projectRepository;

    public TeamService(TeamRepository teamRepository, ProjectRepository projectRepository) {
        this.teamRepository = teamRepository;
        this.projectRepository = projectRepository;
    }

    // List all teams
    public List<Team> getAllTeams() {
        return teamRepository.findAll();
    }

    // Search team by ID
    public Team getTeamById(Long id) {
        return teamRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Team not found"));
    }

    // Create new team
    public Team createTeam(Team team) {
        // If team has projects but no explicit course, infer course from first project
        if ((team.getCourse() == null || team.getCourse().getId() == null) && team.getProjects() != null && !team.getProjects().isEmpty()) {
            Project first = team.getProjects().get(0);
            // If project does not have course preloaded (e.g., JSON with only id), fetch from repository
            if (first.getCourse() == null || first.getCourse().getId() == null) {
                if (first.getId() != null) {
                    Project loaded = projectRepository.findById(first.getId()).orElse(null);
                    if (loaded != null && loaded.getCourse() != null) {
                        team.setCourse(loaded.getCourse());
                    }
                }
            } else {
                team.setCourse(first.getCourse());
            }
        }
        // Valida que cada membro não pertence a outra equipa no mesmo curso
        if (team.getCourse() != null) {
            Long courseId = team.getCourse().getId();
            // Scrum master
            if (team.getScrumMaster() != null) {
                Team existing = teamRepository.findTeamByUserIdAndCourseId(team.getScrumMaster().getId(), courseId);
                if (existing != null) {
                    throw new TeamValidationException("Scrum master já pertence a outra equipa neste curso");
                }
            }
            // Product owner
            if (team.getProductOwner() != null) {
                Team existing = teamRepository.findTeamByUserIdAndCourseId(team.getProductOwner().getId(), courseId);
                if (existing != null) {
                    throw new TeamValidationException("Product owner já pertence a outra equipa neste curso");
                }
            }
            // Developers
            if (team.getDevelopers() != null) {
                for (User dev : team.getDevelopers()) {
                    Team existing = teamRepository.findTeamByUserIdAndCourseId(dev.getId(), courseId);
                        if (existing != null) {
                        throw new TeamValidationException("Um estudante já pertence a outra equipa neste curso");
                    }
                }
            }
        }

        // Valida que todos os projetos da equipa pertencem ao mesmo curso
        if (team.getProjects() != null && team.getCourse() != null) {
            for (Project p : team.getProjects()) {
                    if (p.getCourse() == null || !p.getCourse().getId().equals(team.getCourse().getId())) {
                    throw new TeamValidationException("Todas as equipas devem pertencer a projetos do mesmo curso");
                }
            }
        }

        return teamRepository.save(team);
    }

    // Update team
    public Team updateTeam(Long id, Team teamDetails) {
        Team team = getTeamById(id);
        team.setName(teamDetails.getName());
        team.setProjects(teamDetails.getProjects());
        team.setScrumMaster(teamDetails.getScrumMaster());
        team.setProductOwner(teamDetails.getProductOwner());
        team.setDevelopers(teamDetails.getDevelopers());
        // Valida unicidade por curso para membros
        if (team.getCourse() != null) {
            Long courseId = team.getCourse().getId();
            if (team.getScrumMaster() != null) {
                Team existing = teamRepository.findTeamByUserIdAndCourseId(team.getScrumMaster().getId(), courseId);
                if (existing != null && !existing.getId().equals(team.getId())) {
                    throw new TeamValidationException("Scrum master já pertence a outra equipa neste curso");
                }
            }
            if (team.getProductOwner() != null) {
                Team existing = teamRepository.findTeamByUserIdAndCourseId(team.getProductOwner().getId(), courseId);
                if (existing != null && !existing.getId().equals(team.getId())) {
                    throw new TeamValidationException("Product owner já pertence a outra equipa neste curso");
                }
            }
            if (team.getDevelopers() != null) {
                for (User dev : team.getDevelopers()) {
                    Team existing = teamRepository.findTeamByUserIdAndCourseId(dev.getId(), courseId);
                    if (existing != null && !existing.getId().equals(team.getId())) {
                        throw new TeamValidationException("Um estudante já pertence a outra equipa neste curso");
                    }
                }
            }
        }

        // Valida que projetos da equipa pertençam ao mesmo curso
        if (team.getProjects() != null && team.getCourse() != null) {
            for (Project p : team.getProjects()) {
                if (p.getCourse() == null || !p.getCourse().getId().equals(team.getCourse().getId())) {
                    throw new TeamValidationException("Todas as equipas devem pertencer a projetos do mesmo curso");
                }
            }
        }
        return teamRepository.save(team);
    }

    // Apagar equipa
    public void deleteTeam(Long id) {
        teamRepository.deleteById(id);
    }

    // Obter equipas disponíveis num curso
    public List<Team> getAvailableTeamsByCourse(Long courseId) {
        return teamRepository.findAvailableTeamsByCourse(courseId);
    }
}
