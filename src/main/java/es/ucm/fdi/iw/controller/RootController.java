package es.ucm.fdi.iw.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * Non-authenticated requests only.
 */
@Controller
public class RootController {

    private static final Logger log = LogManager.getLogger(RootController.class);

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
    public String game(Model model) {
        return "game";
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
            //TODO Aqui la creacion del usuario, creo que hay q usar los @NamedQueries, no se si hay un DAO que lo gestiona, mirare los apuntes
            
            
            return "redirect:/login?success"; 
        } catch (Exception e) {
            // 3. Manejo de errores (ej. el usuario ya existe)
            model.addAttribute("error", "El nombre de usuario ya está en uso o algo más ha ido mal.");
            return "register";
        }
    }
}

