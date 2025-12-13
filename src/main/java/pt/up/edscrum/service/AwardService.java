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
import pt.up.edscrum.repository.SprintRepository;
import pt.up.edscrum.repository.StudentAwardRepository;
import pt.up.edscrum.repository.TeamAwardRepository;
import pt.up.edscrum.repository.TeamRepository;
import pt.up.edscrum.repository.UserRepository;

@Service
/**
 * Serviço que gere prémios (Awards) automáticos e manuais, bem como os registos
 * de prémios atribuídos a estudantes e equipas. Contém lógica para
 * criação/consulta/atualização/eliminar prémios, atribuições automáticas
 * baseadas em eventos (por exemplo criação de sprints) e atualizações dos
 * scores de utilizadores e equipas.
 */
public class AwardService {

    private final AwardRepository awardRepo;
    private final StudentAwardRepository studentAwardRepo;
    private final TeamAwardRepository teamAwardRepo;
    private final UserRepository userRepo;
    private final ScoreRepository scoreRepo;
    private final TeamRepository teamRepo;
    private final ProjectRepository projectRepo;
    private final SprintRepository sprintRepo;

    public AwardService(AwardRepository awardRepo, StudentAwardRepository studentAwardRepo,
            TeamAwardRepository teamAwardRepo, UserRepository userRepo,
            ScoreRepository scoreRepo, TeamRepository teamRepo, ProjectRepository projectRepo,
            SprintRepository sprintRepo) {
        this.awardRepo = awardRepo;
        this.studentAwardRepo = studentAwardRepo;
        this.teamAwardRepo = teamAwardRepo;
        this.userRepo = userRepo;
        this.scoreRepo = scoreRepo;
        this.teamRepo = teamRepo;
        this.projectRepo = projectRepo;
        this.sprintRepo = sprintRepo;
    }

    /**
     * Garantir que existe um prémio automático com o nome fornecido. Se não
     * existir, cria-o com os atributos fornecidos.
     *
     * @param name nome do prémio
     * @param description descrição do prémio
     * @param points pontos associados ao prémio
     * @param targetType tipo de alvo ("INDIVIDUAL" ou "TEAM")
     * @return o `Award` existente ou recém-criado
     */
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

    /**
     * Atribui um prémio automático a um estudante identificado por `studentId`.
     * O prémio é criado automaticamente se não existir e evita duplicados para
     * o mesmo estudante (e projeto, se fornecido).
     *
     * @param name nome do prémio automático
     * @param description descrição do prémio
     * @param points pontos do prémio
     * @param studentId id do estudante
     * @param projectId id do projeto (pode ser null)
     */
    public void assignAutomaticAwardToStudentByName(String name, String description, int points, Long studentId, Long projectId) {
        Award award = ensureAutomaticAward(name, description, points, "INDIVIDUAL");
        // Verifica se já existe
        boolean exists = (projectId != null) ? studentAwardRepo.existsByStudentIdAndAwardIdAndProjectId(studentId, award.getId(), projectId)
                : studentAwardRepo.findAllByStudentId(studentId).stream().anyMatch(sa -> sa.getAward().getId().equals(award.getId()));

        if (exists) {
            return;
        }

        StudentAward sa = new StudentAward();
        sa.setAward(award);
        sa.setStudent(userRepo.findById(studentId).orElseThrow());
        if (projectId != null) {
            sa.setProject(projectRepo.findById(projectId).orElse(null));
        }
        sa.setPointsEarned(award.getPoints());
        studentAwardRepo.save(sa);

        updateUserScore(sa.getStudent());
    }

    /**
     * Atribui um prémio automático a uma equipa identificado por `teamId`. O
     * prémio é criado automaticamente se não existir e evita duplicados para a
     * mesma equipa (e projeto, se fornecido).
     *
     * @param name nome do prémio automático
     * @param description descrição do prémio
     * @param points pontos do prémio
     * @param teamId id da equipa
     * @param projectId id do projeto (pode ser null)
     */
    public void assignAutomaticAwardToTeamByName(String name, String description, int points, Long teamId, Long projectId) {
        Award award = ensureAutomaticAward(name, description, points, "TEAM");

        boolean exists = (projectId != null) ? teamAwardRepo.existsByTeamIdAndAwardIdAndProjectId(teamId, award.getId(), projectId)
                : teamAwardRepo.findByTeamId(teamId).stream().anyMatch(ta -> ta.getAward().getId().equals(award.getId()));

        if (exists) {
            return;
        }

        TeamAward ta = new TeamAward();
        ta.setAward(award);
        ta.setTeam(teamRepo.findById(teamId).orElseThrow());
        if (projectId != null) {
            ta.setProject(projectRepo.findById(projectId).orElse(null));
        }
        ta.setPointsEarned(award.getPoints());
        teamAwardRepo.save(ta);

        // Atualizar score da equipa e dos membros
        updateTeamScore(ta.getTeam());
        if (ta.getTeam().getScrumMaster() != null) {
            updateUserScore(ta.getTeam().getScrumMaster());
        }
        if (ta.getTeam().getProductOwner() != null) {
            updateUserScore(ta.getTeam().getProductOwner());
        }
        for (User dev : ta.getTeam().getDevelopers()) {
            updateUserScore(dev);
        }
    }

    /**
     * Lógica invocada quando um estudante cria um sprint. Pode atribuir prémios
     * automáticos consoante milestones (ex: primeiro sprint, 5º sprint, 10º
     * sprint). Evita execução se `studentId` for null.
     *
     * @param studentId id do estudante que criou o sprint
     * @param projectId id do projeto associado (pode ser null)
     */
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

    /**
     * Retorna a lista de todos os prémios registados.
     *
     * @return lista de `Award`
     */
    public List<Award> getAllAwards() {
        return awardRepo.findAll();
    }

    public Award getAwardById(Long id) {
        return awardRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Award not found"));
    }

    /**
     * Cria um novo prémio.
     *
     * @param award o objeto `Award` a criar
     * @return o prémio guardado
     */
    public Award createAward(Award award) {
        return awardRepo.save(award);
    }

    /**
     * Atualiza os dados de um prémio existente.
     *
     * @param id id do prémio a atualizar
     * @param awardDetails objecto com os novos valores
     * @return o prémio atualizado
     */
    public Award updateAward(Long id, Award awardDetails) {
        Award award = getAwardById(id);
        award.setName(awardDetails.getName());
        award.setDescription(awardDetails.getDescription());
        award.setPoints(awardDetails.getPoints());
        award.setType(awardDetails.getType());
        return awardRepo.save(award);
    }

    /**
     * Elimina um prémio por id.
     *
     * @param id id do prémio a eliminar
     */
    public void deleteAward(Long id) {
        awardRepo.deleteById(id);
    }

    /**
     * Retorna os prémios disponíveis para atribuição a uma equipa num dado
     * projeto, excluindo prémios já atribuídos a essa equipa nesse projeto.
     *
     * @param teamId id da equipa
     * @param projectId id do projeto (pode ser null)
     * @return lista de prémios do tipo TEAM disponíveis
     */
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

    /**
     * Retorna os prémios disponíveis para atribuição a um estudante num dado
     * projeto, excluindo prémios já atribuídos a esse estudante nesse projeto.
     *
     * @param studentId id do estudante
     * @param projectId id do projeto
     * @return lista de prémios do tipo INDIVIDUAL disponíveis
     */
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

    /**
     * Atribui um prémio existente a um estudante num projeto. Verifica
     * duplicados e atualiza o score do estudante.
     *
     * @param awardId id do prémio
     * @param studentId id do estudante
     * @param projectId id do projeto
     */
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

    /**
     * Atribui um prémio existente a um estudante (sem projeto). Mantido por
     * compatibilidade.
     *
     * @param awardId id do prémio
     * @param studentId id do estudante
     */
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

    /**
     * Atribui um prémio existente a uma equipa num projeto. Verifica duplicados
     * e atualiza os scores da equipa e dos seus membros.
     *
     * @param awardId id do prémio
     * @param teamId id da equipa
     * @param projectId id do projeto
     */
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

    /**
     * Atribui um prémio existente a uma equipa (sem projeto). Mantido por
     * compatibilidade.
     *
     * @param awardId id do prémio
     * @param teamId id da equipa
     */
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

    /**
     * Calcula os pontos totais de um estudante somando prémios individuais e os
     * pontos das equipas a que pertence.
     *
     * @param studentId id do estudante
     * @return total de pontos
     */
    public int calculateTotalPoints(Long studentId) {
        // 1. Pontos Individuais
        int individualPoints = studentAwardRepo.findAllByStudentId(studentId).stream()
                .mapToInt(StudentAward::getPointsEarned).sum();

        // 2. Pontos vindos da Equipa (CORRIGIDO PARA SUPORTAR MÚLTIPLAS EQUIPAS)
        int teamPoints = 0;
        List<Team> teams = teamRepo.findTeamByUserId(studentId);

        if (teams != null && !teams.isEmpty()) {
            // Itera sobre todas as equipas do aluno e soma os pontos
            for (Team team : teams) {
                teamPoints += teamAwardRepo.findByTeamId(team.getId()).stream()
                        .mapToInt(TeamAward::getPointsEarned).sum();
            }
        }

        return individualPoints + teamPoints;
    }

    /**
     * Atualiza o `Score` global de um utilizador com base nos pontos
     * calculados. Depois de guardar o `Score`, verifica o ranking global para
     * atribuir prémios automáticos baseados na posição (ex: Top5, Top3).
     *
     * @param user o utilizador cujo score será atualizado
     */
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
                // Top5 award
                assignAutomaticAwardToStudentByName("Estrela da Turma (Top 5)", "Entraste no Top 5 do ranking global.", 50, user.getId(), null);
            }
            if (rank > 0 && rank <= 3) {
                // Top3 award
                assignAutomaticAwardToStudentByName("Mestre do Podium (Top 3)", "Chegaste ao Top 3 do ranking global.", 120, user.getId(), null);
            }

        } catch (Exception e) {
        }
    }

    /**
     * Atualiza o `Score` de uma equipa com a soma dos `TeamAwards` atribuídos.
     *
     * @param team equipa a atualizar
     */
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
