package com.hcn.controller;

import com.hcn.v3.ActivePrimeIndex;
import com.hcn.v3.HcnBody;
import com.hcn.v3.Matrix;
import com.hcn.v3.ScientificNumber;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Controller
public class MatrixV3Controller {
    private Matrix matrix = new Matrix();
    
    @GetMapping("/v3")
    public String index(Model model) {
        model.addAttribute("matrix", matrix);
        model.addAttribute("displayDecimals", ScientificNumber.getDisplayDecimals());
        return "indexV3";
    }
    
    @PostMapping("/v3/proveNext")
    public String proveNext() {
        matrix.proveNextHcn();
        return "redirect:/v3";
    }
    
    @PostMapping("/v3/setDisplayDecimals")
    public String setDisplayDecimals(@RequestParam int decimals) {
        ScientificNumber.setDisplayDecimals(decimals);
        return "redirect:/v3";
    }
    
    public List<ActivePrimeIndex> buildPrimeIndexList(ActivePrimeIndex lastActivePrimeIndex) {
        List<ActivePrimeIndex> list = new ArrayList<>();
        ActivePrimeIndex current = lastActivePrimeIndex;
        while (current != null) {
            list.add(current);
            current = current.getParentActivePrimeIndex();
        }
        Collections.reverse(list);
        return list;
    }
    
    public List<HcnBody> getAggregatedBodies(ActivePrimeIndex pi) {
        return Stream.of(
                pi.getNeverActivatedBodyList().stream(),
                pi.getDeactivatedBodyList().stream(),
                pi.getHcnBodyList().stream()
        )
        .flatMap(s -> s)
        .sorted()
        .collect(Collectors.toList());
    }
    
    public int getTotalActiveOffspring(com.hcn.v3.PrimeIndexPower pip) {
        return pip.getActiveHcnBodies().stream()
                .mapToInt(body -> body.getOffspring().size())
                .sum();
    }
}
