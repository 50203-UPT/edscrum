package pt.up.edscrum.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pt.up.edscrum.model.Enrollment;
import pt.up.edscrum.model.Team;
import pt.up.edscrum.model.User;
import pt.up.edscrum.repository.EnrollmentRepository;
import pt.up.edscrum.repository.UserRepository;
import pt.up.edscrum.service.TeamService;

@RestController
@RequestMapping("/api/teams")
public class TeamController {

    private final TeamService teamService;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;

    public TeamController(TeamService teamService, EnrollmentRepository enrollmentRepository, UserRepository userRepository) {
        this.teamService = teamService;
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
    }

    /**
     * Obtém todas as equipas.
     *
     * @return Lista de Team
     */
    @GetMapping
    public List<Team> getAllTeams() {
        return teamService.getAllTeams();
    }

    /**
     * Obtém uma equipa por ID.
     *
     * @param id ID da equipa
     * @return ResponseEntity com o Team
     */
    @GetMapping("/{id}")
    public ResponseEntity<Team> getTeamById(@PathVariable Long id) {
        Team team = teamService.getTeamById(id);
        return ResponseEntity.ok(team);
    }

    /**
     * Cria uma nova equipa.
     *
     * @param team Dados da equipa no corpo da requisição
     * @return ResponseEntity com o Team criado (201)
     */
    @PostMapping
    public ResponseEntity<Team> createTeam(@RequestBody Team team) {
        Team created = teamService.createTeam(team);
        return ResponseEntity.status(201).body(created);
    }

    /**
     * Atualiza uma equipa existente.
     *
     * @param id ID da equipa a atualizar
     * @param teamDetails Dados atualizados
     * @return ResponseEntity com o Team atualizado
     */
    @PutMapping("/{id}")
    public ResponseEntity<Team> updateTeam(@PathVariable Long id, @RequestBody Team teamDetails) {
        Team updated = teamService.updateTeam(id, teamDetails);
        return ResponseEntity.ok(updated);
    }

    /**
     * Elimina uma equipa por ID.
     *
     * @param id ID da equipa a eliminar
     * @return ResponseEntity sem conteúdo (204)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTeam(@PathVariable Long id) {
        teamService.deleteTeam(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get student's team in a specific course
     */
    @GetMapping("/student-team/{studentId}/course/{courseId}")
    public ResponseEntity<Map<String, Object>> getStudentTeamInCourse(
            @PathVariable Long studentId,
            @PathVariable Long courseId) {
        Team team = teamService.getStudentTeamInCourse(studentId, courseId);
        
        Map<String, Object> response = new HashMap<>();
        if (team != null) {
            response.put("teamId", team.getId());
            response.put("name", team.getName());
            response.put("productOwner", team.getProductOwner());
            response.put("scrumMaster", team.getScrumMaster());
            response.put("developers", team.getDevelopers());
            response.put("currentMemberCount", team.getCurrentMemberCount());
            response.put("maxMembers", team.getMaxMembers());
            response.put("isClosed", team.isClosed());
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get available teams for a course (open and with slots)
     */
    @GetMapping("/available-teams/{courseId}")
    public ResponseEntity<List<Team>> getAvailableTeamsByCourse(@PathVariable Long courseId) {
        List<Team> teams = teamService.getAvailableTeamsForStudentByCourse(courseId);
        return ResponseEntity.ok(teams);
    }

    /**
     * Student joins a team
     */
    @PostMapping("/{teamId}/join")
    public ResponseEntity<Map<String, String>> joinTeam(
            @PathVariable Long teamId,
            @RequestBody Map<String, Long> payload) {
        Long studentId = payload.get("studentId");
        
        Map<String, String> response = new HashMap<>();
        try {
            // Get student user
            User student = userRepository.findById(studentId)
                    .orElseThrow(() -> new RuntimeException("Aluno não encontrado"));
            
            // Add student to team as developer
            teamService.addStudentToTeam(teamId, studentId, student);
            
            response.put("success", "Juntaste-te à equipa com sucesso!");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("error", "Erro ao juntar-se à equipa: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Get coursemates for a specific course (only students WITHOUT a team)
     */
    @GetMapping("/course-mates/{courseId}")
    public ResponseEntity<List<User>> getCoursemates(@PathVariable Long courseId) {
        List<Enrollment> enrollments = enrollmentRepository.findByCourseId(courseId);
        List<User> students = enrollments.stream()
                .map(Enrollment::getStudent)
                .filter(student -> "STUDENT".equals(student.getRole()))
                // Filter out students who already have a team in this course
                .filter(student -> teamService.getStudentTeamInCourse(student.getId(), courseId) == null)
                .collect(Collectors.toList());
        return ResponseEntity.ok(students);
    }
}

