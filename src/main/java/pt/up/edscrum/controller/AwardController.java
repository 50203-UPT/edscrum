package pt.up.edscrum.controller;

import java.util.List;
import java.util.Set;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import pt.up.edscrum.model.Award;
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
    
    // OBTER PRÉMIOS DISPONÍVEIS (não atribuídos) PARA EQUIPA NUM PROJETO
    @GetMapping("/available-for-team")
    public List<Award> getAvailableAwardsForTeam(@RequestParam Long teamId, @RequestParam Long projectId) {
        return awardService.getAvailableAwardsForTeam(teamId, projectId);
    }
    
    // OBTER PRÉMIOS DISPONÍVEIS (não atribuídos) PARA ESTUDANTE NUM PROJETO
    @GetMapping("/available-for-student")
    public List<Award> getAvailableAwardsForStudent(@RequestParam Long studentId, @RequestParam Long projectId) {
        return awardService.getAvailableAwardsForStudent(studentId, projectId);
    }

    // ATRIBUI PONTOS A UM ESTUDANTE -------------------------
    // Corrigido: agora retorna void porque o serviço também é void
    @PostMapping("/assign/{awardId}/to/{studentId}")
    public void assignAwardToStudent(
            @PathVariable Long awardId,
            @PathVariable Long studentId) {
        awardService.assignAwardToStudent(awardId, studentId);
    }

    // ATRIBUI PONTOS A UMA EQUIPA (NOVO) --------------------
    @PostMapping("/assign/{awardId}/to-team/{teamId}")
    public void assignAwardToTeam(
            @PathVariable Long awardId,
            @PathVariable Long teamId) {
        awardService.assignAwardToTeam(awardId, teamId);
    }

    // CALCULA TOTAL DE PONTOS -----------------------------
    @GetMapping("/points/{studentId}")
    public int getTotalPoints(@PathVariable Long studentId) {
        return awardService.calculateTotalPoints(studentId);
    }
}
