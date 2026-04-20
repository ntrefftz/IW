package es.ucm.fdi.iw.controller;

import java.time.OffsetDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import es.ucm.fdi.iw.LobbyException;
import es.ucm.fdi.iw.LobbyService;
import es.ucm.fdi.iw.model.Game;
import es.ucm.fdi.iw.model.User;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/lobbies")
public class LobbyApiController {

    @Autowired
    private LobbyService lobbyService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @PostMapping("/{code}/chat")
    public ObjectNode chat(@PathVariable String code,
            @RequestBody ObjectNode payload,
            HttpSession session,
            HttpServletResponse response) {
        try {
            User currentUser = (User) session.getAttribute("u");
            if (currentUser == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return error("NOT_AUTHENTICATED", "Debes iniciar sesion");
            }

            Game lobby = lobbyService.getLobbyByCode(code);
            if (!lobbyService.isMember(lobby, currentUser)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return error("NOT_IN_LOBBY", "No perteneces a este lobby");
            }

            if (currentUser.isChatBan()) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return error("CHAT_BANNED", "No tienes permiso para usar el chat");
            }

            String text = payload.path("message").asText("").trim();
            if (text.isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return error("EMPTY_MESSAGE", "No puedes enviar un mensaje vacio");
            }
            if (text.length() > 400) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return error("MESSAGE_TOO_LONG", "El mensaje supera los 400 caracteres");
            }

            ObjectNode msg = objectMapper.createObjectNode();
            msg.put("type", "LOBBY_CHAT_MESSAGE");
            msg.put("code", code);
            msg.put("from", currentUser.getUsername());
            msg.put("message", text);
            msg.put("sentAt", OffsetDateTime.now().toString());

            messagingTemplate.convertAndSend("/topic/lobby/" + code, msg);
            return msg;
        } catch (LobbyException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return error("INVALID_LOBBY", e.getMessage());
        }
    }

    private ObjectNode error(String code, String message) {
        ObjectNode err = objectMapper.createObjectNode();
        err.put("type", "LOBBY_CHAT_REJECTED");
        err.put("code", code);
        err.put("message", message);
        return err;
    }
}
