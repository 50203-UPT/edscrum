package pt.up.edscrum.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import pt.up.edscrum.model.UserStory;

public interface UserStoryRepository extends JpaRepository<UserStory, Long> {

    List<UserStory> findBySprintId(Long sprintId);
}
