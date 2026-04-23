package es.ucm.fdi.iw;
import es.ucm.fdi.iw.model.Game.*;
import java.util.Optional;


public interface GameRepository extends JpaRepository<Game, Long> {
    Optional<Game> findByCode(String code);
}