package net.driftingsouls.ds2.server.controller;

import net.driftingsouls.ds2.server.entities.Rasse;
import net.driftingsouls.ds2.server.model.requests.RegisterForm;
import net.driftingsouls.ds2.server.repositories.RaceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toList;

@Controller
public class PortalController2 {
    private final RaceRepository raceRepository;

    @Autowired
    public PortalController2(RaceRepository raceRepository) {
        this.raceRepository = raceRepository;
    }

    @GetMapping("/")
    public String portal() {
        return "portal/main";
    }

    @RequestMapping(value = "/login")
    public String login() {
        return "portal/login";
    }

    @GetMapping("/portal/agb")
    public String agb() {
        return "portal/agb";
    }

    @GetMapping("/portal/register")
    public String register(@RequestParam(required = false) String message, Model model) {
        List<Rasse> playableRaces = StreamSupport.stream(raceRepository.findAll().spliterator(), false)
                .filter(Rasse::isPlayable)
                .collect(toList());

        if(message != null) {
            model.addAttribute("message", message);
        }

        model.addAttribute("races", playableRaces);
        return "portal/register";
    }

    @PostMapping("/portal/register/selectsystem")
    public String chooseSystem(@ModelAttribute RegisterForm registerForm, RedirectAttributes redirectAttributes) {
        if(registerForm.getUsername().trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("message", "Haha, sehr witzig ... Du musst schon einen Namen eingeben");
            return "redirect:/portal/register";
        }

        if(registerForm.getEmail().isEmpty() || !registerForm.getEmail().contains("@")) {
            redirectAttributes.addFlashAttribute("message", "Was auch immer das sein soll - eine E-Mailadresse ist es nicht");
            return "redirect:/portal/register";
        }

        return "portal/register_selectsystem";
    }
}
