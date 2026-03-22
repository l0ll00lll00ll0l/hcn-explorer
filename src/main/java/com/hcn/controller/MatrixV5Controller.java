package com.hcn.controller;

import com.hcn.v5.ActivePrimeIndex;
import com.hcn.v5.FixedPowerGroup;
import com.hcn.v5.HcnBody;
import com.hcn.v5.Matrix;
import com.hcn.v5.PrimeCenter;
import com.hcn.v5.ScientificNumber;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class MatrixV5Controller {
    private Matrix matrix = new Matrix();
    
    @GetMapping("/v5")
    public String index(Model model) {
        model.addAttribute("matrix", matrix);
        model.addAttribute("displayDecimals", ScientificNumber.getDisplayDecimals());
        return "indexV5";
    }
    
    @PostMapping("/v5/proveUntilIndex")
    public String proveUntilIndex(@RequestParam int primeIndex) {
        matrix.proveUntilPrimeIndex(primeIndex);
        return "redirect:/v5";
    }
    
    @PostMapping("/v5/proveNext")
    public String proveNext() {
        matrix.proveNextHcn();
        return "redirect:/v5";
    }
    
    @PostMapping("/v5/proveUntilCount")
    public String proveUntilCount(@RequestParam int count) {
        matrix.proveUntilCount(count);
        return "redirect:/v5";
    }
    
    @PostMapping("/v5/reset")
    public String reset() {
        matrix = new Matrix();
        return "redirect:/v5";
    }
    
    @PostMapping("/v5/setDisplayDecimals")
    public String setDisplayDecimals(@RequestParam int decimals) {
        ScientificNumber.setDisplayDecimals(decimals);
        return "redirect:/v5";
    }
    
    public List<Object> buildMatrixChain(ActivePrimeIndex lastActivePrimeIndex) {
        List<Object> chain = new ArrayList<>();
        Object current = lastActivePrimeIndex;
        
        while (current != null) {
            if (current instanceof ActivePrimeIndex) {
                ActivePrimeIndex api = (ActivePrimeIndex) current;
                chain.add(api);
                if (api.getParentFixedPowerGroup() != null) {
                    current = api.getParentFixedPowerGroup();
                } else {
                    current = api.getParentActivePrimeIndex();
                }
            } else if (current instanceof FixedPowerGroup) {
                FixedPowerGroup fpg = (FixedPowerGroup) current;
                chain.add(fpg);
                current = fpg.getParentPrimeIndex();
            } else {
                current = null;
            }
        }
        
        Collections.reverse(chain);
        return chain;
    }
    
    public List<HcnBody> getActiveBodies(ActivePrimeIndex pi) {
        return pi.getHcnBodyList().stream()
                .sorted()
                .collect(Collectors.toList());
    }
    
    public String getPrimeRangeDisplay(FixedPowerGroup fpg) {
        List<ActivePrimeIndex> group = fpg.getFixedPowerGroup();
        if (group.isEmpty()) return "";
        int first = group.get(0).getIndex();
        int last = group.get(group.size() - 1).getIndex();
        return first == last ? String.valueOf(first) : first + "-" + last;
    }

    public String getPrimeValueRangeDisplay(FixedPowerGroup fpg) {
        List<ActivePrimeIndex> group = fpg.getFixedPowerGroup();
        if (group.isEmpty()) return "";
        int first = PrimeCenter.getPrime(group.get(0).getIndex());
        int last = PrimeCenter.getPrime(group.get(group.size() - 1).getIndex());
        return first == last ? String.valueOf(first) : first + "-" + last;
    }

    public int getGeneratorCount(HcnBody body) {
        if (body.getHcnFactory() != null) return 1;
        return body.getOffsprings().stream()
                .mapToInt(this::getGeneratorCount)
                .sum();
    }

    public int getTotalGeneratorCount(com.hcn.v5.PrimeIndexPower pip) {
        return pip.getActiveHcnBodies().stream()
                .mapToInt(this::getGeneratorCount)
                .sum();
    }

}
