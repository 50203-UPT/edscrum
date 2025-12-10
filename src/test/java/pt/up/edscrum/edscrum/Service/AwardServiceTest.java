// package pt.up.edscrum.edscrum.Service;

// import java.util.List;

// import static org.junit.jupiter.api.Assertions.assertEquals;
// import static org.junit.jupiter.api.Assertions.assertNotNull;
// import static org.junit.jupiter.api.Assertions.assertThrows;
// import static org.junit.jupiter.api.Assertions.assertTrue;
// import org.junit.jupiter.api.Test;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.test.context.SpringBootTest;
// import org.springframework.transaction.annotation.Transactional;

// import pt.up.edscrum.model.Award;
// import pt.up.edscrum.model.StudentAward;
// import pt.up.edscrum.model.User;
// import pt.up.edscrum.service.AwardService;
// import pt.up.edscrum.service.UserService;

// @SpringBootTest
// @Transactional
// class AwardServiceTest {

//     @Autowired
//     private AwardService awardService;

//     @Autowired
//     private UserService userService;

//     // ------------------- CRUD TESTS -------------------
//     @Test
//     void testCreateAndGetAward() {
//         Award award = new Award();
//         award.setName("Participação");
//         award.setDescription("Award de teste");
//         award.setPoints(10);
//         award.setType("MANUAL");

//         Award saved = awardService.createAward(award);

//         assertNotNull(saved.getId());
//         assertEquals("Participação", saved.getName());

//         Award found = awardService.getAwardById(saved.getId());
//         assertEquals(saved.getId(), found.getId());
//     }

//     @Test
//     void testGetAllAwards() {
//         Award a1 = new Award();
//         a1.setName("A1");
//         a1.setPoints(5);
//         a1.setType("MANUAL");
//         awardService.createAward(a1);

//         Award a2 = new Award();
//         a2.setName("A2");
//         a2.setPoints(10);
//         a2.setType("AUTOMATIC");
//         awardService.createAward(a2);

//         List<Award> awards = awardService.getAllAwards();

//         assertTrue(awards.size() >= 2);
//     }

//     @Test
//     void testUpdateAward() {
//         Award award = new Award();
//         award.setName("Original");
//         award.setDescription("Desc");
//         award.setPoints(10);
//         award.setType("MANUAL");

//         Award saved = awardService.createAward(award);

//         Award update = new Award();
//         update.setName("Alterado");
//         update.setDescription("Nova desc");
//         update.setPoints(50);
//         update.setType("AUTOMATIC");

//         Award updated = awardService.updateAward(saved.getId(), update);

//         assertEquals("Alterado", updated.getName());
//         assertEquals(50, updated.getPoints());
//         assertEquals("AUTOMATIC", updated.getType());
//     }

//     @Test
//     void testDeleteAward() {
//         Award award = new Award();
//         award.setName("Para apagar");
//         award.setPoints(5);
//         award.setType("MANUAL");

//         Award saved = awardService.createAward(award);

//         awardService.deleteAward(saved.getId());

//         assertThrows(RuntimeException.class, () -> awardService.getAwardById(saved.getId()));
//     }

//     // ------------------- ASSIGN AWARD TO STUDENT -------------------
//     @Test
//     void testAssignAwardToStudent() {
//         // Criar student
//         User student = new User();
//         student.setName("Pedro");
//         student.setEmail("pedro@mail.com");
//         student.setRole("STUDENT");
//         User savedStudent = userService.createUser(student);

//         // Criar award
//         Award award = new Award();
//         award.setName("Commit");
//         award.setDescription("Fez primeiro commit");
//         award.setPoints(50);
//         award.setType("AUTOMATIC");
//         Award savedAward = awardService.createAward(award);

//         // Atribuir award
//         StudentAward sa = awardService.assignAwardToStudent(savedAward.getId(), savedStudent.getId());

//         assertNotNull(sa.getId());
//         assertEquals(50, sa.getPointsEarned());
//         assertEquals(savedStudent.getId(), sa.getStudent().getId());
//         assertEquals(savedAward.getId(), sa.getAward().getId());
//     }

//     @Test
//     void testAssignAwardToNonStudentThrowsError() {
//         User teacher = new User();
//         teacher.setName("Carlos");
//         teacher.setEmail("carlos@mail.com");
//         teacher.setRole("TEACHER");

//         User savedTeacher = userService.createUser(teacher);

//         Award award = new Award();
//         award.setName("Award X");
//         award.setPoints(10);
//         award.setType("MANUAL");

//         Award savedAward = awardService.createAward(award);

//         assertThrows(RuntimeException.class,
//                 () -> awardService.assignAwardToStudent(savedAward.getId(), savedTeacher.getId()));
//     }

//     // ------------------- TOTAL POINTS -------------------
//     @Test
//     void testCalculateTotalPoints() {
//         // Criar student
//         User student = new User();
//         student.setName("Maria");
//         student.setEmail("maria@mail.com");
//         student.setRole("STUDENT");

//         User savedStudent = userService.createUser(student);

//         // award 1
//         Award a1 = new Award();
//         a1.setName("A1");
//         a1.setPoints(10);
//         a1.setType("MANUAL");
//         Award sa1 = awardService.createAward(a1);

//         // award 2
//         Award a2 = new Award();
//         a2.setName("A2");
//         a2.setPoints(40);
//         a2.setType("AUTOMATIC");
//         Award sa2 = awardService.createAward(a2);

//         // Atribuir 2 awards
//         awardService.assignAwardToStudent(sa1.getId(), savedStudent.getId());
//         awardService.assignAwardToStudent(sa2.getId(), savedStudent.getId());

//         int total = awardService.calculateTotalPoints(savedStudent.getId());

//         assertEquals(50, total);
//     }
// }
