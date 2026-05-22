package com.hcn.controller;

import com.hcn.db.HcnApplication;
import com.hcn.db.HcnApplicationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LandingController {

    @Autowired
    private HcnApplicationRepository hcnApplicationRepository;

    @GetMapping("/")
    public String landing(Model model) {
        model.addAttribute("applications", hcnApplicationRepository.findAll());
        return "landing";
    }

    @PostMapping("/start")
    public String start(@RequestParam String dbMode,
                        @RequestParam(defaultValue = "false") boolean extendedHcnBodyData) {
        if ("withdb".equals(dbMode)) {
            HcnApplication app = new HcnApplication();
            app.setName("HCN-" + System.currentTimeMillis());
            hcnApplicationRepository.save(app);

            return "redirect:/detailed";
        }
        return "redirect:/core";
    }

    @PostMapping("/delete")
    public String delete(@RequestParam Long appId) {
        hcnApplicationRepository.deleteById(appId);
        return "redirect:/";
    }
}
