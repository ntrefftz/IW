package es.ucm.fdi.iw;

import es.ucm.fdi.iw.model.Game;
import es.ucm.fdi.iw.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Optional;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
public class LobbyService {

    private static final int MAX_PLAYERS = 4;
    private static final String MODE_ALL = "ALL";
    private static final List<String> AVAILABLE_MODES = List.of("UNO");

    private final List<Game> mockGames = new ArrayList<>();
    @Autowired
    private GameRepository gameRepository;

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
        gameRepository.save(successGame);
        
        Game fullGame = new Game();
        fullGame.setCode("LLENA99");
        fullGame.setPrivado(false);
        fullGame.setHost(host);
        fullGame.setEstado(Game.Estado.LOBBY);
        
        for (int i = 0; i < MAX_PLAYERS; i++) {
            fullGame.getPlayers().add(new User());
        }
        mockGames.add(successGame);
    }

    public List<Game> getPublicLobbies() {
        return getPublicLobbies(null, MODE_ALL);
    }

    public List<Game> getPublicLobbies(String query, String mode) {
        String normalizedQuery = normalizeSearchQuery(query);
        String normalizedMode = normalizeMode(mode);

        return gameRepository.findAll().stream()
                .filter(this::isPublicLobby)
                .filter(g -> matchesSearch(g, normalizedQuery))
                .filter(g -> matchesMode(g, normalizedMode))
                .collect(Collectors.toList());
    }

    public List<String> getAvailableModes() {
        return AVAILABLE_MODES;
    }

    public List<Game> getLobbies() {
        return gameRepository.findAll();
    }

    public String createGame(User host) {
        Game newGame = new Game();
        String randomCode = generateUniqueCode();

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
        String normalizedCode = normalizeCode(code);
        return mockGames.stream()
                .filter(g -> g.getCode().equals(normalizedCode))
                .findFirst()
                .orElseThrow(() -> new LobbyException("Partida no encontrada"));
    }

    public void attemptJoin(String code, String password, User user) throws LobbyException {
        Game game = getLobbyByCode(code);

        if (user == null) {
            throw new LobbyException("Debes iniciar sesion para unirte a la sala");
        }

        if (game.getEstado() != Game.Estado.LOBBY) {
            throw new LobbyException("La partida ya ha comenzado");
        }

        if (game.getPlayers().stream().anyMatch(p -> sameUser(p, user))) {
            return;
        }

        if (game.getPlayers().size() >= MAX_PLAYERS) {
            throw new LobbyException("La sala esta llena");
        }

        if (requiresPassword(game) && (password == null || !password.equals(game.getPassword()))) {
            throw new LobbyException("Contrasena incorrecta");
        }

        game.getPlayers().add(user);
    }

    public Game joinRandomPublicLobby(User user) {
        if (user == null) {
            throw new LobbyException("Debes iniciar sesion para unirte a la sala");
        }

        List<Game> candidates = mockGames.stream()
                .filter(this::isJoinablePublicLobby)
                .collect(Collectors.toList());

        if (candidates.isEmpty()) {
            throw new LobbyException("No hay partidas publicas disponibles ahora mismo");
        }

        Game selected = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
        attemptJoin(selected.getCode(), null, user);
        return selected;
    }

    public boolean isOwner(Game game, User user) {
        return game != null && user != null && sameUser(game.getHost(), user);
    }

    public boolean isMember(Game game, User user) {
        if (game == null || user == null) {
            return false;
        }
        return game.getPlayers().stream().anyMatch(p -> sameUser(p, user));
    }

    public void updateLobbySettings(String code, User user, String modalidad, boolean privado) {
        Game game = getLobbyByCode(code);

        if (!isOwner(game, user)) {
            throw new LobbyException("Solo el owner puede modificar la configuracion");
        }

        if (game.getEstado() != Game.Estado.LOBBY) {
            throw new LobbyException("No se puede cambiar la configuracion con la partida iniciada");
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
        Game game = getLobbyByCode(code);

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

        if (game.getEstado() != Game.Estado.LOBBY) {
            throw new LobbyException("La partida no esta en estado de lobby");
        }

        if (game.getPlayers().size() < 2) {
            throw new LobbyException("Se necesitan al menos 2 jugadores para comenzar");
        }

        game.setEstado(Game.Estado.PARTIDA);
    }

    private String generateUniqueCode() {
        while (true) {
            String candidate = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase(Locale.ROOT);
            boolean exists = false;
            for (Game game : mockGames) {
                if (candidate.equals(game.getCode())) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                return candidate;
            }
        }
    }

    private String normalizeCode(String code) {
        if (code == null) {
            throw new LobbyException("Debes indicar un codigo de partida");
        }

        String normalized = code.trim().toUpperCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            throw new LobbyException("Debes indicar un codigo de partida");
        }
        return normalized;
    }

    private String normalizeSearchQuery(String query) {
        if (query == null) {
            return "";
        }
        return query.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeMode(String mode) {
        if (mode == null || mode.isBlank()) {
            return MODE_ALL;
        }
        String normalized = mode.trim().toUpperCase(Locale.ROOT);
        if (MODE_ALL.equals(normalized) || AVAILABLE_MODES.contains(normalized)) {
            return normalized;
        }
        return MODE_ALL;
    }

    private boolean isPublicLobby(Game game) {
        return game != null && !game.isPrivado() && game.getEstado() == Game.Estado.LOBBY;
    }

    private boolean isJoinablePublicLobby(Game game) {
        return isPublicLobby(game) && game.getPlayerCount() < MAX_PLAYERS;
    }

    private boolean matchesSearch(Game game, String query) {
        if (query.isEmpty()) {
            return true;
        }

        String code = game.getCode() != null ? game.getCode().toLowerCase(Locale.ROOT) : "";
        String host = game.getHost() != null && game.getHost().getUsername() != null
                ? game.getHost().getUsername().toLowerCase(Locale.ROOT): "";

        return host.equals(query);
    }

    private boolean matchesMode(Game game, String mode) {
        if (MODE_ALL.equals(mode)) {
            return true;
        }

        String gameMode = game.getModalidad() != null ? game.getModalidad().toUpperCase(Locale.ROOT) : "";
        return gameMode.equals(mode);
    }

    private boolean requiresPassword(Game game) {
        return game.isPrivado() && game.getPassword() != null && !game.getPassword().isBlank();
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


public interface GameRepository extends JpaRepository<Game, Long> {
    Optional<Game> findByCode(String code);
}