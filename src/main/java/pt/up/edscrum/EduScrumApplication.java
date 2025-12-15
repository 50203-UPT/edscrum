package pt.up.edscrum;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import pt.up.edscrum.model.Award;
import pt.up.edscrum.repository.AwardRepository;

@SpringBootApplication
/**
 * Entrada da aplicação Spring Boot EduScrum e rotina de seed inicial de
 * prémios por defeito.
 */
public class EduScrumApplication {

    public static void main(String[] args) {
        SpringApplication.run(EduScrumApplication.class, args);
    }

    @Bean
    public CommandLineRunner seedAwards(AwardRepository awardRepository) {
        return args -> {
            
            List<Award> defaults = List.of(
                    createAwardObj("Primeiro Salto", "Criaste o teu primeiro sprint.", 20, "AUTOMATIC", "INDIVIDUAL"),
                    createAwardObj("Sprint Artisan (5)", "Criaste 5 sprints.", 40, "AUTOMATIC", "INDIVIDUAL"),
                    createAwardObj("Sprint Veteran (10)", "Criaste 10 sprints.", 90, "AUTOMATIC", "INDIVIDUAL"),
                    createAwardObj("Conquistador de Projetos", "Concluíste o teu primeiro projeto.", 100, "AUTOMATIC", "INDIVIDUAL"),
                    createAwardObj("Primeiro Prémio", "Recebeste o teu primeiro prémio.", 10, "AUTOMATIC", "INDIVIDUAL"),
                    createAwardObj("Explorador de Cursos", "Inscreveste-te em 3 cursos diferentes.", 40, "AUTOMATIC", "INDIVIDUAL"),
                    createAwardObj("Arquiteto de Equipas", "Formaste a tua primeira equipa.", 30, "AUTOMATIC", "INDIVIDUAL"),
                    createAwardObj("Líder de Projeto (PO)", "Assumiste o papel de Product Owner num projeto.", 80, "AUTOMATIC", "INDIVIDUAL"),
                    createAwardObj("Líder de Projeto (SM)", "Assumiste o papel de Scrum Master num projeto.", 80, "AUTOMATIC", "INDIVIDUAL"),
                    createAwardObj("Colaborador Estelar", "Participaste activamente em 3 projetos diferentes.", 70, "AUTOMATIC", "INDIVIDUAL"),
                    createAwardObj("Estrela da Turma (Top 5)", "Entraste no Top 5 do ranking global.", 50, "AUTOMATIC", "INDIVIDUAL"),
                    createAwardObj("Mestre do Podium (Top 3)", "Chegaste ao Top 3 do ranking global.", 120, "AUTOMATIC", "INDIVIDUAL"),
                    createAwardObj("Bateu a Média da Turma", "Obtiveste pontos acima da média da turma.", 50, "AUTOMATIC", "INDIVIDUAL"), 
                    createAwardObj("Equipa Formada", "A tua equipa foi formada.", 30, "AUTOMATIC", "TEAM"), 
                    createAwardObj("Conquistadores de Projeto", "A equipa concluiu um projeto.", 150, "AUTOMATIC", "TEAM")
            );

            for (Award a : defaults) {
                awardRepository.findByName(a.getName()).orElseGet(() -> awardRepository.save(a));
            }
        };
    }

    private static Award createAwardObj(String name, String desc, int points, String type, String target) {
        Award a = new Award();
        a.setName(name);
        a.setDescription(desc);
        a.setPoints(points);
        a.setType(type);
        a.setTargetType(target);
        return a;
    }

}
