package pt.up.edscrum.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import pt.up.edscrum.model.Project;

public interface ProjectRepository extends JpaRepository<Project, Long> {
}
