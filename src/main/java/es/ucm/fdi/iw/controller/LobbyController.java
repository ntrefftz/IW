package es.ucm.fdi.iw.controller;

import es.ucm.fdi.iw.LobbyException;
import es.ucm.fdi.iw.LobbyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class LobbyController {

    @Autowired
    private LobbyService lobbyService;

    @GetMapping("/lobby-select")
    public String showSelector(Model model) {
        model.addAttribute("publicLobbies", lobbyService.getPublicLobbies());
        return "lobby-select";
    }

    @PostMapping("/lobbies/join")
    public String joinLobby(@RequestParam String code, 
                            @RequestParam(required = false) String password, 
                            RedirectAttributes ra) {
        try {
            lobbyService.attemptJoin(code, password);
            return "redirect:/lobby"; 
        } catch (LobbyException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/lobby-select";
        }
    }

    @GetMapping("/lobby")
    public String showLobby() {
        return "lobby";
    }
}