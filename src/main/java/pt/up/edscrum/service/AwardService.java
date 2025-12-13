package pt.up.edscrum.service;

import java.util.List;

import org.springframework.stereotype.Service;

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

    public AwardService(AwardRepository awardRepo, StudentAwardRepository studentAwardRepo,
            TeamAwardRepository teamAwardRepo, UserRepository userRepo,
            ScoreRepository scoreRepo, TeamRepository teamRepo, ProjectRepository projectRepo) {
        this.awardRepo = awardRepo;
        this.studentAwardRepo = studentAwardRepo;
        this.teamAwardRepo = teamAwardRepo;
        this.userRepo = userRepo;
        this.scoreRepo = scoreRepo;
        this.teamRepo = teamRepo;
        this.projectRepo = projectRepo;
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
    
    // Obter prémios disponíveis para uma equipa num projeto (exclui já atribuídos e filtra por tipo TEAM)
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
    
    // Obter prémios disponíveis para um estudante num projeto (exclui já atribuídos e filtra por tipo INDIVIDUAL)
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
        
        // Verificar se já existe este prémio para este estudante neste projeto
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
    }
    
    // Método legacy sem projectId (para compatibilidade)
    public void assignAwardToStudent(Long awardId, Long studentId) {
        Award award = getAwardById(awardId);
        User student = userRepo.findById(studentId).orElseThrow();

        StudentAward sa = new StudentAward();
        sa.setAward(award);
        sa.setStudent(student);
        sa.setPointsEarned(award.getPoints());
        studentAwardRepo.save(sa);

        updateUserScore(student);
    }

    public void assignAwardToTeam(Long awardId, Long teamId, Long projectId) {
        Award award = getAwardById(awardId);
        Team team = teamRepo.findById(teamId).orElseThrow();
        Project project = projectRepo.findById(projectId).orElseThrow();
        
        // Verificar se já existe este prémio para esta equipa neste projeto
        if (teamAwardRepo.existsByTeamIdAndAwardIdAndProjectId(teamId, awardId, projectId)) {
            throw new RuntimeException("Este prémio já foi atribuído a esta equipa neste projeto.");
        }

        TeamAward ta = new TeamAward();
        ta.setAward(award);
        ta.setTeam(team);
        ta.setProject(project);
        ta.setPointsEarned(award.getPoints());
        teamAwardRepo.save(ta);

        // Atualizar score da equipa
        updateTeamScore(team);

        // Opcional: Atualizar score individual dos membros
        if (team.getScrumMaster() != null) {
            updateUserScore(team.getScrumMaster());
        }
        if (team.getProductOwner() != null) {
            updateUserScore(team.getProductOwner());
        }
        for (User dev : team.getDevelopers()) {
            updateUserScore(dev);
        }
    }
    
    // Método legacy sem projectId (para compatibilidade)
    public void assignAwardToTeam(Long awardId, Long teamId) {
        Award award = getAwardById(awardId);
        Team team = teamRepo.findById(teamId).orElseThrow();

        TeamAward ta = new TeamAward();
        ta.setAward(award);
        ta.setTeam(team);
        ta.setPointsEarned(award.getPoints());
        teamAwardRepo.save(ta);

        updateTeamScore(team);

        if (team.getScrumMaster() != null) {
            updateUserScore(team.getScrumMaster());
        }
        if (team.getProductOwner() != null) {
            updateUserScore(team.getProductOwner());
        }
        for (User dev : team.getDevelopers()) {
            updateUserScore(dev);
        }
    }

    public int calculateTotalPoints(Long studentId) {
        // 1. Pontos Individuais
        int individualPoints = studentAwardRepo.findAllByStudentId(studentId).stream()
                .mapToInt(StudentAward::getPointsEarned).sum();

        // 2. Pontos vindos da Equipa (CORRIGIDO PARA SUPORTAR MÚLTIPLAS EQUIPAS)
        int teamPoints = 0;
        List<Team> teams = teamRepo.findTeamByUserId(studentId); // Agora retorna Lista
        
        if (teams != null && !teams.isEmpty()) {
            // Itera sobre todas as equipas do aluno e soma os pontos
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
            
            // Tenta associar equipa se existir (CORRIGIDO PARA LISTA)
            List<Team> teams = teamRepo.findTeamByUserId(user.getId());
            if (teams != null && !teams.isEmpty()) {
                // Como é um score global do user, associamos à primeira equipa encontrada 
                // apenas para preencher o campo, caso a BD obrigue.
                score.setTeam(teams.get(0));
            }
        }
        score.setTotalPoints(total);
        scoreRepo.save(score);
    }

    private void updateTeamScore(Team team) {
        // Calcula pontos totais da equipa (soma de TeamAwards)
        int total = teamAwardRepo.findByTeamId(team.getId()).stream()
                .mapToInt(TeamAward::getPointsEarned).sum();

        Score score = scoreRepo.findByTeamAndUserIsNull(team);
        if (score == null) {
            score = new Score();
            score.setTeam(team);
            score.setUser(null); // Importante: User a null indica Score de Equipa
        }
        score.setTotalPoints(total);
        scoreRepo.save(score);
    }
}