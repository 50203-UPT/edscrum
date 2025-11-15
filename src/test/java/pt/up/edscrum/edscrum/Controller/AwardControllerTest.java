package pt.up.edscrum.edscrum.Controller;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import pt.up.edscrum.model.Award;
import pt.up.edscrum.service.AwardService;

@SpringBootTest
@Transactional
class AwardControllerTest {

    @Autowired
    private AwardService awardService;

    @Test
    void testCreateAndGetAward() {
        Award a = new Award();
        a.setName("Gold Badge");
        a.setDescription("Earned for excellence");
        a.setPoints(100);
        a.setType("MANUAL");

        Award saved = awardService.createAward(a);

        assertNotNull(saved.getId());
        assertEquals("Gold Badge", saved.getName());

        Award found = awardService.getAwardById(saved.getId());
        assertEquals(saved.getName(), found.getName());
    }

    @Test
    void testGetAllAwards() {
        Award a1 = new Award();
        a1.setName("Award 1");
        a1.setPoints(10);
        a1.setType("MANUAL");
        awardService.createAward(a1);

        Award a2 = new Award();
        a2.setName("Award 2");
        a2.setPoints(20);
        a2.setType("AUTOMATIC");
        awardService.createAward(a2);

        List<Award> awards = awardService.getAllAwards();
        assertTrue(awards.size() >= 2);
    }

    @Test
    void testUpdateAward() {
        Award a = new Award();
        a.setName("Old Name");
        a.setDescription("Old Desc");
        a.setPoints(5);
        a.setType("MANUAL");
        Award saved = awardService.createAward(a);

        Award update = new Award();
        update.setName("New Award Name");
        update.setDescription("Updated Description");
        update.setPoints(50);
        update.setType("AUTOMATIC");

        Award updated = awardService.updateAward(saved.getId(), update);

        assertEquals("New Award Name", updated.getName());
        assertEquals("Updated Description", updated.getDescription());
        assertEquals(50, updated.getPoints());
        assertEquals("AUTOMATIC", updated.getType());
    }

    @Test
    void testDeleteAward() {
        Award a = new Award();
        a.setName("To be deleted");
        a.setType("MANUAL");
        Award saved = awardService.createAward(a);

        awardService.deleteAward(saved.getId());

        assertThrows(RuntimeException.class, () -> awardService.getAwardById(saved.getId()));
    }
}
