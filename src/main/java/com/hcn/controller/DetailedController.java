package com.hcn.controller;

import com.hcn.core.ActivePrimeIndex;
import com.hcn.core.FixedPowerGroup;
import com.hcn.core.GeneratorConfig;
import com.hcn.core.HcnBody;
import com.hcn.core.Matrix;
import com.hcn.core.Hcn;
import com.hcn.core.LastActivePrimeIndexGroup;
import com.hcn.core.PrimeCenter;
import com.hcn.core.ScientificNumber;
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
public class DetailedController {
    private Matrix matrix;
    private String dbName;

    @org.springframework.beans.factory.annotation.Autowired
    private com.hcn.db.MatrixSaveService matrixSaveService;

    @org.springframework.beans.factory.annotation.Autowired
    private com.hcn.db.SaveProgress saveProgress;

    @org.springframework.beans.factory.annotation.Autowired
    private com.hcn.db.MatrixLoadService matrixLoadService;

    @org.springframework.beans.factory.annotation.Autowired
    private com.hcn.db.IntervalStorageService intervalStorageService;

    public DetailedController() {
        matrix = new Matrix();
    }
    
    @GetMapping("/detailed")
    public String index(Model model, @RequestParam(defaultValue = "matrix") String tab,
                        @RequestParam(defaultValue = "chain") String lapiView,
                        @RequestParam(value = "new", defaultValue = "false") boolean isNew,
                        @RequestParam(required = false) String dbName,
                        @RequestParam(defaultValue = "true") boolean basicData) {
        if (isNew) {
            matrix = new Matrix();
            GeneratorConfig.reset();
            GeneratorConfig.setBasicData(basicData);
            GeneratorConfig.setIntervalStorageService(intervalStorageService);
            if (dbName != null) {
                this.dbName = dbName;
                GeneratorConfig.setDbName(dbName);
            }
        } else if (dbName != null) {
            this.dbName = dbName;
            GeneratorConfig.setDbName(dbName);
            GeneratorConfig.setIntervalStorageService(intervalStorageService);
            matrix = matrixLoadService.load(dbName);
            GeneratorConfig.lock();
        }
        if (!GeneratorConfig.isLocked()) {
            GeneratorConfig.lock();
            matrix.initialize();
        }
        model.addAttribute("matrix", matrix);
        model.addAttribute("configLocked", GeneratorConfig.isLocked());
        model.addAttribute("basicData", GeneratorConfig.isBasicData());
        model.addAttribute("displayDecimals", ScientificNumber.getDisplayDecimals());
        model.addAttribute("activeTab", tab);
        model.addAttribute("lapiView", lapiView);
        return "indexDetailed";
    }

    @PostMapping("/detailed/updateConfig")
    public String updateConfig(@RequestParam(defaultValue = "false") boolean extendedHcnBodyData,
                               @RequestParam(defaultValue = "config") String activeTab,
                               @RequestParam(defaultValue = "chain") String lapiView) {
        return "redirect:/detailed?tab=" + activeTab + "&lapiView=" + lapiView;
    }
    
    @PostMapping("/detailed/proveNext")
    public String proveNext(@RequestParam(defaultValue = "1") int count,
                            @RequestParam(defaultValue = "matrix") String activeTab,
                            @RequestParam(defaultValue = "chain") String lapiView) {
        for (int i = 0; i < count; i++) matrix.proveNextSuperior();
        return "redirect:/detailed?tab=" + activeTab + "&lapiView=" + lapiView;
    }

    @PostMapping("/detailed/proveUntilLapi")
    public String proveUntilLapi(@RequestParam int lapiIndex, @RequestParam(defaultValue = "matrix") String activeTab,
                                  @RequestParam(defaultValue = "chain") String lapiView) {
        matrix.proveLapi(lapiIndex);
        return "redirect:/detailed?tab=" + activeTab + "&lapiView=" + lapiView;
    }
    
    @PostMapping("/detailed/reset")
    public String reset(@RequestParam(defaultValue = "matrix") String activeTab,
                        @RequestParam(defaultValue = "chain") String lapiView) {
        matrix = new Matrix();
        GeneratorConfig.reset();
        return "redirect:/detailed?tab=" + activeTab + "&lapiView=" + lapiView;
    }
    
    @PostMapping("/detailed/setDisplayDecimals")
    public String setDisplayDecimals(@RequestParam int decimals, @RequestParam(defaultValue = "matrix") String activeTab,
                                      @RequestParam(defaultValue = "chain") String lapiView) {
        ScientificNumber.setDisplayDecimals(decimals);
        return "redirect:/detailed?tab=" + activeTab + "&lapiView=" + lapiView;
    }

    @PostMapping("/detailed/save")
    public String save(@RequestParam(defaultValue = "matrix") String activeTab,
                       @RequestParam(defaultValue = "chain") String lapiView) {
        saveProgress.start();
        new Thread(() -> matrixSaveService.save(matrix, dbName, "detailed")).start();
        return "redirect:/detailed?tab=" + activeTab + "&lapiView=" + lapiView;
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
        for (Object item : buildMatrixChain(matrix.getLastActivePrimeIndex())) {
            if (item instanceof ActivePrimeIndex) list.add((ActivePrimeIndex) item);
        }
        return list;
    }

    public List<LastActivePrimeIndexGroup> getLapiGroupsReversed() {
        List<LastActivePrimeIndexGroup> list = new ArrayList<>();
        LastActivePrimeIndexGroup current = matrix.getHighestLapiGroup();
        while (current != null) {
            list.add(current);
            current = current.getLowerLapiGroup();
        }
        return list;
    }

    public List<Hcn> getHcnsForLapiGroup(LastActivePrimeIndexGroup group) {
        return group.getHcnList();
    }

    public java.util.Map<HcnBody, Integer> getBodyOrderMap() {
        java.util.Map<HcnBody, Integer> map = new java.util.LinkedHashMap<>();
        int index = 0;
        HcnBody current = matrix.getLastActivePrimeIndex().getHcnBodyList().getSmallestBody();
        while (current != null) {
            map.put(current, index++);
            current = current.getLargerBody();
        }
        return map;
    }

}
