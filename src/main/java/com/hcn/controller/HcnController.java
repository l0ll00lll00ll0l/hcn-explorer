package com.hcn.controller;

import com.hcn.util.Matrix;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@SessionAttributes({"matrix", "displayFormat", "showValue", "showFactor", "enableGraph", "sampleInterval", "trackNextPrime", "trackPowerExtension"})
public class HcnController {
    
    @ModelAttribute("matrix")
    public Matrix getMatrix() {
        return Matrix.initializeMatrix();
    }
    
    @ModelAttribute("displayFormat")
    public String getDisplayFormat() {
        return "full";
    }
    
    @ModelAttribute("showValue")
    public Boolean getShowValue() {
        return false;
    }
    
    @ModelAttribute("showFactor")
    public Boolean getShowFactor() {
        return false;
    }
    
    @ModelAttribute("enableGraph")
    public Boolean getEnableGraph() {
        return true;
    }
    
    @ModelAttribute("sampleInterval")
    public Integer getSampleInterval() {
        return 1;
    }
    
    @ModelAttribute("trackNextPrime")
    public Boolean getTrackNextPrime() {
        return false;
    }
    
    @ModelAttribute("trackPowerExtension")
    public Boolean getTrackPowerExtension() {
        return false;
    }
    
    @GetMapping("/")
    public String index(Model model) {
        return "index";
    }
    
    @PostMapping("/proveHcns")
    public String proveHcns(
            @ModelAttribute("matrix") Matrix matrix,
            @ModelAttribute("enableGraph") Boolean enableGraph,
            @ModelAttribute("sampleInterval") Integer sampleInterval,
            @ModelAttribute("trackNextPrime") Boolean trackNextPrime,
            @ModelAttribute("trackPowerExtension") Boolean trackPowerExtension,
            @RequestParam(defaultValue = "100") int count) {
        int interval = (enableGraph != null && enableGraph) ? sampleInterval : 0;
        boolean trackNext = trackNextPrime != null && trackNextPrime;
        boolean trackPower = trackPowerExtension != null && trackPowerExtension;
        matrix.proveMultipleHcns(count, interval, trackNext, trackPower);
        return "redirect:/";
    }
    
    @PostMapping("/reset")
    public String reset(Model model) {
        model.addAttribute("matrix", Matrix.initializeMatrix());
        return "redirect:/";
    }
    
    @PostMapping("/setDisplayFormat")
    public String setDisplayFormat(@RequestParam String format, Model model) {
        model.addAttribute("displayFormat", format);
        return "redirect:/";
    }
    
    @PostMapping("/setDisplayOptions")
    public String setDisplayOptions(
            @RequestParam(required = false) Boolean showValue,
            @RequestParam(required = false) Boolean showFactor,
            @RequestParam(required = false) Boolean enableGraph,
            @RequestParam(defaultValue = "10") Integer sampleInterval,
            @RequestParam(required = false) Boolean trackNextPrime,
            @RequestParam(required = false) Boolean trackPowerExtension,
            Model model) {
        model.addAttribute("showValue", showValue != null);
        model.addAttribute("showFactor", showFactor != null);
        model.addAttribute("enableGraph", enableGraph != null);
        model.addAttribute("sampleInterval", sampleInterval);
        model.addAttribute("trackNextPrime", trackNextPrime != null);
        model.addAttribute("trackPowerExtension", trackPowerExtension != null);
        return "redirect:/";
    }
}
