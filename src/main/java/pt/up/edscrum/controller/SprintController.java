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

import pt.up.edscrum.model.Sprint;
import pt.up.edscrum.service.SprintService;

@RestController
@RequestMapping("/sprints")
@CrossOrigin(origins = "*")
public class SprintController {

    private final SprintService sprintService;

    public SprintController(SprintService sprintService) {
        this.sprintService = sprintService;
    }

    /**
     * Obtém todos os sprints de um projeto.
     *
     * @param projectId ID do projeto
     * @return Lista de Sprint
     */
    @GetMapping("/project/{projectId}")
    public List<Sprint> getSprints(@PathVariable Long projectId) {
        return sprintService.getSprintsByProject(projectId);
    }

    /**
     * Cria um sprint para um projeto.
     *
     * @param projectId ID do projeto
     * @param sprint Dados do sprint
     * @return Sprint criado
     */
    @PostMapping("/project/{projectId}")
    public Sprint createSprint(@PathVariable Long projectId, @RequestBody Sprint sprint) {
        return sprintService.createSprint(projectId, sprint);
    }

    /**
     * Atualiza um sprint existente.
     *
     * @param sprintId ID do sprint
     * @param sprint Dados atualizados
     * @return Sprint atualizado
     */
    @PutMapping("/{sprintId}")
    public Sprint updateSprint(@PathVariable Long sprintId, @RequestBody Sprint sprint) {
        return sprintService.updateSprint(sprintId, sprint);
    }

    /**
     * Marca um sprint como concluído.
     *
     * @param sprintId ID do sprint
     * @return Sprint após conclusão
     */
    @PostMapping("/{sprintId}/complete")
    public Sprint completeSprint(@PathVariable Long sprintId) {
        return sprintService.completeSprint(sprintId);
    }

    /**
     * Reabre um sprint previamente concluído.
     *
     * @param sprintId ID do sprint
     * @return Sprint reaberto
     */
    @PostMapping("/{sprintId}/reopen")
    public Sprint reopenSprint(@PathVariable Long sprintId) {
        return sprintService.reopenSprint(sprintId);
    }

    /**
     * Elimina um sprint pelo ID.
     *
     * @param sprintId ID do sprint a eliminar
     */
    @DeleteMapping("/{sprintId}")
    public void deleteSprint(@PathVariable Long sprintId) {
        sprintService.deleteSprint(sprintId);
    }
}
