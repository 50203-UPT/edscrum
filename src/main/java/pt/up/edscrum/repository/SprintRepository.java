package pt.up.edscrum.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import pt.up.edscrum.model.Sprint;

/**
 * Repositório para manipulação de `Sprint`.
 */
public interface SprintRepository extends JpaRepository<Sprint, Long> {

    /**
     * Encontra sprints associados a um projeto.
     *
     * @param projectId id do projeto
     * @return lista de `Sprint`
     */
    List<Sprint> findByProjectId(Long projectId);

    /**
     * Encontra sprints criados por um utilizador.
     *
     * @param createdById id do utilizador que criou os sprints
     * @return lista de `Sprint`
     */
    List<Sprint> findByCreatedById(Long createdById);
}
