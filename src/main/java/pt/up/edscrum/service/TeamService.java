package pt.up.edscrum.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import pt.up.edscrum.model.Team;
import pt.up.edscrum.model.User;
import pt.up.edscrum.repository.TeamRepository;

@Service
public class TeamService {

    private final TeamRepository teamRepository;
    private final pt.up.edscrum.repository.EnrollmentRepository enrollmentRepository;

    public TeamService(TeamRepository teamRepository,
                      pt.up.edscrum.repository.EnrollmentRepository enrollmentRepository) {
        this.teamRepository = teamRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    public List<Team> getAllTeams() {
        return teamRepository.findAll();
    }

    public Team getTeamById(Long id) {
        return teamRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Team not found"));
    }

    // --- NOVO: Validar duplicações antes da UI ---
    public Set<Long> getTakenStudentIdsByCourse(Long courseId) {
        List<Team> teams = teamRepository.findByCourseId(courseId);
        Set<Long> takenIds = new HashSet<>();

        for (Team t : teams) {
            // Verifica Scrum Master (se for aluno)
            if (t.getScrumMaster() != null && "STUDENT".equals(t.getScrumMaster().getRole())) {
                takenIds.add(t.getScrumMaster().getId());
            }
            // Verifica Product Owner (se for aluno) - Professores são ignorados aqui!
            if (t.getProductOwner() != null && "STUDENT".equals(t.getProductOwner().getRole())) {
                takenIds.add(t.getProductOwner().getId());
            }
            // Verifica Developers
            if (t.getDevelopers() != null) {
                for (User dev : t.getDevelopers()) {
                    if ("STUDENT".equals(dev.getRole())) {
                        takenIds.add(dev.getId());
                    }
                }
            }
        }
        return takenIds;
    }

    // Método auxiliar para validar duplicações no SAVE (Backend safety)
    private void validateStudentAvailability(User student, Long courseId) {
        if (student != null && "STUDENT".equals(student.getRole())) {
            // NOVA VALIDAÇÃO: Verificar se o aluno está inscrito no curso
            boolean isEnrolled = enrollmentRepository.existsByStudentIdAndCourseId(student.getId(), courseId);
            if (!isEnrolled) {
                throw new RuntimeException("O aluno " + student.getName() + " (" + student.getId() + "-UPT) não está inscrito neste curso!");
            }
            
            // Validação existente: Verificar duplicações
            long count = teamRepository.countStudentTeamsInCourse(student.getId(), courseId);
            if (count > 0) {
                throw new RuntimeException("O aluno " + student.getName() + " (" + student.getId() + "-UPT) já pertence a uma equipa neste curso!");
            }
        }
    }

    public Team createTeam(Team team) {
        Long courseId = team.getCourse().getId();

        validateStudentAvailability(team.getScrumMaster(), courseId);
        validateStudentAvailability(team.getProductOwner(), courseId);

        if (team.getDevelopers() != null) {
            for (User dev : team.getDevelopers()) {
                validateStudentAvailability(dev, courseId);
            }
        }

        return teamRepository.save(team);
    }

    public Team updateTeam(Long id, Team teamDetails) {
        Team team = getTeamById(id);
        team.setName(teamDetails.getName());
        team.setProject(teamDetails.getProject());
        team.setScrumMaster(teamDetails.getScrumMaster());
        team.setProductOwner(teamDetails.getProductOwner());
        team.setDevelopers(teamDetails.getDevelopers());
        return teamRepository.save(team);
    }

    public void deleteTeam(Long id) {
        teamRepository.deleteById(id);
    }

    public List<Team> getAvailableTeamsByCourse(Long courseId) {
        return teamRepository.findAvailableTeamsByCourse(courseId);
    }

    // Retorna todos os membros de uma equipa (SM, PO e Developers)
    public List<User> getTeamMembers(Long teamId) {
        Team team = getTeamById(teamId);
        java.util.List<User> members = new java.util.ArrayList<>();
        
        if (team.getScrumMaster() != null) {
            members.add(team.getScrumMaster());
        }
        if (team.getProductOwner() != null) {
            members.add(team.getProductOwner());
        }
        if (team.getDevelopers() != null) {
            members.addAll(team.getDevelopers());
        }
        
        return members;
    }

    // Retorna um Mapa: ID do Curso -> Conjunto de IDs de Alunos Ocupados
    public java.util.Map<Long, java.util.Set<Long>> getTakenStudentsMap() {
        java.util.Map<Long, java.util.Set<Long>> map = new java.util.HashMap<>();
        List<Team> allTeams = teamRepository.findAll();

        for (Team t : allTeams) {
            if (t.getCourse() == null) continue;
            Long cId = t.getCourse().getId();
            map.putIfAbsent(cId, new java.util.HashSet<>());

            // SM (se for aluno)
            if (t.getScrumMaster() != null && "STUDENT".equals(t.getScrumMaster().getRole())) {
                map.get(cId).add(t.getScrumMaster().getId());
            }
            // PO (se for aluno)
            if (t.getProductOwner() != null && "STUDENT".equals(t.getProductOwner().getRole())) {
                map.get(cId).add(t.getProductOwner().getId());
            }
            // Developers
            if (t.getDevelopers() != null) {
                for (User dev : t.getDevelopers()) {
                    if ("STUDENT".equals(dev.getRole())) {
                        map.get(cId).add(dev.getId());
                    }
                }
            }
        }
        return map;
    }
}