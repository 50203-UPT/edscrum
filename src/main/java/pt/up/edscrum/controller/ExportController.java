package pt.up.edscrum.controller;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pt.up.edscrum.dto.dashboard.RankingDTO;
import pt.up.edscrum.model.Course;
import pt.up.edscrum.service.CourseService;
import pt.up.edscrum.service.DashboardService;
import pt.up.edscrum.service.UserService;
import pt.up.edscrum.service.TeamService;
import pt.up.edscrum.model.User;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/export")
public class ExportController {

    private final CourseService courseService;
    private final DashboardService dashboardService;
    private final UserService userService;
    private final TeamService teamService;

    public ExportController(CourseService courseService, DashboardService dashboardService, UserService userService, TeamService teamService) {
        this.courseService = courseService;
        this.dashboardService = dashboardService;
        this.userService = userService;
        this.teamService = teamService;
    }

    private Map<Long, List<String>> getStudentCoursesMap(List<Course> teacherCourses) {
        Map<Long, List<String>> studentCoursesMap = new HashMap<>();
        if (teacherCourses == null) return studentCoursesMap;
        for (Course course : teacherCourses) {
            List<User> enrolled = courseService.getEnrolledStudentsByCourse(course.getId());
            if (enrolled == null) continue;
            for (User u : enrolled) {
                studentCoursesMap.computeIfAbsent(u.getId(), k -> new ArrayList<>()).add(course.getName());
            }
        }
        return studentCoursesMap;
    }

    private Map<Long, String> getStudentTeamMap(List<Course> teacherCourses, List<RankingDTO> rankings) {
        Map<Long, String> studentTeamMap = new HashMap<>();
        if (rankings == null) return studentTeamMap;
        for (RankingDTO r : rankings) {
            try {
                List<pt.up.edscrum.model.Team> teams = teamService.findTeamsByUserId(r.getId());
                if (teams != null) {
                    for (pt.up.edscrum.model.Team t : teams) {
                        if (t.getCourse() != null && teacherCourses != null) {
                            for (Course c : teacherCourses) {
                                if (t.getCourse().getId().equals(c.getId())) {
                                    studentTeamMap.put(r.getId(), t.getName());
                                    break;
                                }
                            }
                        }
                        if (studentTeamMap.containsKey(r.getId())) break;
                    }
                }
            } catch (Exception e) {
                // ignore
            }
        }
        return studentTeamMap;
    }

    /**
     * Exporta rankings agregados para um professor em formato CSV.
     *
     * @param teacherId ID do professor
     * @return ResponseEntity com o ficheiro CSV em bytes
     */
    @GetMapping("/rankings/csv/{teacherId}")
    public ResponseEntity<byte[]> exportRankingsCsv(@PathVariable Long teacherId) {
        List<Course> teacherCourses = courseService.getCoursesByTeacher(teacherId);

        Map<Long, RankingDTO> byStudent = new HashMap<>();
        if (teacherCourses != null) {
            for (Course c : teacherCourses) {
                List<RankingDTO> rlist = dashboardService.getStudentRanking(c.getId());
                if (rlist == null) {
                    continue;
                }
                for (RankingDTO r : rlist) {
                    RankingDTO existing = byStudent.get(r.getId());
                    if (existing == null) {
                        byStudent.put(r.getId(), new RankingDTO(r.getId(), r.getName(), r.getTotalPoints()));
                    } else {
                        if (r.getTotalPoints() > existing.getTotalPoints()) {
                            byStudent.put(r.getId(), new RankingDTO(r.getId(), r.getName(), r.getTotalPoints()));
                        }
                    }
                }
            }
        }

        List<RankingDTO> rankings = new ArrayList<>(byStudent.values());
        rankings.sort((r1, r2) -> Long.compare(r2.getTotalPoints(), r1.getTotalPoints()));

        StringBuilder sb = new StringBuilder();

        // BOM (UTF-8) so Excel detects encoding correctly
        sb.append('\uFEFF');

        final String sep = ";";
        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String teacherName = "";
        try { User t = userService.getUserById(teacherId); teacherName = t.getName(); } catch (Exception e) { }

        Map<Long, List<String>> studentCoursesMap = getStudentCoursesMap(teacherCourses);
        Map<Long, String> studentTeamMap = getStudentTeamMap(teacherCourses, rankings);

                // File header summary: label, date and time in separate cells
                LocalDateTime now = LocalDateTime.now();
                sb.append("Report Generated").append(sep)
                    .append('"').append(now.toLocalDate().format(DateTimeFormatter.ISO_DATE)).append('"').append(sep)
                    .append('"').append(now.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm:ss"))).append('"').append("\r\n");
        sb.append("Teacher").append(sep).append('"').append(teacherName).append('"').append("\r\n");
        sb.append("Total Students").append(sep).append(rankings.size()).append("\r\n\r\n");

        // Students table header
        sb.append("Position").append(sep).append("UPT_ID").append(sep).append("StudentTag").append(sep).append("Name").append(sep).append("Email").append(sep).append("Courses").append(sep).append("Team").append(sep).append("TotalPoints").append("\r\n");

        for (int i = 0; i < rankings.size(); i++) {
            RankingDTO r = rankings.get(i);
            String studentTag = "";
            String email = "";
            try {
                User u = userService.getUserById(r.getId());
                studentTag = u.getStudentTag();
                email = u.getEmail() != null ? u.getEmail() : "";
            } catch (Exception e) { }

            String name = r.getName() != null ? r.getName().replace("\"", "\"\"") : "";
            List<String> coursesList = studentCoursesMap.getOrDefault(r.getId(), new ArrayList<>());
            String courses = String.join(", ", coursesList).replace("\"", "\"\"");
            String teamName = studentTeamMap.getOrDefault(r.getId(), "").replace("\"", "\"\"");

            sb.append(i + 1).append(sep)
              .append('"').append("UPT-" + r.getId()).append('"').append(sep)
              .append('"').append(studentTag).append('"').append(sep)
              .append('"').append(name).append('"').append(sep)
              .append('"').append(email).append('"').append(sep)
              .append('"').append(courses).append('"').append(sep)
              .append('"').append(teamName).append('"').append(sep)
              .append(r.getTotalPoints()).append("\r\n");
        }

        sb.append("\r\n");

        // Teams per course with members and project
        if (teacherCourses != null) {
            for (Course c : teacherCourses) {
                // Put title and course name in separate cells
                sb.append("Team Rankings for Course").append(sep).append('"').append(c.getName()).append('"').append("\r\n");
                sb.append("Position").append(sep).append("TeamID").append(sep).append("TeamName").append(sep).append("Members").append(sep).append("ProjectName").append(sep).append("TotalPoints\r\n");
                List<RankingDTO> teamRanks = dashboardService.getTeamRanking(c.getId());
                if (teamRanks == null || teamRanks.isEmpty()) {
                    sb.append("(no teams)").append("\r\n\r\n");
                    continue;
                }
                for (int j = 0; j < teamRanks.size(); j++) {
                    RankingDTO tr = teamRanks.get(j);
                    List<User> members = new ArrayList<>();
                    try { members = teamService.getTeamMembers(tr.getId()); } catch (Exception e) { }
                    String memberNames = members.stream().map(m -> m.getName().replace("\"", "\"\"")).collect(java.util.stream.Collectors.joining(", "));
                    String projectName = "";
                    try { pt.up.edscrum.model.Team team = teamService.getTeamById(tr.getId()); if (team != null && team.getProject() != null) projectName = team.getProject().getName().replace("\"", "\"\""); } catch (Exception e) { }

                    sb.append(j + 1).append(sep)
                      .append(tr.getId() != null ? tr.getId() : 0).append(sep)
                      .append('"').append(tr.getName() != null ? tr.getName().replace("\"", "\"\"") : "").append('"').append(sep)
                      .append('"').append(memberNames).append('"').append(sep)
                      .append('"').append(projectName).append('"').append(sep)
                      .append(tr.getTotalPoints()).append("\r\n");
                }
                sb.append("\r\n");
            }
        }

        byte[] csvBytes = sb.toString().getBytes(StandardCharsets.UTF_8);

        String filename = "rankings_teacher_" + teacherName + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .header("Pragma", "no-cache")
                .header("Cache-Control", "no-cache, no-store, must-revalidate")
                .header("Expires", "0")
                .contentLength(csvBytes.length)
                .body(csvBytes);
    }
}
