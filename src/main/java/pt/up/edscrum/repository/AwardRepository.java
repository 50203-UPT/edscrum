package pt.up.edscrum.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pt.up.edscrum.model.Award;

/**
 * Repositório para valores de `Award` (prémios) e consultas auxiliares.
 */
public interface AwardRepository extends JpaRepository<Award, Long> {

    /**
     * Procura um prémio pelo seu nome.
     *
     * @param name nome do prémio
     * @return Optional contendo o `Award` se existir
     */
    Optional<Award> findByName(String name);
}
