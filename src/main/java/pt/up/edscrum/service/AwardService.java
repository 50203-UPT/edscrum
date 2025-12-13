package pt.up.edscrum.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import pt.up.edscrum.enums.NotificationType; // Importação adicionada
import pt.up.edscrum.model.Award;
import pt.up.edscrum.model.Project;
import pt.up.edscrum.model.Score;
import pt.up.edscrum.model.StudentAward;
import pt.up.edscrum.model.Team;
import pt.up.edscrum.model.TeamAward;
import pt.up.edscrum.model.User;
import pt.up.edscrum.repository.AwardRepository;
import pt.up.edscrum.repository.ProjectRepository;
import pt.up.edscrum.repository.ScoreRepository;
import pt.up.edscrum.repository.SprintRepository;
import pt.up.edscrum.repository.StudentAwardRepository;
import pt.up.edscrum.repository.TeamAwardRepository;
import pt.up.edscrum.repository.TeamRepository;
import pt.up.edscrum.repository.UserRepository;

@Service
public class AwardService {

    private final AwardRepository awardRepo;
    private final StudentAwardRepository studentAwardRepo;
    private final TeamAwardRepository teamAwardRepo;
    private final UserRepository userRepo;
    private final ScoreRepository scoreRepo;
    private final TeamRepository teamRepo;
    private final ProjectRepository projectRepo;
    private final SprintRepository sprintRepo;
    
    // Serviço de Notificações Injetado
    private final NotificationService notificationService;

    public AwardService(AwardRepository awardRepo, StudentAwardRepository studentAwardRepo,
            TeamAwardRepository teamAwardRepo, UserRepository userRepo,
            ScoreRepository scoreRepo, TeamRepository teamRepo, ProjectRepository projectRepo,
            SprintRepository sprintRepo, NotificationService notificationService) {
        this.awardRepo = awardRepo;
        this.studentAwardRepo = studentAwardRepo;
        this.teamAwardRepo = teamAwardRepo;
        this.userRepo = userRepo;
        this.scoreRepo = scoreRepo;
        this.teamRepo = teamRepo;
        this.projectRepo = projectRepo;
        this.sprintRepo = sprintRepo;
        this.notificationService = notificationService;
    }

    // --- HELPERS PARA PRÉMIOS AUTOMÁTICOS ---
    private Award ensureAutomaticAward(String name, String description, int points, String targetType) {
        return awardRepo.findByName(name).orElseGet(() -> {
            Award a = new Award();
            a.setName(name);
            a.setDescription(description);
            a.setPoints(points);
            a.setType("AUTOMATIC");
            a.setTargetType(targetType);
            return awardRepo.save(a);
        });
    }

    public void assignAutomaticAwardToStudentByName(String name, String description, int points, Long studentId, Long projectId) {
        Award award = ensureAutomaticAward(name, description, points, "INDIVIDUAL");
        // Verifica se já existe
        boolean exists = (projectId != null) ? studentAwardRepo.existsByStudentIdAndAwardIdAndProjectId(studentId, award.getId(), projectId)
                : studentAwardRepo.findAllByStudentId(studentId).stream().anyMatch(sa -> sa.getAward().getId().equals(award.getId()));

        if (exists) {
            return;
        }

        User student = userRepo.findById(studentId).orElseThrow();
        StudentAward sa = new StudentAward();
        sa.setAward(award);
        sa.setStudent(student);
        if (projectId != null) {
            sa.setProject(projectRepo.findById(projectId).orElse(null));
        }
        sa.setPointsEarned(award.getPoints());
        studentAwardRepo.save(sa);

        // Atualiza score
        updateUserScore(sa.getStudent());

        // --- NOTIFICAÇÃO ---
        notificationService.createNotification(
            student, 
            NotificationType.AWARD, 
            "Novo Prémio Automático!", 
            "Parabéns! Ganhaste o prémio '" + award.getName() + "' (+" + award.getPoints() + " XP)."
        );
    }

    public void assignAutomaticAwardToTeamByName(String name, String description, int points, Long teamId, Long projectId) {
        Award award = ensureAutomaticAward(name, description, points, "TEAM");

        boolean exists = (projectId != null) ? teamAwardRepo.existsByTeamIdAndAwardIdAndProjectId(teamId, award.getId(), projectId)
                : teamAwardRepo.findByTeamId(teamId).stream().anyMatch(ta -> ta.getAward().getId().equals(award.getId()));

        if (exists) {
            return;
        }

        Team team = teamRepo.findById(teamId).orElseThrow();
        TeamAward ta = new TeamAward();
        ta.setAward(award);
        ta.setTeam(team);
        if (projectId != null) {
            ta.setProject(projectRepo.findById(projectId).orElse(null));
        }
        ta.setPointsEarned(award.getPoints());
        teamAwardRepo.save(ta);

        // Atualizar score da equipa e dos membros
        updateTeamScore(ta.getTeam());
        
        // Notificar e atualizar score dos membros
        notifyAndUpdateTeamMembers(team, award, true);
    }

    // Helper para notificar membros da equipa
    private void notifyAndUpdateTeamMembers(Team team, Award award, boolean isAutomatic) {
        String title = isAutomatic ? "Prémio de Equipa Automático!" : "Novo Prémio de Equipa!";
        String msg = "A tua equipa '" + team.getName() + "' ganhou o prémio '" + award.getName() + "' (+" + award.getPoints() + " XP).";

        if (team.getScrumMaster() != null) {
            updateUserScore(team.getScrumMaster());
            notificationService.createNotification(team.getScrumMaster(), NotificationType.AWARD, title, msg);
        }
        if (team.getProductOwner() != null) {
            updateUserScore(team.getProductOwner());
            notificationService.createNotification(team.getProductOwner(), NotificationType.AWARD, title, msg);
        }
        for (User dev : team.getDevelopers()) {
            updateUserScore(dev);
            notificationService.createNotification(dev, NotificationType.AWARD, title, msg);
        }
    }

    // Handle logic when a student creates a sprint: first sprint, 5/10 counts, marathon (3 in 7 days), record activity
    public void handleSprintCreated(Long studentId, Long projectId) {
        if (studentId == null) {
            return;
        }

        // Count sprints created by user
        List<pt.up.edscrum.model.Sprint> created = sprintRepo.findByCreatedById(studentId);
        int totalCreated = created != null ? created.size() : 0;

        // First sprint
        if (totalCreated == 1) {
            assignAutomaticAwardToStudentByName("Primeiro Salto", "Criaste o teu primeiro sprint! Continua assim.", 20, studentId, projectId);
        }

        // 5 and 10 sprints
        if (totalCreated == 5) {
            assignAutomaticAwardToStudentByName("Sprint Artisan (5)", "Criaste 5 sprints.", 40, studentId, null);
        }
        if (totalCreated == 10) {
            assignAutomaticAwardToStudentByName("Sprint Veteran (10)", "Criaste 10 sprints.", 90, studentId, null);
        }
    }

    // --- MÉTODOS CRUD ---
    public List<Award> getAllAwards() {
        return awardRepo.findAll();
    }

    public Award getAwardById(Long id) {
        return awardRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Award not found"));
    }

    public Award createAward(Award award) {
        return awardRepo.save(award);
    }

    public Award updateAward(Long id, Award awardDetails) {
        Award award = getAwardById(id);
        award.setName(awardDetails.getName());
        award.setDescription(awardDetails.getDescription());
        award.setPoints(awardDetails.getPoints());
        award.setType(awardDetails.getType());
        return awardRepo.save(award);
    }

    public void deleteAward(Long id) {
        awardRepo.deleteById(id);
    }

    public List<Award> getAvailableAwardsForTeam(Long teamId, Long projectId) {
        List<Award> allAwards = awardRepo.findAll();
        List<Long> assignedAwardIds = teamAwardRepo.findByTeamIdAndProjectId(teamId, projectId)
                .stream()
                .map(ta -> ta.getAward().getId())
                .toList();

        return allAwards.stream()
                .filter(a -> "TEAM".equals(a.getTargetType()))
                .filter(a -> !assignedAwardIds.contains(a.getId()))
                .toList();
    }

    public List<Award> getAvailableAwardsForStudent(Long studentId, Long projectId) {
        List<Award> allAwards = awardRepo.findAll();
        List<Long> assignedAwardIds = studentAwardRepo.findByStudentIdAndProjectId(studentId, projectId)
                .stream()
                .map(sa -> sa.getAward().getId())
                .toList();

        return allAwards.stream()
                .filter(a -> "INDIVIDUAL".equals(a.getTargetType()))
                .filter(a -> !assignedAwardIds.contains(a.getId()))
                .toList();
    }

    // --- ATRIBUIÇÃO E CÁLCULOS ---
    public void assignAwardToStudent(Long awardId, Long studentId, Long projectId) {
        Award award = getAwardById(awardId);
        User student = userRepo.findById(studentId).orElseThrow();
        Project project = projectRepo.findById(projectId).orElseThrow();

        if (studentAwardRepo.existsByStudentIdAndAwardIdAndProjectId(studentId, awardId, projectId)) {
            throw new RuntimeException("Este prémio já foi atribuído a este aluno neste projeto.");
        }

        StudentAward sa = new StudentAward();
        sa.setAward(award);
        sa.setStudent(student);
        sa.setProject(project);
        sa.setPointsEarned(award.getPoints());
        studentAwardRepo.save(sa);

        updateUserScore(student);

        // --- NOTIFICAÇÃO ---
        notificationService.createNotification(
            student, 
            NotificationType.AWARD, 
            "Novo Prémio Conquistado!", 
            "Recebeste o prémio '" + award.getName() + "' (+" + award.getPoints() + " XP) no projeto " + project.getName()
        );
    }

    public void assignAwardToStudent(Long awardId, Long studentId) {
        Award award = getAwardById(awardId);
        User student = userRepo.findById(studentId).orElseThrow();

        StudentAward sa = new StudentAward();
        sa.setAward(award);
        sa.setStudent(student);
        sa.setPointsEarned(award.getPoints());
        studentAwardRepo.save(sa);

        updateUserScore(student);

        // --- NOTIFICAÇÃO ---
        notificationService.createNotification(
            student, 
            NotificationType.AWARD, 
            "Novo Prémio Conquistado!", 
            "Recebeste o prémio '" + award.getName() + "' (+" + award.getPoints() + " XP)."
        );
    }

    public void assignAwardToTeam(Long awardId, Long teamId, Long projectId) {
        Award award = getAwardById(awardId);
        Team team = teamRepo.findById(teamId).orElseThrow();
        Project project = projectRepo.findById(projectId).orElseThrow();

        if (teamAwardRepo.existsByTeamIdAndAwardIdAndProjectId(teamId, awardId, projectId)) {
            throw new RuntimeException("Este prémio já foi atribuído a esta equipa neste projeto.");
        }

        TeamAward ta = new TeamAward();
        ta.setAward(award);
        ta.setTeam(team);
        ta.setProject(project);
        ta.setPointsEarned(award.getPoints());
        teamAwardRepo.save(ta);

        updateTeamScore(team);
        
        // Notificar e atualizar score dos membros
        notifyAndUpdateTeamMembers(team, award, false);
    }

    public void assignAwardToTeam(Long awardId, Long teamId) {
        Award award = getAwardById(awardId);
        Team team = teamRepo.findById(teamId).orElseThrow();

        TeamAward ta = new TeamAward();
        ta.setAward(award);
        ta.setTeam(team);
        ta.setPointsEarned(award.getPoints());
        teamAwardRepo.save(ta);

        updateTeamScore(team);
        
        // Notificar e atualizar score dos membros
        notifyAndUpdateTeamMembers(team, award, false);
    }

    public int calculateTotalPoints(Long studentId) {
        int individualPoints = studentAwardRepo.findAllByStudentId(studentId).stream()
                .mapToInt(StudentAward::getPointsEarned).sum();

        int teamPoints = 0;
        List<Team> teams = teamRepo.findTeamByUserId(studentId); 

        if (teams != null && !teams.isEmpty()) {
            for (Team team : teams) {
                teamPoints += teamAwardRepo.findByTeamId(team.getId()).stream()
                        .mapToInt(TeamAward::getPointsEarned).sum();
            }
        }

        return individualPoints + teamPoints;
    }

    // --- MÉTODOS AUXILIARES PARA ATUALIZAR TABELA SCORE ---
    private void updateUserScore(User user) {
        int total = calculateTotalPoints(user.getId());

        Score score = scoreRepo.findByUser(user);
        if (score == null) {
            score = new Score();
            score.setUser(user);

            List<Team> teams = teamRepo.findTeamByUserId(user.getId());
            if (teams != null && !teams.isEmpty()) {
                score.setTeam(teams.get(0));
            }
        }
        score.setTotalPoints(total);
        scoreRepo.save(score);
        
        // --- AFTER SAVE: check ranking-based automatic awards ---
        try {
            List<pt.up.edscrum.model.Score> allScores = scoreRepo.findAllByUserIsNotNullOrderByTotalPointsDesc();
            int rank = 0;
            for (int i = 0; i < allScores.size(); i++) {
                if (allScores.get(i).getUser() != null && allScores.get(i).getUser().getId().equals(user.getId())) {
                    rank = i + 1;
                    break;
                }
            }

            if (rank > 0 && rank <= 5) {
                assignAutomaticAwardToStudentByName("Estrela da Turma (Top 5)", "Entraste no Top 5 do ranking global.", 50, user.getId(), null);
                // Notificação de Ranking (Opcional, pois o assignAutomaticAward já notifica)
            }
            if (rank > 0 && rank <= 3) {
                assignAutomaticAwardToStudentByName("Mestre do Podium (Top 3)", "Chegaste ao Top 3 do ranking global.", 120, user.getId(), null);
            }

        } catch (Exception e) {
            // Non-fatal
        }
    }

    private void updateTeamScore(Team team) {
        int total = teamAwardRepo.findByTeamId(team.getId()).stream()
                .mapToInt(TeamAward::getPointsEarned).sum();

        Score score = scoreRepo.findByTeamAndUserIsNull(team);
        if (score == null) {
            score = new Score();
            score.setTeam(team);
            score.setUser(null); 
        }
        score.setTotalPoints(total);
        scoreRepo.save(score);
    }
}