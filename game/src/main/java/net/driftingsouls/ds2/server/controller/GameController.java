package net.driftingsouls.ds2.server.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class GameController {
    @RequestMapping("/overview")
    public String overview() {
        return "game/overview";
    }
}
