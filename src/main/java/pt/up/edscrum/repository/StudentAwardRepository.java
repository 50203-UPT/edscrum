package pt.up.edscrum.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import pt.up.edscrum.model.StudentAward;

public interface StudentAwardRepository extends JpaRepository<StudentAward, Long> {

    List<StudentAward> findByStudentId(Long studentId);
}
