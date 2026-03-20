package com.hcn.controller;

import com.hcn.v4.ActivePrimeIndex;
import com.hcn.v4.FixedPowerGroup;
import com.hcn.v4.HcnBody;
import com.hcn.v4.Matrix;
import com.hcn.v4.ScientificNumber;
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
public class MatrixV4Controller {
    private Matrix matrix = new Matrix();
    
    @GetMapping("/v4")
    public String index(Model model) {
        model.addAttribute("matrix", matrix);
        model.addAttribute("displayDecimals", ScientificNumber.getDisplayDecimals());
        return "indexV4";
    }
    
    @PostMapping("/v4/proveUntilIndex")
    public String proveUntilIndex(@RequestParam int primeIndex) {
        matrix.proveUntilPrimeIndex(primeIndex);
        return "redirect:/v4";
    }
    
    @PostMapping("/v4/proveNext")
    public String proveNext() {
        matrix.proveNextHcn();
        return "redirect:/v4";
    }
    
    @PostMapping("/v4/proveUntilCount")
    public String proveUntilCount(@RequestParam int count) {
        matrix.proveUntilCount(count);
        return "redirect:/v4";
    }
    
    @PostMapping("/v4/reset")
    public String reset() {
        matrix = new Matrix();
        return "redirect:/v4";
    }
    
    @PostMapping("/v4/setDisplayDecimals")
    public String setDisplayDecimals(@RequestParam int decimals) {
        ScientificNumber.setDisplayDecimals(decimals);
        return "redirect:/v4";
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
    
    public String getPrimeRangeDisplay(FixedPowerGroup fpg) {
        List<ActivePrimeIndex> group = fpg.getFixedPowerGroup();
        if (group.isEmpty()) return "";
        int first = group.get(0).getIndex();
        int last = group.get(group.size() - 1).getIndex();
        return first == last ? String.valueOf(first) : first + "-" + last;
    }

    public int getTotalActiveOffspring(com.hcn.v4.PrimeIndexPower pip) {
        return pip.getActiveHcnBodies().stream()
                .mapToInt(body -> body.getOffsprings().size())
                .sum();
    }
}
