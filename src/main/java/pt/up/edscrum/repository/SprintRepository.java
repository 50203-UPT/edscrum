package pt.up.edscrum.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import pt.up.edscrum.model.Sprint;

public interface SprintRepository extends JpaRepository<Sprint, Long> {

    List<Sprint> findByProjectId(Long projectId);
}
