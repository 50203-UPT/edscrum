package pt.up.edscrum.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import pt.up.edscrum.model.Team;
import pt.up.edscrum.model.User;
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
    private final pt.up.edscrum.repository.EnrollmentRepository enrollmentRepository;
    private final pt.up.edscrum.service.AwardService awardService;

    public TeamService(TeamRepository teamRepository,
            pt.up.edscrum.repository.EnrollmentRepository enrollmentRepository,
            pt.up.edscrum.service.AwardService awardService) {
        this.teamRepository = teamRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.awardService = awardService;
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

        Team saved = teamRepository.save(team);

        // Atribuir prémio de equipa automático por formação da equipa
        try {
            awardService.assignAutomaticAwardToTeamByName("Equipa Formada", "A tua equipa foi formada.", 30, saved.getId(), null);
        } catch (Exception e) {
        }

        // Após criar equipa, verificar participação em projetos para cada membro
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
