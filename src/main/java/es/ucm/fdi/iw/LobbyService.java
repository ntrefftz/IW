package es.ucm.fdi.iw;

import es.ucm.fdi.iw.model.Game;
import es.ucm.fdi.iw.model.User;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class LobbyService {

    private List<Game> mockGames = new ArrayList<>();

    public LobbyService() {
        
        User host = new User();
        host.setUsername("MasterCard99");

        Game successGame = new Game();
        successGame.setCode("ABC123");
        successGame.setPrivado(false);
        successGame.setHost(host);
        successGame.setEstado(Game.Estado.LOBBY);
        successGame.getPlayers().add(host);
        mockGames.add(successGame);

        
        Game fullGame = new Game();
        fullGame.setCode("LLENA99");
        fullGame.setPrivado(false);
        fullGame.setHost(host);
        fullGame.setEstado(Game.Estado.LOBBY);
        
        for (int i = 0; i < 4; i++) fullGame.getPlayers().add(new User());
        mockGames.add(fullGame);
    }

    public List<Game> getPublicLobbies() {
        return mockGames.stream()
                .filter(g -> !g.isPrivado() && g.getEstado() == Game.Estado.LOBBY)
                .collect(Collectors.toList());
    }

    public String createGame(User host) {
        Game newGame = new Game();
        String randomCode = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        
        newGame.setCode(randomCode);
        newGame.setHost(host);
        newGame.setEstado(Game.Estado.LOBBY);
        newGame.setPrivado(false);
        newGame.setModalidad("UNO");
        newGame.getPlayers().add(host);
        
        mockGames.add(newGame);
        return randomCode;
    }

    public Game getLobbyByCode(String code) {
        return mockGames.stream()
                .filter(g -> g.getCode().equals(code))
                .findFirst()
                .orElseThrow(() -> new LobbyException("Partida no encontrada"));
    }

    public void attemptJoin(String code, String password, User user) throws LobbyException {
        Game game = getLobbyByCode(code);

        if (user == null) {
            throw new LobbyException("Debes iniciar sesión para unirte a la sala");
        }

        if (game.getPlayers().stream().anyMatch(p -> sameUser(p, user))) {
            return;
        }

        if (game.getPlayers().size() >= 4) {
            throw new LobbyException("La sala está llena");
        }

        if (game.isPrivado() && (password == null || !password.equals(game.getPassword()))) {
            throw new LobbyException("Contraseña incorrecta");
        }

        game.getPlayers().add(user);
    }

    public boolean isOwner(Game game, User user) {
        return game != null && user != null && sameUser(game.getHost(), user);
    }

    public void updateLobbySettings(String code, User user, String modalidad, boolean privado) {
        Game game = getLobbyByCode(code);

        if (!isOwner(game, user)) {
            throw new LobbyException("Solo el owner puede modificar la configuración");
        }

        game.setModalidad(modalidad);
        game.setPrivado(privado);
        if (!privado) {
            game.setPassword(null);
        }
    }

    public void leaveLobby(String code, User user) {
        Game game = getLobbyByCode(code);

        if (user == null) {
            throw new LobbyException("Usuario no identificado");
        }

        game.getPlayers().removeIf(p -> sameUser(p, user));

        if (isOwner(game, user)) {
            if (game.getPlayers().isEmpty()) {
                mockGames.remove(game);
            } else {
                game.setHost(game.getPlayers().get(0));
            }
        }
    }

    public void closeLobby(String code, User user) {
        Game game = mockGames.stream()
                .filter(g -> g.getCode().equals(code))
                .findFirst()
                .orElseThrow(() -> new LobbyException("Partida no encontrada"));

        if (!isOwner(game, user)) {
            throw new LobbyException("Solo el owner puede cerrar el lobby");
        }

        mockGames.remove(game);
    }

    public void startLobby(String code, User user) {
        Game game = getLobbyByCode(code);

        if (!isOwner(game, user)) {
            throw new LobbyException("Solo el owner puede comenzar la partida");
        }

        if (game.getPlayers().size() < 2) {
            throw new LobbyException("Se necesitan al menos 2 jugadores para comenzar");
        }

        game.setEstado(Game.Estado.PARTIDA);
    }

    private boolean sameUser(User a, User b) {
        if (a == null || b == null) {
            return false;
        }

        if (a.getId() > 0 && b.getId() > 0) {
            return a.getId() == b.getId();
        }

        return a.getUsername() != null && a.getUsername().equals(b.getUsername());
    }
}