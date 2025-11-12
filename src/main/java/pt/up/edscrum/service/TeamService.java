package pt.up.edscrum.service;

import java.util.List;

import org.springframework.stereotype.Service;

import pt.up.edscrum.model.Team;
import pt.up.edscrum.repository.TeamRepository;

@Service
public class TeamService {

    private final TeamRepository teamRepository;

    public TeamService(TeamRepository teamRepository) {
        this.teamRepository = teamRepository;
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
        return teamRepository.save(team);
    }

    // Update team
    public Team updateTeam(Long id, Team teamDetails) {
        Team team = getTeamById(id);
        team.setName(teamDetails.getName());
        team.setProject(teamDetails.getProject());
        team.setScrumMaster(teamDetails.getScrumMaster());
        team.setProductOwner(teamDetails.getProductOwner());
        team.setDevelopers(teamDetails.getDevelopers());
        return teamRepository.save(team);
    }

    // Apagar equipa
    public void deleteTeam(Long id) {
        teamRepository.deleteById(id);
    }
}
