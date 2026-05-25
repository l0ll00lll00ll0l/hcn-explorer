package com.hcn.controller;

import com.hcn.db.SaveProgress;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class SaveProgressController {

    @Autowired
    private SaveProgress saveProgress;

    @GetMapping("/api/save-progress")
    public Map<String, Object> getProgress() {
        return Map.of(
            "active", saveProgress.isActive(),
            "phase", saveProgress.getPhase(),
            "phaseNumber", saveProgress.getPhaseNumber(),
            "totalPhases", saveProgress.getTotalPhases(),
            "itemsDone", saveProgress.getItemsDone(),
            "itemsTotal", saveProgress.getItemsTotal(),
            "percentage", saveProgress.getPercentage(),
            "error", saveProgress.getError() != null ? saveProgress.getError() : ""
        );
    }
}
