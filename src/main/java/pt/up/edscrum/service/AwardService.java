package pt.up.edscrum.service;

import java.util.List;

import org.springframework.stereotype.Service;

import pt.up.edscrum.model.Award;
import pt.up.edscrum.model.StudentAward;
import pt.up.edscrum.model.User;
import pt.up.edscrum.repository.AwardRepository;
import pt.up.edscrum.repository.StudentAwardRepository;
import pt.up.edscrum.repository.UserRepository;

@Service
public class AwardService {

    private final AwardRepository awardRepository;
    private final StudentAwardRepository studentAwardRepository;
    private final UserRepository userRepository;

    public AwardService(AwardRepository awardRepository, StudentAwardRepository studentAwardRepository,
            UserRepository userRepository) {
        this.awardRepository = awardRepository;
        this.studentAwardRepository = studentAwardRepository;
        this.userRepository = userRepository;
    }

    // CRUD -------------------------------
    public List<Award> getAllAwards() {
        return awardRepository.findAll();
    }

    public Award getAwardById(Long id) {
        return awardRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Award not found"));
    }

    public Award createAward(Award award) {
        return awardRepository.save(award);
    }

    public Award updateAward(Long id, Award awardDetails) {
        Award award = getAwardById(id);
        award.setName(awardDetails.getName());
        award.setDescription(awardDetails.getDescription());
        award.setPoints(awardDetails.getPoints());
        award.setType(awardDetails.getType());
        return awardRepository.save(award);
    }

    public void deleteAward(Long id) {
        awardRepository.deleteById(id);
    }

    // LÓGICA DE ATRIBUIÇÃO DE PONTOS --------------------------------------
    public StudentAward assignAwardToStudent(Long awardId, Long studentId) {

        Award award = getAwardById(awardId);

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        if (!"STUDENT".equals(student.getRole())) {
            throw new RuntimeException("Awards can only be assigned to students");
        }

        StudentAward sa = new StudentAward();
        sa.setAward(award);
        sa.setStudent(student);
        sa.setPointsEarned(award.getPoints());

        return studentAwardRepository.save(sa);
    }

    // CÁLCULO DA PONTUAÇÃO GLOBAL -----------------------------------------
    public int calculateTotalPoints(Long studentId) {

        List<StudentAward> awards = studentAwardRepository.findAllByStudentId(studentId);

        return awards.stream()
                .mapToInt(StudentAward::getPointsEarned)
                .sum();
    }

}
