package fr.alexandredch.pinguquizz.controllers;

import fr.alexandredch.pinguquizz.config.RoleRequired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @GetMapping("/")
    @RoleRequired(roles = { "ADMIN" })
    public String home() {
        return "Hello World!";
    }
}
