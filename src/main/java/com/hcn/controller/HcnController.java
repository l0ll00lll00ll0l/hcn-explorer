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
@SessionAttributes({"matrix", "displayFormat", "showValue", "showFactor"})
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
    
    @GetMapping("/")
    public String index(Model model) {
        return "index";
    }
    
    @PostMapping("/proveHcns")
    public String proveHcns(@ModelAttribute("matrix") Matrix matrix, @RequestParam(defaultValue = "100") int count) {
        matrix.proveMultipleHcns(count);
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
            Model model) {
        model.addAttribute("showValue", showValue != null);
        model.addAttribute("showFactor", showFactor != null);
        return "redirect:/";
    }
}
