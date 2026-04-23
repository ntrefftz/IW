package es.ucm.fdi.iw;
import es.ucm.fdi.iw.model.*;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameRepository extends JpaRepository<Game, Long> {
    Optional<Game> findByCode(String code);
}