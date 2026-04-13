package es.ucm.fdi.iw.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import es.ucm.fdi.iw.LobbyException;
import es.ucm.fdi.iw.LobbyService;
import es.ucm.fdi.iw.model.Game;
import es.ucm.fdi.iw.model.User;

/**
 * Non-authenticated requests only.
 */

@Controller
public class RootController {

    private static final Logger log = LogManager.getLogger(RootController.class);

    //Acceso a la DB
    @Autowired
    private EntityManager entityManager;

    //Hasheador de contraseñas 
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private LobbyService lobbyService;

    @ModelAttribute
    public void populateModel(HttpSession session, Model model) {
        for (String name : new String[] { "u", "url", "ws", "topics" }) {
            model.addAttribute(name, session.getAttribute(name));
        }
    }

    @GetMapping("/login")
    public String login(Model model, HttpServletRequest request) {
        boolean error = request.getQueryString() != null && request.getQueryString().indexOf("error") != -1;
        model.addAttribute("loginError", error);
        return "login";
    }

    @GetMapping("/")
    public String index(Model model) {
        return "index";
    }

    @GetMapping("/about")
    public String about(Model model) {
        return "about";
    }
    @GetMapping("/game")
    public String game(@RequestParam(required = false) String code,
            HttpSession session,
            Model model,
            RedirectAttributes ra) {
        String lobbyCode = code != null ? code : (String) session.getAttribute("currentLobbyCode");
        if (lobbyCode == null || lobbyCode.isBlank()) {
            ra.addFlashAttribute("error", "No hay partida activa");
            return "redirect:/lobby-select";
        }

        try {
            Game game = lobbyService.getLobbyByCode(lobbyCode);
            model.addAttribute("lobbyCode", lobbyCode);
            model.addAttribute("playerNames", game.getPlayers().stream().map(User::getUsername).toList());
            return "game";
        } catch (LobbyException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/lobby-select";
        }
    }
    @GetMapping("/profile")
    public String profile(Model model) {
        return "profile";
    }

    @GetMapping("/register")
    public String register(Model model) {
        return "register";
    }

    @PostMapping("/register")
    @Transactional
    public String registerUser(
            @RequestParam String username, 
            @RequestParam String password, 
            @RequestParam String confirmPassword, 
            Model model) {

        // seria mas comodo para el usuario que esto tb se comprobase en frontend, pero con esto basta :P
        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Las contraseñas no coinciden.");
            return "register";
        }

        try {
            User u = new User();
            //nº de usuarios con mismo nombre. Si intento persisitr el usuario sin comprobarlo me lanza un error incapturable
            long n = entityManager.createNamedQuery("User.hasUsername", Long.class)
                    .setParameter("username", username)
                    .getSingleResult(); 
            if (n > 0) {
                model.addAttribute("error", "El nombre de usuario '" + username + "' ya está en uso.");
                return "register";
            }
            u.setEnabled(true);
            u.setNumDerrotas(0);
            u.setNumVictorias(0);
            u.setRoles("USER");
            u.setPassword(passwordEncoder.encode(password));
            u.setUsername(username);
            entityManager.persist(u);

            return "redirect:/login"; 
        } catch (Exception e) {
            model.addAttribute("error", "Error interno del servidor, contacte con un administrador." );
            return "register";
        }
    }
}

