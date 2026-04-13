package es.ucm.fdi.iw.controller;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
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

                for (User p : game.getPlayers()) {
                    ObjectNode view = unoService.generatePlayerView(game, p, objectMapper);
                    messagingTemplate.convertAndSendToUser(p.getUsername(), "/queue/updates", view);
                }

                requesterView = unoService.generatePlayerView(game, player, objectMapper);
            }

            return requesterView;
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
}
