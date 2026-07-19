package com.hcn.controller;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.hcn.db.ActivityReadService;
import com.hcn.db.DatabaseService;
import com.hcn.db.DbInsertService;
import com.hcn.db.MatrixDeserializer;
import com.hcn.db.MatrixSerializer;
import com.hcn.event.ActivityCenter;
import com.hcn.newCore.*;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Controller
public class NewCoreController {

    private Matrix matrix;
    private String activeTab = "matrix";
    private String activeSubTab = null;
    private int activeBodyList = -1;
    private int activeMatrixNodeIdx = 0;
    private int activeBodyNodeId = -1;
    private String currentLogLevel = "INFO";

    @Autowired
    private DatabaseService databaseService;

    @Autowired
    private ActivityReadService activityReadService;

    @Autowired
    private DbInsertService dbInsertService;

    @GetMapping("/newcore")
    public String index(Model model, @RequestParam(defaultValue = "false") boolean dbMode) {
        if (matrix == null) {
            matrix = Matrix.builder().dbMode(dbMode).build();
            if (dbMode) {
                matrix.setDbInsertService(dbInsertService);
                matrix.setDbName(databaseService.assignDbName());
                if (!databaseService.databaseExists(matrix.getDbName())) {
                    databaseService.createDatabase(matrix.getDbName());
                }
                databaseService.createPermanentTables(matrix.getDbName());
                dbInsertService.setTargetDb(matrix.getDbName());
            }
            matrix.initialize();
        }
        model.addAttribute("matrix", matrix);
        model.addAttribute("logLevel", currentLogLevel);
        model.addAttribute("activeTab", activeTab);
        model.addAttribute("activeSubTab", activeSubTab);
        return "newcore";
    }

    @GetMapping("/newcore/bodynode-detail")
    public String bodynodeDetail(Model model,
                                 @RequestParam(defaultValue = "0") int matrixNodeIdx,
                                 @RequestParam(defaultValue = "-1") int bodyNodeId) {
        if (matrixNodeIdx >= 0) activeMatrixNodeIdx = matrixNodeIdx;
        if (bodyNodeId >= 0) activeBodyNodeId = bodyNodeId;
        model.addAttribute("matrix", matrix);
        List<MatrixNode> chain = getMatrixChain();
        MatrixNode node = chain.get(activeMatrixNodeIdx);
        BodyNode selected = node.getBodyNodes().get(activeBodyNodeId);
        if (selected == null) selected = node.getDeactivatedBodyNodes().get(activeBodyNodeId);
        if (selected == null && !node.getBodyNodes().isEmpty()) selected = node.getBodyNodes().firstEntry().getValue();
        model.addAttribute("selectedBodyNode", selected);
        return "tabs/bodynode-detail :: content";
    }

    @GetMapping("/newcore/tab")
    public String tab(Model model,
                      @RequestParam String tab,
                      @RequestParam(defaultValue = "") String subtab,
                      @RequestParam(defaultValue = "-1") int matrixNodeIdx,
                      @RequestParam(defaultValue = "-1") int bodyNodeId) {
        activeTab = tab;
        model.addAttribute("matrix", matrix);
        List<MatrixNode> chain = getMatrixChain();
        model.addAttribute("matrixChain", chain);

        switch (tab) {
            case "matrix" -> {}
            case "bodynodes" -> {
                activeBodyList = matrixNodeIdx == -1 ? (activeBodyList == -1 ? chain.size() - 1 : activeBodyList) : matrixNodeIdx;
                model.addAttribute("activeBodyList", activeBodyList);
            }
            case "bodynode" -> {
                if (matrixNodeIdx >= 0) activeMatrixNodeIdx = matrixNodeIdx;
                if (bodyNodeId >= 0) activeBodyNodeId = bodyNodeId;
                model.addAttribute("activeMatrixNodeIdx", activeMatrixNodeIdx);
                model.addAttribute("activeBodyNodeId", activeBodyNodeId);
                model.addAttribute("bodyNodesPerMatrixNode", getBodyNodesPerMatrixNodeJson(chain));
                MatrixNode node = chain.get(activeMatrixNodeIdx);
                BodyNode selected = node.getBodyNodes().get(activeBodyNodeId);
                if (selected == null) selected = node.getDeactivatedBodyNodes().get(activeBodyNodeId);
                if (selected == null && !node.getBodyNodes().isEmpty()) selected = node.getBodyNodes().firstEntry().getValue();
                model.addAttribute("selectedBodyNode", selected);
                model.addAttribute("activeBodyNodes", getBodyNodesForNode(node));
            }
            case "database" -> {
                activeSubTab = subtab.isEmpty() ? (activeSubTab != null ? activeSubTab : "hcn") : subtab;
                model.addAttribute("activeSubTab", activeSubTab);
                if (!subtab.isEmpty()) {
                    return "tabs/database-" + activeSubTab;
                }
            }
            case "processactivity" -> {
                String db = matrix.getDbName();
                model.addAttribute("dbMatrixMainActivities",      activityReadService.getMatrixMainActivities(db));
                model.addAttribute("dbMatrixExtensionActivities", activityReadService.getMatrixExtensionActivities(db));
                model.addAttribute("dbHcnGenerationActivities",   activityReadService.getHcnGenerationActivities(db));
                model.addAttribute("dbApiNodeCreationActivities", activityReadService.getApiNodeCreationActivities(db));
                model.addAttribute("dbTransitionNodeActivities",  activityReadService.getTransitionNodeCreationActivities(db));
                model.addAttribute("dbSqlInsertActivities",       activityReadService.getSqlInsertActivities(db));
            }
        }
        return "tabs/" + tab + " :: content";
    }

    private List<BodyNode> getBodyNodesForNode(MatrixNode node) {
        List<BodyNode> result = new ArrayList<>();
        node.getBodyNodes().descendingMap().values().forEach(result::add);
        node.getDeactivatedBodyNodes().descendingMap().values().forEach(result::add);
        return result;
    }

    private String getBodyNodesPerMatrixNodeJson(List<MatrixNode> chain) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < chain.size(); i++) {
            if (i > 0) sb.append(",");
            MatrixNode node = chain.get(i);
            sb.append("[");
            boolean first = true;
            for (var entry : node.getBodyNodes().descendingMap().entrySet()) {
                if (!first) sb.append(","); first = false;
                BodyNode bn = entry.getValue();
                sb.append("{\"id\":").append(bn.getBodyNodeId()).append(",\"deactivated\":false,\"value\":\"").append(bn.getValue()).append("\",\"factor\":\"").append(bn.getFactor()).append("\"}");
            }
            for (var entry : node.getDeactivatedBodyNodes().descendingMap().entrySet()) {
                if (!first) sb.append(","); first = false;
                BodyNode bn = entry.getValue();
                sb.append("{\"id\":").append(bn.getBodyNodeId()).append(",\"deactivated\":true,\"value\":\"").append(bn.getValue()).append("\",\"factor\":\"").append(bn.getFactor()).append("\"}");
            }
            sb.append("]");
        }
        return sb.append("]").toString();
    }

    @PostMapping("/newcore/loglevel")
    public String setLogLevel(@RequestParam String level) {
        currentLogLevel = level.toUpperCase();
        LoggerContext ctx = (LoggerContext) LoggerFactory.getILoggerFactory();
        ctx.getLogger("com.hcn.newCore").setLevel(Level.toLevel(currentLogLevel));
        return "redirect:/newcore";
    }

    @PostMapping("/newcore/save")
    public String save() {
        if (matrix != null && !ActivityCenter.isProving()) {
            if (matrix.getDbName() == null) {
                matrix.setDbName(databaseService.assignDbName());
            }
            if (!databaseService.databaseExists(matrix.getDbName())) {
                databaseService.createDatabase(matrix.getDbName());
            } else {
                databaseService.truncateTmpTables(matrix.getDbName());
            }
            MatrixSerializer serializer = new MatrixSerializer();
            serializer.assignTempIds(matrix);
            JdbcTemplate dbTemplate = databaseService.createTemplateForDb(matrix.getDbName());
            dbTemplate.execute(serializer.buildMatrixNodeInsert());
            dbTemplate.execute(serializer.buildPrimeInsert(matrix));
            dbTemplate.execute(serializer.buildLapiInsert(matrix));
            String lapiHcnInsert = serializer.buildLapiHcnInsert(matrix);
            if (lapiHcnInsert != null) dbTemplate.execute(lapiHcnInsert);
            String refIntervalHcnInsert = serializer.buildReferenceIntervalHcnInsert(matrix);
            if (refIntervalHcnInsert != null) dbTemplate.execute(refIntervalHcnInsert);
            dbTemplate.execute(serializer.buildBodyInsert());
            dbTemplate.execute(serializer.buildBodyNodeInsert());
            dbTemplate.execute(serializer.buildHcnInsert());
            dbTemplate.execute(serializer.buildMatrixInsert(matrix));
        }
        return "redirect:/newcore";
    }

    @GetMapping("/newcore/load")
    public String load(@RequestParam String db) {
        JdbcTemplate dbTemplate = databaseService.createTemplateForDb(db);
        MatrixDeserializer deserializer = new MatrixDeserializer(dbTemplate, dbInsertService);
        matrix = deserializer.load();
        matrix.setDbName(db);
        if (matrix.isDbMode()) {
            dbInsertService.setTargetDb(db);
        }
        activeTab = "matrix";
        activeSubTab = null;
        activeBodyList = -1;
        return "redirect:/newcore";
    }

    @GetMapping("/newcore/quit")
    public String quit() {
        matrix = null;
        activeTab = "matrix";
        activeSubTab = null;
        activeBodyList = -1;
        return "redirect:/";
    }

    @PostMapping("/newcore/prove")
    @ResponseBody
    public String prove(@RequestParam(defaultValue = "1") int count) {
        if (!ActivityCenter.isProving()) {
            ActivityCenter.setProving(true);
            ActivityCenter.setProveTarget(count);
            ActivityCenter.setProveProgress(0);
            new Thread(() -> matrix.proveLapi(count)).start();
        }
        return "{\"started\":true}";
    }

    @GetMapping("/newcore/progress")
    @ResponseBody
    public String progress() {
        if (matrix == null) return "{\"proving\":false,\"progress\":0,\"target\":0}";
        return "{\"proving\":" + ActivityCenter.isProving() + ",\"progress\":" + ActivityCenter.getProveProgress() + ",\"target\":" + ActivityCenter.getProveTarget() + "}";
    }

    public List<MatrixNode> getMatrixChain() {
        List<MatrixNode> chain = new ArrayList<>();
        MatrixNode current = matrix.getLastTransition();
        while (current != null) {
            chain.add(current);
            current = current.getPrevMatrixNode();
        }
        Collections.reverse(chain);
        return chain;
    }

    public List<Body> getBodies(BodyList bodyList) {
        List<Body> bodies = new ArrayList<>();
        Body current = bodyList.getSmallestBody();
        while (current != null) {
            bodies.add(current);
            current = current.getLargerBody();
        }
        return bodies;
    }

    public List<Body> getActiveBodiesChain(BodyList bodyList) {
        List<Body> bodies = new ArrayList<>();
        Body current = bodyList.getSmallestBody();
        while (current != null && current.isDeactivated()) {
            current = current.getLargerBody();
        }
        while (current != null) {
            bodies.add(current);
            current = current.getLargerHcnGenerator();
        }
        return bodies;
    }

    public String guiString(Body body) {
        if (body == null) return "";
        if (body.getBodyNode() == null) return "DELETED BODY";
        StringBuilder sb = new StringBuilder();
        buildGuiString(body, sb);
        return sb.toString();
    }

    private void buildGuiString(Body body, StringBuilder sb) {
        if (body.getParent() != null) {
            buildGuiString(body.getParent(), sb);
            sb.append(", ");
        }
        BodyNode node = body.getBodyNode();
        if (node.getParentNode() instanceof ApiNode apiNode) {
            sb.append("p").append(apiNode.getIndexes().get(0).getIndex()).append("^").append(node.getBodyNodeId());
        } else {
            sb.append("t").append(node.getBodyNodeId());
        }
    }

    public List<Lapi> getLapiChain() {
        List<Lapi> chain = new ArrayList<>();
        if (matrix.getNextLapi() != null) {
            chain.add(matrix.getNextLapi());
        }
        Lapi current = matrix.getHighestLapi();
        while (current != null) {
            chain.add(current);
            current = current.getLowerLapi();
        }
        return chain;
    }

    public String offspringString(Body body) {
        if (body.getOffsprings().isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < body.getOffsprings().size(); i++) {
            if (i > 0) sb.append(", ");
            BodyNode node = body.getOffsprings().get(i).getBodyNode();
            if (node.getParentNode() instanceof ApiNode) {
                sb.append(node.getBodyNodeId());
            } else {
                sb.append("t").append(node.getBodyNodeId());
            }
        }
        return sb.toString();
    }

    public String deactivatedOffspringString(Body body) {
        if (body.getDeactivatedOffsprings().isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        List<Body> sorted = body.getDeactivatedOffsprings().stream()
                .sorted(Comparator.comparingInt(b -> b.getBodyNode().getBodyNodeId()))
                .toList();
        for (int i = 0; i < sorted.size(); i++) {
            if (i > 0) sb.append(", ");
            BodyNode node = sorted.get(i).getBodyNode();
            if (node.getParentNode() instanceof ApiNode) {
                sb.append(node.getBodyNodeId());
            } else {
                sb.append("t").append(node.getBodyNodeId());
            }
        }
        return sb.toString();
    }

    public String matrixNodeLabel(MatrixNode node) {
        if (node instanceof ApiNode a) return "p" + a.getIndexes().get(0).getIndex();
        if (node instanceof TransitionNode t) return "p" + t.getIndexes().get(0).getIndex() + "→p" + t.getIndexes().get(t.getIndexes().size() - 1).getIndex();
        return "";
    }

    public int totalActiveBodyCount(MatrixNode node) {
        return node.getBodyNodes().values().stream().mapToInt(bn -> bn.getActiveBodies().size()).sum();
    }

    public int totalDeactivatedBodyCount(MatrixNode node) {
        int fromActive = node.getBodyNodes().values().stream().mapToInt(bn -> bn.getDeactivatedBodies().size()).sum();
        int fromDeactivated = node.getDeactivatedBodyNodes().values().stream().mapToInt(bn -> bn.getDeactivatedBodies().size()).sum();
        return fromActive + fromDeactivated;
    }

    public int totalActiveHcnGen(BodyNode bn) {
        return bn.getActiveBodies().stream().mapToInt(Body::getActiveHcnGeneratorCount).sum();
    }


}
