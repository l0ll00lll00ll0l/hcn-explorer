package com.hcn.controller;

import com.hcn.v2.ActivePrimeIndex;
import com.hcn.v2.HcnBody;
import com.hcn.v2.Matrix;
import com.hcn.v2.ScientificNumber;
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
public class MatrixV2Controller {
    private Matrix matrix = new Matrix();
    
    @GetMapping("/v2")
    public String index(Model model) {
        model.addAttribute("matrix", matrix);
        model.addAttribute("displayDecimals", ScientificNumber.getDisplayDecimals());
        return "indexV2";
    }
    
    @PostMapping("/v2/proveUntilIndex")
    public String proveUntilIndex(@RequestParam int primeIndex) {
        matrix.proveUntilPrimeIndex(primeIndex);
        return "redirect:/v2";
    }
    
    @PostMapping("/v2/proveNext")
    public String proveNext() {
        matrix.proveNextHcn();
        return "redirect:/v2";
    }
    
    @PostMapping("/v2/proveUntilCount")
    public String proveUntilCount(@RequestParam int count) {
        matrix.proveUntilCount(count);
        return "redirect:/v2";
    }
    
    @PostMapping("/v2/reset")
    public String reset() {
        matrix = new Matrix();
        return "redirect:/v2";
    }
    
    @PostMapping("/v2/setDisplayDecimals")
    public String setDisplayDecimals(@RequestParam int decimals) {
        ScientificNumber.setDisplayDecimals(decimals);
        return "redirect:/v2";
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
    
    public int getTotalActiveOffspring(com.hcn.v2.PrimeIndexPower pip) {
        return pip.getActiveHcnBodies().stream()
                .mapToInt(body -> body.getOffspring().size())
                .sum();
    }
}
