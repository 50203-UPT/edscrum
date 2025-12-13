package pt.up.edscrum.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import pt.up.edscrum.model.UserStory;

public interface UserStoryRepository extends JpaRepository<UserStory, Long> {

    /**
     * Obtém as user stories associadas a um sprint.
     *
     * @param sprintId id do sprint
     * @return lista de `UserStory` pertencentes ao sprint
     */
    List<UserStory> findBySprintId(Long sprintId);
}
