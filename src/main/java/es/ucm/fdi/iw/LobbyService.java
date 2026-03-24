package es.ucm.fdi.iw;

import es.ucm.fdi.iw.model.Game;
import es.ucm.fdi.iw.model.User;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LobbyService {

    private List<Game> mockGames = new ArrayList<>();

    public LobbyService() {
        // Mock de usuario Host
        User host = new User();
        host.setUsername("MasterCard99");

        // Escenario Éxito: ABC123 [cite: 3, 4]
        Game successGame = new Game();
        successGame.setCode("ABC123");
        successGame.setPrivado(false);
        successGame.setHost(host);
        successGame.setEstado(Game.Estado.LOBBY);
        successGame.getPlayers().add(host);
        mockGames.add(successGame);

        // Escenario Sala Llena: LLENA99 [cite: 6]
        Game fullGame = new Game();
        fullGame.setCode("LLENA99");
        fullGame.setPrivado(false);
        fullGame.setHost(host);
        fullGame.setEstado(Game.Estado.LOBBY);
        // Llenamos la sala con 4 usuarios (suponiendo que 4 es el máximo)
        for (int i = 0; i < 4; i++) fullGame.getPlayers().add(new User());
        mockGames.add(fullGame);
    }

    public List<Game> getPublicLobbies() {
        return mockGames.stream()
                .filter(g -> !g.isPrivado() && g.getEstado() == Game.Estado.LOBBY)
                .collect(Collectors.toList());
    }

    public void attemptJoin(String code, String password) throws LobbyException {
        Game game = mockGames.stream()
                .filter(g -> g.getCode().equals(code))
                .findFirst()
                .orElseThrow(() -> new LobbyException("partida no encontrada"));

        if (game.getPlayers().size() >= 4) {
            throw new LobbyException("La sala está llena");
        }

        if (game.isPrivado() && (password == null || !password.equals(game.getPassword()))) {
            throw new LobbyException("Contraseña incorrecta");
        }
    }
}