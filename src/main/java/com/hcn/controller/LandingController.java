package com.hcn.controller;

import com.hcn.db.DatabaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LandingController {

    @Autowired
    private DatabaseService databaseService;

    @GetMapping("/")
    public String landing(Model model) {
        model.addAttribute("databases", databaseService.listDatabases());
        return "landing";
    }

    @PostMapping("/start")
    public String start(@RequestParam String dbMode,
                        @RequestParam(defaultValue = "false") boolean basicData) {
        if ("withdb".equals(dbMode)) {
            String dbName = databaseService.createDatabase(basicData);
            return "redirect:/detailed?new=true&dbName=" + dbName + "&basicData=" + basicData;
        }
        String dbName = databaseService.createDatabase(false);
        return "redirect:/core?new=true&dbName=" + dbName;
    }

    @PostMapping("/delete")
    public String delete(@RequestParam String dbName) {
        databaseService.deleteDatabase(dbName);
        return "redirect:/";
    }
}
