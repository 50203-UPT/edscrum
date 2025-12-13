package pt.up.edscrum.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import pt.up.edscrum.enums.NotificationType; // Importação
import pt.up.edscrum.model.Score;
import pt.up.edscrum.model.Team;
import pt.up.edscrum.model.TeamAward;
import pt.up.edscrum.model.User;
import pt.up.edscrum.repository.ScoreRepository;
import pt.up.edscrum.repository.TeamAwardRepository;
import pt.up.edscrum.repository.TeamRepository;

@Service
/**
 * Serviço que gere operações relacionadas com `Team` (equipas): CRUD,
 * validações de duplicações, obtenção de membros e listas de equipas
 * disponíveis por curso. Também integra atribuição de prémios automáticos via
 * `AwardService` durante eventos de criação de equipa.
 */
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamAwardRepository teamAwardRepository;
    private final ScoreRepository scoreRepository;
    private final pt.up.edscrum.repository.EnrollmentRepository enrollmentRepository;
    private final pt.up.edscrum.service.AwardService awardService;

    // Serviço de Notificações
    private final NotificationService notificationService;

    public TeamService(TeamRepository teamRepository,
            TeamAwardRepository teamAwardRepository,
            ScoreRepository scoreRepository,
            pt.up.edscrum.repository.EnrollmentRepository enrollmentRepository,
            pt.up.edscrum.service.AwardService awardService,
            NotificationService notificationService) {
        this.teamRepository = teamRepository;
        this.teamAwardRepository = teamAwardRepository;
        this.scoreRepository = scoreRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.awardService = awardService;
        this.notificationService = notificationService;
    }

    /**
     * Retorna todas as equipas existentes.
     *
     * @return lista de `Team`
     */
    public List<Team> getAllTeams() {
        return teamRepository.findAll();
    }

    /**
     * Obtém uma equipa pelo seu id.
     *
     * @param id id da equipa
     * @return `Team` correspondente
     */
    public Team getTeamById(Long id) {
        return teamRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Team not found"));
    }

    /**
     * Retorna o conjunto de IDs de estudantes que já estão ocupados por uma
     * equipa dentro de um dado curso (usado para evitar duplicações na UI).
     *
     * @param courseId id do curso
     * @return conjunto de ids de estudantes ocupados
     */
    public Set<Long> getTakenStudentIdsByCourse(Long courseId) {
        List<Team> teams = teamRepository.findByCourseId(courseId);
        Set<Long> takenIds = new HashSet<>();

        for (Team t : teams) {
            if (t.getScrumMaster() != null && "STUDENT".equals(t.getScrumMaster().getRole())) {
                takenIds.add(t.getScrumMaster().getId());
            }
            if (t.getProductOwner() != null && "STUDENT".equals(t.getProductOwner().getRole())) {
                takenIds.add(t.getProductOwner().getId());
            }
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

    /**
     * Valida se um estudante está inscrito no curso e não pertence já a outra
     * equipa do mesmo curso. Lança `RuntimeException` em caso de violação.
     *
     * @param student objeto `User` do estudante (pode ser null)
     * @param courseId id do curso
     */
    private void validateStudentAvailability(User student, Long courseId) {
        if (student != null && "STUDENT".equals(student.getRole())) {
            // Verificar se o aluno está inscrito no curso
            boolean isEnrolled = enrollmentRepository.existsByStudentIdAndCourseId(student.getId(), courseId);
            if (!isEnrolled) {
                throw new RuntimeException("O aluno " + student.getName() + " (" + student.getId() + "-UPT) não está inscrito neste curso!");
            }

            // Verificar duplicações
            long count = teamRepository.countStudentTeamsInCourse(student.getId(), courseId);
            if (count > 0) {
                throw new RuntimeException("O aluno " + student.getName() + " (" + student.getId() + "-UPT) já pertence a uma equipa neste curso!");
            }
        }
    }

    /**
     * Cria uma nova equipa após validar a disponibilidade dos estudantes.
     * Atribui um prémio automático por formação de equipa e, opcionalmente,
     * outros prémios de participação.
     *
     * @param team o objeto `Team` a criar
     * @return a equipa criada e persistida
     */
    public Team createTeam(Team team) {
        Long courseId = team.getCourse().getId();

        validateStudentAvailability(team.getScrumMaster(), courseId);
        validateStudentAvailability(team.getProductOwner(), courseId);

        if (team.getDevelopers() != null) {
            for (User dev : team.getDevelopers()) {
                validateStudentAvailability(dev, courseId);
            }
        }

        // Calculate current member count
        int memberCount = team.getCurrentMemberCount();

        // Auto-close team if it's full
        if (memberCount >= team.getMaxMembers()) {
            team.setClosed(true);
        }

        Team saved = teamRepository.save(team);

        // --- NOTIFICAÇÕES PARA OS MEMBROS ---
        notifyTeamMembers(saved, "Bem-vindo à equipa!", "Foste adicionado à equipa '" + saved.getName() + "' no curso " + saved.getCourse().getName() + ".");

        // Atribuir prémio de equipa automático
        try {
            awardService.assignAutomaticAwardToTeamByName("Equipa Formada", "A tua equipa foi formada.", 30, saved.getId(), null);
        } catch (Exception e) {
        }

        // Verificar participação em projetos
        try {
            List<pt.up.edscrum.model.User> members = getTeamMembers(saved.getId());
            for (pt.up.edscrum.model.User u : members) {
                List<Team> userTeams = teamRepository.findTeamByUserId(u.getId());
                long projectCount = userTeams.stream().filter(t -> t.getProject() != null).map(t -> t.getProject().getId()).distinct().count();
                if (projectCount >= 3) {
                    awardService.assignAutomaticAwardToStudentByName("Colaborador Estelar", "Participaste activamente em 3 projetos diferentes.", 70, u.getId(), null);
                }
            }
        } catch (Exception e) {
        }

        return saved;
    }

    // Helper para notificar todos os membros
    private void notifyTeamMembers(Team team, String title, String message) {
        if (team.getScrumMaster() != null) {
            notificationService.createNotification(team.getScrumMaster(), NotificationType.TEAM, title, message);
        }
        if (team.getProductOwner() != null) {
            notificationService.createNotification(team.getProductOwner(), NotificationType.TEAM, title, message);
        }
        if (team.getDevelopers() != null) {
            for (User dev : team.getDevelopers()) {
                notificationService.createNotification(dev, NotificationType.TEAM, title, message);
            }
        }
    }

    /**
     * Atualiza os dados de uma equipa existente.
     *
     * @param id id da equipa a atualizar
     * @param teamDetails objecto com os novos valores
     * @return a equipa atualizada
     */
    public Team updateTeam(Long id, Team teamDetails) {
        Team team = getTeamById(id);
        team.setName(teamDetails.getName());
        team.setProject(teamDetails.getProject());
        team.setScrumMaster(teamDetails.getScrumMaster());
        team.setProductOwner(teamDetails.getProductOwner());
        team.setDevelopers(teamDetails.getDevelopers());
        return teamRepository.save(team);
    }

    /**
     * Remove uma equipa por id.
     *
     * @param id id da equipa a eliminar
     */
    public void deleteTeam(Long id) {
        // Opcional: Notificar membros que a equipa foi eliminada antes de apagar
        try {
            Team team = getTeamById(id);
            notifyTeamMembers(team, "Equipa Eliminada", "A equipa '" + team.getName() + "' foi removida.");
        } catch(Exception e) {}
        
        // Eliminar Scores associados antes de eliminar a equipa (FK constraint)
        List<Score> scores = scoreRepository.findByTeamId(id);
        if (scores != null && !scores.isEmpty()) {
            scoreRepository.deleteAll(scores);
        }
        
        // Eliminar TeamAwards associados antes de eliminar a equipa (FK constraint)
        List<TeamAward> teamAwards = teamAwardRepository.findByTeamId(id);
        if (teamAwards != null && !teamAwards.isEmpty()) {
            teamAwardRepository.deleteAll(teamAwards);
        }
        
        teamRepository.deleteById(id);
    }

    /**
     * Obtém as equipas disponíveis para um dado curso.
     *
     * @param courseId id do curso
     * @return lista de equipas disponíveis
     */
    public List<Team> getAvailableTeamsByCourse(Long courseId) {
        return teamRepository.findAvailableTeamsByCourse(courseId);
    }

    /**
     * Procura todas as equipas a que um utilizador pertence.
     *
     * @param userId id do utilizador
     * @return lista de equipas do utilizador
     */
    public List<Team> findTeamsByUserId(Long userId) {
        return teamRepository.findTeamByUserId(userId);
    }

    /**
     * Retorna a lista dos membros de uma equipa (Scrum Master, Product Owner e
     * Developers).
     *
     * @param teamId id da equipa
     * @return lista de `User` membros da equipa
     */
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

    /**
     * Constrói um mapa de curso -> conjunto de ids de estudantes ocupados por
     * equipas nesse curso.
     *
     * @return mapa com chave cursoId e valor conjunto de studentIds
     */
    public java.util.Map<Long, java.util.Set<Long>> getTakenStudentsMap() {
        java.util.Map<Long, java.util.Set<Long>> map = new java.util.HashMap<>();
        List<Team> allTeams = teamRepository.findAll();

        for (Team t : allTeams) {
            if (t.getCourse() == null) {
                continue;
            }
            Long cId = t.getCourse().getId();
            map.putIfAbsent(cId, new java.util.HashSet<>());

            if (t.getScrumMaster() != null && "STUDENT".equals(t.getScrumMaster().getRole())) {
                map.get(cId).add(t.getScrumMaster().getId());
            }
            if (t.getProductOwner() != null && "STUDENT".equals(t.getProductOwner().getRole())) {
                map.get(cId).add(t.getProductOwner().getId());
            }
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

    /**
     * Gets available teams for a course (not closed and has slots available)
     */
    public List<Team> getAvailableTeamsForStudentByCourse(Long courseId) {
        List<Team> teams = teamRepository.findByCourseId(courseId);
        return teams.stream()
                .filter(t -> !t.isClosed() && t.canAcceptMembers())
                .toList();
    }

    /**
     * Closes a team (prevents new members from joining)
     */
    public Team closeTeam(Long teamId) {
        Team team = getTeamById(teamId);
        team.setClosed(true);
        return teamRepository.save(team);
    }

    /**
     * Gets the team a student belongs to in a specific course
     */
    public Team getStudentTeamInCourse(Long studentId, Long courseId) {
        List<Team> teams = teamRepository.findByCourseId(courseId);
        for (Team team : teams) {
            if (isStudentInTeam(studentId, team)) {
                return team;
            }
        }
        return null;
    }

    /**
     * Checks if a student is in a specific team
     */
    private boolean isStudentInTeam(Long studentId, Team team) {
        if (team.getScrumMaster() != null && team.getScrumMaster().getId().equals(studentId)) {
            return true;
        }
        if (team.getProductOwner() != null && team.getProductOwner().getId().equals(studentId)) {
            return true;
        }
        if (team.getDevelopers() != null) {
            for (User dev : team.getDevelopers()) {
                if (dev.getId().equals(studentId)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Add a student to a team as a developer
     */
    public Team addStudentToTeam(Long teamId, Long studentId, User student) {
        return addStudentToTeamWithRole(teamId, studentId, student, "DEVELOPER");
    }
    
    /**
     * Add a student to a team with a specific role
     */
    public Team addStudentToTeamWithRole(Long teamId, Long studentId, User student, String role) {
        Team team = getTeamById(teamId);

        // Validate student is not already in a team in this course
        validateStudentAvailability(student, team.getCourse().getId());

        // Check if team can accept members
        if (!team.canAcceptMembers()) {
            throw new RuntimeException("A equipa está fechada ou completa");
        }
        
        switch (role.toUpperCase()) {
            case "PRODUCT_OWNER":
                if (team.getProductOwner() != null) {
                    throw new RuntimeException("A equipa já tem um Product Owner");
                }
                team.setProductOwner(student);
                break;
            case "SCRUM_MASTER":
                if (team.getScrumMaster() != null) {
                    throw new RuntimeException("A equipa já tem um Scrum Master");
                }
                team.setScrumMaster(student);
                break;
            case "DEVELOPER":
            default:
                if (team.getDevelopers() == null) {
                    team.setDevelopers(new java.util.ArrayList<>());
                }
                team.getDevelopers().add(student);
                break;
        }
        
        // Auto-close if now full
        if (team.isFull()) {
            team.setClosed(true);
        }

        return teamRepository.save(team);
    }
}
