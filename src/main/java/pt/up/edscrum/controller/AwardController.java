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

    /**
     * Obtém todos os prémios disponíveis.
     *
     * @return Lista de Award
     */
    @GetMapping
    public List<Award> getAll() {
        return awardService.getAllAwards();
    }

    /**
     * Obtém um prémio pelo seu ID.
     *
     * @param id ID do prémio
     * @return Award correspondente
     */
    @GetMapping("/{id}")
    public Award getById(@PathVariable Long id) {
        return awardService.getAwardById(id);
    }

    /**
     * Cria um novo prémio.
     *
     * @param award Dados do prémio
     * @return Award criado
     */
    @PostMapping
    public Award create(@RequestBody Award award) {
        return awardService.createAward(award);
    }

    /**
     * Atualiza um prémio existente.
     *
     * @param id ID do prémio a atualizar
     * @param award Dados atualizados
     * @return Award atualizado
     */
    @PutMapping("/{id}")
    public Award update(@PathVariable Long id, @RequestBody Award award) {
        return awardService.updateAward(id, award);
    }

    /**
     * Elimina um prémio pelo ID.
     *
     * @param id ID do prémio a eliminar
     */
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        awardService.deleteAward(id);
    }

    /**
     * Obtém prémios disponíveis (não atribuídos) para uma equipa num projeto.
     *
     * @param teamId ID da equipa
     * @param projectId ID do projeto
     * @return Lista de Award disponíveis
     */
    @GetMapping("/available-for-team")
    public List<Award> getAvailableAwardsForTeam(@RequestParam Long teamId, @RequestParam Long projectId) {
        return awardService.getAvailableAwardsForTeam(teamId, projectId);
    }

    /**
     * Obtém prémios disponíveis (não atribuídos) para um estudante num projeto.
     *
     * @param studentId ID do estudante
     * @param projectId ID do projeto
     * @return Lista de Award disponíveis
     */
    @GetMapping("/available-for-student")
    public List<Award> getAvailableAwardsForStudent(@RequestParam Long studentId, @RequestParam Long projectId, jakarta.servlet.http.HttpSession session) {
        Long currentUserId = (Long) session.getAttribute("currentUserId");
        String currentUserRole = (String) session.getAttribute("currentUserRole");
        if (currentUserId == null) return List.of();
        if (!currentUserId.equals(studentId) && !"TEACHER".equals(currentUserRole)) return List.of();
        return awardService.getAvailableAwardsForStudent(studentId, projectId);
    }

    /**
     * Atribui um prémio a um estudante.
     *
     * @param awardId ID do prémio
     * @param studentId ID do estudante
     */
    @PostMapping("/assign/{awardId}/to/{studentId}")
        public org.springframework.http.ResponseEntity<?> assignAwardToStudent(
            @PathVariable Long awardId,
            @PathVariable Long studentId,
            jakarta.servlet.http.HttpSession session) {
        Long currentUserId = (Long) session.getAttribute("currentUserId");
        String currentUserRole = (String) session.getAttribute("currentUserRole");
        if (currentUserId == null) return org.springframework.http.ResponseEntity.status(401).build();
        if (!currentUserId.equals(studentId) && !"TEACHER".equals(currentUserRole)) return org.springframework.http.ResponseEntity.status(403).build();
        awardService.assignAwardToStudent(awardId, studentId);
        return org.springframework.http.ResponseEntity.ok().build();
    }

    /**
     * Atribui um prémio a uma equipa.
     *
     * @param awardId ID do prémio
     * @param teamId ID da equipa
     */
    @PostMapping("/assign/{awardId}/to-team/{teamId}")
    public void assignAwardToTeam(
            @PathVariable Long awardId,
            @PathVariable Long teamId) {
        awardService.assignAwardToTeam(awardId, teamId);
    }

    /**
     * Calcula o total de pontos de um estudante.
     *
     * @param studentId ID do estudante
     * @return Total de pontos
     */
    @GetMapping("/points/{studentId}")
    public org.springframework.http.ResponseEntity<Integer> getTotalPoints(@PathVariable Long studentId, jakarta.servlet.http.HttpSession session) {
        Long currentUserId = (Long) session.getAttribute("currentUserId");
        String currentUserRole = (String) session.getAttribute("currentUserRole");
        if (currentUserId == null) return org.springframework.http.ResponseEntity.status(401).build();
        if (!currentUserId.equals(studentId) && !"TEACHER".equals(currentUserRole)) return org.springframework.http.ResponseEntity.status(403).build();
        int pts = awardService.calculateTotalPoints(studentId);
        return org.springframework.http.ResponseEntity.ok(pts);
    }
}
