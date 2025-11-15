package pt.up.edscrum.controller;

import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pt.up.edscrum.model.Award;
import pt.up.edscrum.model.StudentAward;
import pt.up.edscrum.service.AwardService;

@RestController
@RequestMapping("/awards")
@CrossOrigin(origins = "*")
public class AwardController {

    private final AwardService awardService;

    public AwardController(AwardService awardService) {
        this.awardService = awardService;
    }

    // CRUD --------------------------
    @GetMapping
    public List<Award> getAll() {
        return awardService.getAllAwards();
    }

    @GetMapping("/{id}")
    public Award getById(@PathVariable Long id) {
        return awardService.getAwardById(id);
    }

    @PostMapping
    public Award create(@RequestBody Award award) {
        return awardService.createAward(award);
    }

    @PutMapping("/{id}")
    public Award update(@PathVariable Long id, @RequestBody Award award) {
        return awardService.updateAward(id, award);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        awardService.deleteAward(id);
    }

    // ATRIBUI PONTOS A UM STUDENT -------------------------
    @PostMapping("/assign/{awardId}/to/{studentId}")
    public StudentAward assignAward(
            @PathVariable Long awardId,
            @PathVariable Long studentId) {
        return awardService.assignAwardToStudent(awardId, studentId);
    }

    // CALCULA TOTAL DE PONTOS -----------------------------
    @GetMapping("/points/{studentId}")
    public int getTotalPoints(@PathVariable Long studentId) {
        return awardService.calculateTotalPoints(studentId);
    }
}
