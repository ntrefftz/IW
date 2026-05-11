package es.ucm.fdi.iw.controller;

import es.ucm.fdi.iw.LobbyException;
import es.ucm.fdi.iw.LobbyService;
import es.ucm.fdi.iw.UnoService;
import es.ucm.fdi.iw.model.Game;
import es.ucm.fdi.iw.model.User;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Locale;

@Controller
public class LobbyController {

    @ModelAttribute
    public void populateModel(HttpSession session, Model model) {
        for (String name : new String[] { "u", "url", "ws", "topics" }) {
            model.addAttribute(name, session.getAttribute(name));
        }
    }

    @Autowired
    private LobbyService lobbyService;
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UnoService unoService;

    @GetMapping("/lobby-select")
    public String showSelector(@RequestParam(required = false, name = "q") String query,
                               @RequestParam(required = false, defaultValue = "ALL") String mode,
                               Model model) {
        String normalizedMode = mode != null ? mode.trim().toUpperCase(Locale.ROOT) : "ALL";
        if (!"ALL".equals(normalizedMode) && !lobbyService.getAvailableModes().contains(normalizedMode)) {
            normalizedMode = "ALL";
        }
        model.addAttribute("publicLobbies", lobbyService.getPublicLobbies(query, normalizedMode));
        model.addAttribute("searchQuery", query != null ? query : "");
        model.addAttribute("selectedMode", normalizedMode);
        model.addAttribute("availableModes", lobbyService.getAvailableModes());
        return "lobby-select";
    }

    @PostMapping("/lobbies/join")
    public String joinLobby(@RequestParam String code, 
                            @RequestParam(required = false) String password, 
                            HttpSession session,
                            RedirectAttributes ra) {
        try {
            User user = (User) session.getAttribute("u");
            lobbyService.attemptJoin(code, password, user);
            Game lobby = lobbyService.getLobbyByCode(code);
            broadcastLobbyState(lobby);
            session.setAttribute("currentLobbyCode", lobby.getCode());
            return "redirect:/lobby?code=" + lobby.getCode();
        } catch (LobbyException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/lobby-select";
        }
    }

    @PostMapping("/lobbies/join-random")
    public String joinRandomLobby(HttpSession session, RedirectAttributes ra) {
        try {
            User user = (User) session.getAttribute("u");
            Game lobby = lobbyService.joinRandomPublicLobby(user);
            broadcastLobbyState(lobby);
            session.setAttribute("currentLobbyCode", lobby.getCode());
            return "redirect:/lobby?code=" + lobby.getCode();
        } catch (LobbyException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/lobby-select";
        }
    }

    @PostMapping("/lobbies/create")
    public String createLobby(HttpSession session) {
        User creator = (User) session.getAttribute("u");

        // Si no hay usuario (caso de prueba), creamos uno genérico
        if (creator == null) {
            creator = new User();
            creator.setUsername("Jugador_Nuevo");
        }

        String newCode = lobbyService.createGame(creator);
        broadcastLobbyState(lobbyService.getLobbyByCode(newCode));
        session.setAttribute("currentLobbyCode", newCode);

        return "redirect:/lobby?code=" + newCode;
    }

    @GetMapping("/lobby")
    public String showLobby(@RequestParam(required = false) String code,
                            HttpSession session,
                            Model model,
                            RedirectAttributes ra) {
        String lobbyCode = code != null ? code : (String) session.getAttribute("currentLobbyCode");

        if (lobbyCode == null || lobbyCode.isBlank()) {
            ra.addFlashAttribute("error", "No hay lobby seleccionado");
            return "redirect:/lobby-select";
        }

        try {
            Game lobby = lobbyService.getLobbyByCode(lobbyCode);
            String normalizedLobbyCode = lobby.getCode();

            User currentUser = (User) session.getAttribute("u");
            if (currentUser == null) {
                throw new LobbyException("Debes iniciar sesion para unirte a la sala");
            }

            if (!lobbyService.isMember(lobby, currentUser)) {
                if (lobby.isPrivado()) {
                    throw new LobbyException("Necesitas una contrasena para entrar en este lobby");
                }
                lobbyService.attemptJoin(normalizedLobbyCode, null, currentUser);
                lobby = lobbyService.getLobbyByCode(normalizedLobbyCode);
            }

            if (lobby.getEstado() == Game.Estado.PARTIDA) {
                return "redirect:/game?code=" + normalizedLobbyCode;
            }
            boolean isOwner = lobbyService.isOwner(lobby, currentUser);

            List<User> players = lobby.getPlayers().stream().limit(4).toList();

            model.addAttribute("lobby", lobby);
            model.addAttribute("players", players);
            model.addAttribute("isOwner", isOwner);
            model.addAttribute("maxPlayers", 4);
            model.addAttribute("topics", "lobby/" + normalizedLobbyCode);
            model.addAttribute("currentUsername", currentUser != null ? currentUser.getUsername() : "");
            session.setAttribute("currentLobbyCode", normalizedLobbyCode);
        } catch (LobbyException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/lobby-select";
        }

        return "lobby";
    }

    @PostMapping("/lobby/settings")
    public String updateSettings(@RequestParam String code,
                                 @RequestParam String modalidad,
                                 @RequestParam boolean privado,
                                 @RequestParam(required = false) String password,
                                 HttpSession session,
                                 RedirectAttributes ra) {
        try {
            User currentUser = (User) session.getAttribute("u");
            lobbyService.updateLobbySettings(code, currentUser, modalidad, privado, password);
            broadcastLobbyState(lobbyService.getLobbyByCode(code));
            return "redirect:/lobby?code=" + code;
        } catch (LobbyException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/lobby?code=" + code;
        }
    }

    @PostMapping("/lobby/leave")
    public String leaveLobby(@RequestParam String code,
                             HttpSession session,
                             RedirectAttributes ra) {
        try {
            User currentUser = (User) session.getAttribute("u");
            lobbyService.leaveLobby(code, currentUser);

            try {
                broadcastLobbyState(lobbyService.getLobbyByCode(code));
            } catch (LobbyException ignored) {
                // El lobby ha podido desaparecer al salir el ultimo jugador.
            }

            session.removeAttribute("currentLobbyCode");
            return "redirect:/lobby-select";
        } catch (LobbyException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/lobby?code=" + code;
        }
    }

    @PostMapping("/lobby/close")
    public String closeLobby(@RequestParam String code,
                             HttpSession session,
                             RedirectAttributes ra) {
        try {
            User currentUser = (User) session.getAttribute("u");
            lobbyService.closeLobby(code, currentUser);
            broadcastLobbyClosed(code);
            session.removeAttribute("currentLobbyCode");
            return "redirect:/lobby-select";
        } catch (LobbyException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/lobby?code=" + code;
        }
    }

    @PostMapping("/lobby/start")
    @Transactional
    public String startLobby(@RequestParam String code, HttpSession session, RedirectAttributes ra) {
        try {
            User currentUser = (User) session.getAttribute("u");
            Game game = lobbyService.getLobbyByCode(code);

            //Validaciones y cambio de estado a PARTIDA
            lobbyService.startLobby(code, currentUser);

            // Sincroniza a todos con el estado final del lobby antes de empezar.
            broadcastLobbyState(game);

            //Usamos el nuevo servicio para "montar el tablero"
            unoService.prepareGame(game);

            for (User p : game.getPlayers()) {
                // Generamos un JSON único para este usuario (sus cartas visibles, oponentes ocultos)
                ObjectNode view = unoService.generatePlayerView(game, p, objectMapper);
                
                // Enviamos el JSON privado. Spring busca la sesión WebSocket de ese username.
                messagingTemplate.convertAndSendToUser(
                    p.getUsername(), 
                    "/queue/updates", 
                    view
                );
            }

            //Notificamos por WebSocket (JSON) a todos
            ObjectNode msg = objectMapper.createObjectNode();
            msg.put("type", "GAME_START");
            msg.put("code", code);
            msg.put("redirectPath", "/game?code=" + code);
            messagingTemplate.convertAndSend("/topic/lobby/" + code, msg);

            return "redirect:/game?code=" + code;
        } catch (LobbyException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/lobby?code=" + code;
        }
    }

    private void broadcastLobbyState(Game lobby) {
        messagingTemplate.convertAndSend("/topic/lobby/" + lobby.getCode(), buildLobbyStateMessage(lobby));
    }

    public void broadcastLobbyClosed(String code) {
        ObjectNode msg = objectMapper.createObjectNode();
        msg.put("type", "LOBBY_CLOSED");
        msg.put("code", code);
        messagingTemplate.convertAndSend("/topic/lobby/" + code, msg);
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
}
