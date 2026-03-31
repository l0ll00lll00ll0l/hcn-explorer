package com.hcn.controller;

import com.hcn.v7.ActivePrimeIndex;
import com.hcn.v7.FixedPowerGroup;
import com.hcn.v7.HcnBody;
import com.hcn.v7.HcnGenerator;
import com.hcn.v7.Hcn;
import com.hcn.v7.LastActivePrimeIndexGroup;
import com.hcn.v7.PrimeCenter;
import com.hcn.v7.ScientificNumber;
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
public class MatrixV7Controller {
    private HcnGenerator hcnGenerator;

    public MatrixV7Controller() {
        hcnGenerator = new HcnGenerator();
        hcnGenerator.initialize();
    }
    
    @GetMapping("/v7")
    public String index(Model model, @RequestParam(defaultValue = "matrix") String tab,
                        @RequestParam(defaultValue = "chain") String lapiView) {
        model.addAttribute("hcnGenerator", hcnGenerator);
        model.addAttribute("displayDecimals", ScientificNumber.getDisplayDecimals());
        model.addAttribute("activeTab", tab);
        model.addAttribute("lapiView", lapiView);
        return "indexV7";
    }
    
    @PostMapping("/v7/proveNext")
    public String proveNext(@RequestParam(defaultValue = "1") int count,
                            @RequestParam(defaultValue = "matrix") String activeTab,
                            @RequestParam(defaultValue = "chain") String lapiView) {
        for (int i = 0; i < count; i++) hcnGenerator.proveNextSuperior();
        return "redirect:/v7?tab=" + activeTab + "&lapiView=" + lapiView;
    }

    @PostMapping("/v7/proveUntilLapi")
    public String proveUntilLapi(@RequestParam int lapiIndex, @RequestParam(defaultValue = "matrix") String activeTab,
                                  @RequestParam(defaultValue = "chain") String lapiView) {
        hcnGenerator.proveUntilPrimeIndex(lapiIndex);
        return "redirect:/v7?tab=" + activeTab + "&lapiView=" + lapiView;
    }
    
    @PostMapping("/v7/reset")
    public String reset(@RequestParam(defaultValue = "matrix") String activeTab,
                        @RequestParam(defaultValue = "chain") String lapiView) {
        hcnGenerator = new HcnGenerator();
        hcnGenerator.initialize();
        return "redirect:/v7?tab=" + activeTab + "&lapiView=" + lapiView;
    }
    
    @PostMapping("/v7/setDisplayDecimals")
    public String setDisplayDecimals(@RequestParam int decimals, @RequestParam(defaultValue = "matrix") String activeTab,
                                      @RequestParam(defaultValue = "chain") String lapiView) {
        ScientificNumber.setDisplayDecimals(decimals);
        return "redirect:/v7?tab=" + activeTab + "&lapiView=" + lapiView;
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

    public List<ActivePrimeIndex> getAllActivePrimeIndexes() {
        List<ActivePrimeIndex> list = new ArrayList<>();
        for (Object item : buildMatrixChain(hcnGenerator.getLastActivePrimeIndex())) {
            if (item instanceof ActivePrimeIndex) list.add((ActivePrimeIndex) item);
        }
        return list;
    }

    public List<LastActivePrimeIndexGroup> getLapiGroupsReversed() {
        List<LastActivePrimeIndexGroup> list = new ArrayList<>();
        LastActivePrimeIndexGroup current = hcnGenerator.getHighestLapiGroup();
        while (current != null) {
            list.add(current);
            current = current.getLowerLapiGroup();
        }
        return list;
    }

    public List<Hcn> getHcnsForLapiGroup(LastActivePrimeIndexGroup group) {
        List<Hcn> hcns = new ArrayList<>();
        Hcn current = group.getFirstHcn();
        while (current != null) {
            hcns.add(current);
            current = current.getLargerHcns().get(group);
        }
        return hcns;
    }

    public java.util.Map<HcnBody, Integer> getBodyOrderMap() {
        java.util.Map<HcnBody, Integer> map = new java.util.LinkedHashMap<>();
        int index = 0;
        HcnBody current = hcnGenerator.getLastActivePrimeIndex().getHcnBodyList().getSmallestBody();
        while (current != null) {
            map.put(current, index++);
            current = current.getLargerBody();
        }
        return map;
    }

}
