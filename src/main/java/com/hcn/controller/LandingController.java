package com.hcn.controller;

import com.hcn.db.DatabaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class LandingController {

    @Autowired
    private DatabaseService databaseService;

    @GetMapping("/")
    public String landing(Model model) {
        boolean running = databaseService.isPostgresRunning();
        model.addAttribute("postgresRunning", running);
        model.addAttribute("databases", running ? databaseService.listHcnDatabases() : List.of());
        return "landing";
    }

    @GetMapping("/delete")
    public String deleteDatabase(@RequestParam String db) {
        databaseService.deleteDatabase(db);
        return "redirect:/";
    }
}
