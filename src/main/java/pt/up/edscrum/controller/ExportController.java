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

@RestController
@RequestMapping("/export")
public class ExportController {

    private final CourseService courseService;
    private final DashboardService dashboardService;

    public ExportController(CourseService courseService, DashboardService dashboardService) {
        this.courseService = courseService;
        this.dashboardService = dashboardService;
    }

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

        // Individual students aggregated across teacher courses
        sb.append("# Individual Student Rankings\n");
        sb.append("Position,UPT_ID,Name,TotalPoints\n");
        for (int i = 0; i < rankings.size(); i++) {
            RankingDTO r = rankings.get(i);
            sb.append(i + 1).append(",");
            sb.append("UPT-").append(r.getId()).append(",");
            String name = r.getName() != null ? r.getName().replace("\"", "\"\"") : "";
            sb.append('"').append(name).append('"').append(",");
            sb.append(r.getTotalPoints()).append("\n");
        }

        sb.append("\n");

        // Teams per course
        if (teacherCourses != null) {
            for (Course c : teacherCourses) {
                sb.append("# Team Rankings for Course: ").append(c.getName()).append(" (ID:").append(c.getId()).append(")\n");
                sb.append("Position,TeamName,TotalPoints\n");
                List<RankingDTO> teamRanks = dashboardService.getTeamRanking(c.getId());
                if (teamRanks == null || teamRanks.isEmpty()) {
                    sb.append("\n");
                    continue;
                }
                for (int j = 0; j < teamRanks.size(); j++) {
                    RankingDTO tr = teamRanks.get(j);
                    sb.append(j + 1).append(",");
                    String tname = tr.getName() != null ? tr.getName().replace("\"", "\"\"") : "";
                    sb.append('"').append(tname).append('"').append(",");
                    sb.append(tr.getTotalPoints()).append("\n");
                }
                sb.append("\n");
            }
        }

        byte[] csvBytes = sb.toString().getBytes(StandardCharsets.UTF_8);

        String filename = "rankings_teacher_" + teacherId + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .contentLength(csvBytes.length)
                .body(csvBytes);
    }
}
