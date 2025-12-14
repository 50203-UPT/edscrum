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
import org.springframework.web.bind.annotation.RequestParam;
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
        public ResponseEntity<?> getStudentTeamInCourse(
            @PathVariable Long studentId,
            @PathVariable Long courseId,
            jakarta.servlet.http.HttpSession session) {
        Long currentUserId = (Long) session.getAttribute("currentUserId");
        String currentUserRole = (String) session.getAttribute("currentUserRole");
        if (currentUserId == null) return ResponseEntity.status(401).build();
        if (!currentUserId.equals(studentId) && !"TEACHER".equals(currentUserRole)) return ResponseEntity.status(403).build();
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
     * Session-based variant: get the calling student's team for a course.
     * Teachers may pass an optional `studentId` query param to act on behalf of a student.
     */
    @GetMapping("/student-team/course/{courseId}")
    public ResponseEntity<?> getStudentTeamInCourseSession(
            @PathVariable Long courseId,
            @RequestParam(required = false) Long studentId,
            jakarta.servlet.http.HttpSession session) {
        Long currentUserId = (Long) session.getAttribute("currentUserId");
        String currentUserRole = (String) session.getAttribute("currentUserRole");
        if (currentUserId == null) return ResponseEntity.status(401).build();

        Long targetStudentId = studentId != null ? studentId : currentUserId;
        if (!targetStudentId.equals(currentUserId) && !"TEACHER".equals(currentUserRole)) return ResponseEntity.status(403).build();

        Team team = teamService.getStudentTeamInCourse(targetStudentId, courseId);

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
     * Returns detailed info about available roles
     */
    @GetMapping("/available-teams/{courseId}")
    public ResponseEntity<List<Map<String, Object>>> getAvailableTeamsByCourse(@PathVariable Long courseId) {
        List<Team> teams = teamService.getAvailableTeamsForStudentByCourse(courseId);
        
        List<Map<String, Object>> result = teams.stream().map(team -> {
            Map<String, Object> teamInfo = new HashMap<>();
            teamInfo.put("id", team.getId());
            teamInfo.put("name", team.getName());
            teamInfo.put("currentMemberCount", team.getCurrentMemberCount());
            teamInfo.put("maxMembers", team.getMaxMembers());
            teamInfo.put("hasProductOwner", team.getProductOwner() != null);
            teamInfo.put("hasScrumMaster", team.getScrumMaster() != null);
            teamInfo.put("developerCount", team.getDevelopers() != null ? team.getDevelopers().size() : 0);
            return teamInfo;
        }).collect(Collectors.toList());
        
        return ResponseEntity.ok(result);
    }

    /**
     * Student joins a team with a specific role
     */
    @PostMapping("/{teamId}/join")
        public ResponseEntity<Map<String, String>> joinTeam(
            @PathVariable Long teamId,
            @RequestBody Map<String, Object> payload,
            jakarta.servlet.http.HttpSession session) {
        Long currentUserId = (Long) session.getAttribute("currentUserId");
        String currentUserRole = (String) session.getAttribute("currentUserRole");

        Long studentId = null;
        if (payload.get("studentId") != null) {
            studentId = ((Number) payload.get("studentId")).longValue();
        }
        String role = (String) payload.getOrDefault("role", "DEVELOPER");
        if (studentId == null) {
            studentId = currentUserId;
        }

        if (currentUserId == null) {
            Map<String, String> resp = new HashMap<>(); resp.put("error", "Unauthorized"); return ResponseEntity.status(401).body(resp);
        }
        if (!currentUserId.equals(studentId) && !"TEACHER".equals(currentUserRole)) {
            Map<String, String> resp = new HashMap<>(); resp.put("error", "Forbidden"); return ResponseEntity.status(403).body(resp);
        }
        
        Map<String, String> response = new HashMap<>();
        try {
            // Get student user
            User student = userRepository.findById(studentId)
                    .orElseThrow(() -> new RuntimeException("Aluno não encontrado"));
            
            // Add student to team with specified role
            teamService.addStudentToTeamWithRole(teamId, studentId, student, role);
            
            response.put("success", "Juntaste-te à equipa com sucesso!");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/{teamId}/add-member")
    public ResponseEntity<?> addMemberByTeacher(@PathVariable Long teamId, @RequestBody Map<String, Object> payload) {
        try {
            Long studentId = Long.valueOf(payload.get("studentId").toString());
            String role = (String) payload.get("role");

            // Validate that the caller is a teacher
            jakarta.servlet.http.HttpSession session = null;
            try { session = ((org.springframework.web.context.request.ServletRequestAttributes) org.springframework.web.context.request.RequestContextHolder.currentRequestAttributes()).getRequest().getSession(false); } catch (Exception e) { session = null; }
            if (session == null) return ResponseEntity.status(401).body("Unauthorized");
            Long currentUserId = (Long) session.getAttribute("currentUserId");
            String currentUserRole = (String) session.getAttribute("currentUserRole");
            if (!"TEACHER".equals(currentUserRole)) return ResponseEntity.status(403).body("Forbidden");

            User student = userRepository.findById(studentId)
                    .orElseThrow(() -> new RuntimeException("Aluno não encontrado"));
            
            teamService.addStudentToTeamWithRole(teamId, studentId, student, role);
            
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Get coursemates for a specific course (only students WITHOUT a team)
     * Also includes the course teacher for Product Owner selection
     */
    @GetMapping("/course-mates/{courseId}")
    public ResponseEntity<Map<String, Object>> getCoursemates(@PathVariable Long courseId) {
        List<Enrollment> enrollments = enrollmentRepository.findByCourseId(courseId);
        List<User> students = enrollments.stream()
                .map(Enrollment::getStudent)
                .filter(student -> "STUDENT".equals(student.getRole()))
                // Filter out students who already have a team in this course
                .filter(student -> teamService.getStudentTeamInCourse(student.getId(), courseId) == null)
                .collect(Collectors.toList());
        
        // Get the course teacher
        User teacher = null;
        if (!enrollments.isEmpty() && enrollments.get(0).getCourse() != null) {
            teacher = enrollments.get(0).getCourse().getTeacher();
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("students", students);
        response.put("teacher", teacher);
        
        return ResponseEntity.ok(response);
    }
}

