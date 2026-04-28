package es.ucm.fdi.iw.controller;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import es.ucm.fdi.iw.LobbyException;
import es.ucm.fdi.iw.LobbyService;
import es.ucm.fdi.iw.UnoService;
import es.ucm.fdi.iw.model.Game;
import es.ucm.fdi.iw.model.UnoActionRequest;
import es.ucm.fdi.iw.model.User;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/games")
public class UnoApiController {

    @Autowired
    private LobbyService lobbyService;

    @Autowired
    private UnoService unoService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private final ConcurrentHashMap<String, Object> gameLocks = new ConcurrentHashMap<>();

    @GetMapping("/{code}/state")
    public ObjectNode state(@PathVariable String code, HttpSession session, HttpServletResponse response) {
        try {
            User currentUser = (User) session.getAttribute("u");
            if (currentUser == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return error("NOT_AUTHENTICATED", "Debes iniciar sesion");
            }

            Game game = lobbyService.getLobbyByCode(code);
            User player = unoService.resolvePlayerInGame(game, currentUser);
            if (player == null) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return error("NOT_IN_GAME", "No perteneces a esta partida");
            }
            if (game.getEstado() != Game.Estado.PARTIDA) {
                response.setStatus(HttpServletResponse.SC_CONFLICT);
                return error("GAME_NOT_ACTIVE", "La partida no esta activa");
            }

            synchronized (lockFor(code)) {
                return unoService.generatePlayerView(game, player, objectMapper);
            }
        } catch (LobbyException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return error("INVALID_STATE", e.getMessage());
        }
    }

    @PostMapping("/{code}/action")
    public ObjectNode action(@PathVariable String code,
            @RequestBody UnoActionRequest action,
            HttpSession session,
            HttpServletResponse response) {
        try {
            User currentUser = (User) session.getAttribute("u");
            if (currentUser == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return error("NOT_AUTHENTICATED", "Debes iniciar sesion");
            }

            Game game = lobbyService.getLobbyByCode(code);
            User player = unoService.resolvePlayerInGame(game, currentUser);
            if (player == null) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return error("NOT_IN_GAME", "No perteneces a esta partida");
            }

            ObjectNode requesterView;
            synchronized (lockFor(code)) {
                unoService.applyAction(game, player, action);

                if (game.getEstado() == Game.Estado.TERMINADA && game.getWinner() != null) {
                    requesterView = broadcastGameOver(game, player);
                    unoService.resetToLobbyAfterGame(game);
                    messagingTemplate.convertAndSend("/topic/lobby/" + game.getCode(), buildLobbyStateMessage(game));
                } else {
                    for (User p : game.getPlayers()) {
                        ObjectNode view = unoService.generatePlayerView(game, p, objectMapper);
                        messagingTemplate.convertAndSendToUser(p.getUsername(), "/queue/updates", view);
                    }
                    requesterView = unoService.generatePlayerView(game, player, objectMapper);
                }
            }

            return requesterView;
        } catch (ObjectOptimisticLockingFailureException e) {
            response.setStatus(HttpServletResponse.SC_CONFLICT);
            return error("CONCURRENT_UPDATE", "La partida se actualizo en paralelo. Reintenta la accion");
        } catch (LobbyException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return error("INVALID_ACTION", e.getMessage());
        }
    }

    private Object lockFor(String code) {
        return gameLocks.computeIfAbsent(code, c -> new Object());
    }

    private ObjectNode error(String code, String message) {
        ObjectNode err = objectMapper.createObjectNode();
        err.put("type", "ACTION_REJECTED");
        err.put("code", code);
        err.put("message", message);
        return err;
    }

    private ObjectNode broadcastGameOver(Game game, User requester) {
        User winner = game.getWinner();
        String winnerUsername = winner != null && winner.getUsername() != null ? winner.getUsername() : "Alguien";
        ObjectNode requesterPayload = null;

        for (User p : game.getPlayers()) {
            boolean viewerWon = sameUser(p, winner);
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("type", "GAME_OVER");
            payload.put("code", game.getCode());
            payload.put("winnerUsername", winnerUsername);
            payload.put("isWinner", viewerWon);
            payload.put("message", viewerWon
                    ? "HAS GANADO LA PARTIDA"
                    : winnerUsername + " HA GANADO LA PARTIDA, HAS PERDIDO");
            payload.put("redirectPath", "/lobby?code=" + game.getCode());
            payload.put("autoRedirectMs", 20000);

            messagingTemplate.convertAndSendToUser(p.getUsername(), "/queue/updates", payload);
            if (sameUser(p, requester)) {
                requesterPayload = payload;
            }
        }

        return requesterPayload != null ? requesterPayload : error("GAME_OVER", "La partida ha terminado");
    }

    private ObjectNode buildLobbyStateMessage(Game lobby) {
        ObjectNode msg = objectMapper.createObjectNode();
        msg.put("type", "LOBBY_STATE_UPDATE");
        msg.put("code", lobby.getCode());
        msg.put("estado", lobby.getEstado().name());
        msg.put("modalidad", lobby.getModalidad());
        msg.put("privado", lobby.isPrivado());

        User host = lobby.getHost();
        msg.put("host", host != null ? host.getUsername() : "");

        ArrayNode playersNode = msg.putArray("players");
        for (User p : lobby.getPlayers()) {
            playersNode.add(p.getUsername() != null ? p.getUsername() : "Jugador sin nombre");
        }
        msg.put("playerCount", lobby.getPlayers().size());
        return msg;
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
