package com.hcn.controller;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.hcn.db.DatabaseService;
import com.hcn.db.MatrixDeserializer;
import com.hcn.db.MatrixSerializer;
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
import java.util.List;

@Controller
public class NewCoreController {

    private Matrix matrix;
    private String activeTab = "matrix";
    private int activeBodyList = -1;
    private int activeMatrixNodeIdx = 0;
    private int activeBodyNodeId = -1;
    private String currentLogLevel = "INFO";

    @Autowired
    private DatabaseService databaseService;

    @GetMapping("/newcore")
    public String index(Model model) {
        if (matrix == null) {
            matrix = Matrix.builder().build();
            matrix.initialize();
        }
        model.addAttribute("matrix", matrix);
        model.addAttribute("logLevel", currentLogLevel);
        if (!matrix.isProving()) {
            List<MatrixNode> chain = getMatrixChain();
            model.addAttribute("matrixChain", chain);
            model.addAttribute("activeBodyList", activeBodyList == -1 ? chain.size() - 1 : activeBodyList);
            model.addAttribute("activeMatrixNodeIdx", activeMatrixNodeIdx);
            model.addAttribute("activeBodyNodeId", activeBodyNodeId);
        }
        model.addAttribute("activeTab", activeTab);
        return "newcore";
    }

    @PostMapping("/newcore/loglevel")
    public String setLogLevel(@RequestParam String level,
                              @RequestParam(defaultValue = "matrix") String tab,
                              @RequestParam(defaultValue = "-1") int bodyListIdx) {
        activeTab = tab;
        activeBodyList = bodyListIdx;
        currentLogLevel = level.toUpperCase();
        LoggerContext ctx = (LoggerContext) LoggerFactory.getILoggerFactory();
        ctx.getLogger("com.hcn.newCore").setLevel(Level.toLevel(currentLogLevel));
        return "redirect:/newcore";
    }

    @PostMapping("/newcore/save")
    public String save() {
        if (matrix != null && !matrix.isProving()) {
            if (matrix.getDbName() == null) {
                matrix.setDbName(databaseService.assignDbName());
            }
            if (!databaseService.databaseExists(matrix.getDbName())) {
                databaseService.createDatabase(matrix.getDbName());
            } else {
                databaseService.truncateTmpTables(matrix.getDbName());
            }
            MatrixSerializer serializer = new MatrixSerializer();
            List<MatrixNode> nodes = serializer.buildMatrixNodeSet(matrix);
            List<Lapi> lapis = serializer.buildLapiList(matrix);
            JdbcTemplate dbTemplate = databaseService.createTemplateForDb(matrix.getDbName());
            dbTemplate.execute(serializer.buildMatrixNodeInsert(nodes));
            dbTemplate.execute(serializer.buildPrimeInsert(nodes, lapis));
            dbTemplate.execute(serializer.buildBodyNodeInsert(nodes));
            dbTemplate.execute(serializer.buildBodyInsert(nodes));
            String orphanInsert = serializer.buildOrphanBodyNodeInsert();
            if (orphanInsert != null) dbTemplate.execute(orphanInsert);
            dbTemplate.execute(serializer.buildLapiInsert(lapis));
            String lapiHcnInsert = serializer.buildLapiHcnInsert(lapis);
            if (lapiHcnInsert != null) dbTemplate.execute(lapiHcnInsert);
            String hcnInsert = serializer.buildHcnInsert();
            if (hcnInsert != null) dbTemplate.execute(hcnInsert);
            dbTemplate.execute(serializer.buildMatrixInsert(matrix));
        }
        return "redirect:/newcore";
    }

    @GetMapping("/newcore/load")
    public String load(@RequestParam String db) {
        JdbcTemplate dbTemplate = databaseService.createTemplateForDb(db);
        MatrixDeserializer deserializer = new MatrixDeserializer(dbTemplate);
        matrix = deserializer.load();
        matrix.setDbName(db);
        activeTab = "matrix";
        activeBodyList = -1;
        return "redirect:/newcore";
    }

    @GetMapping("/newcore/quit")
    public String quit() {
        matrix = null;
        activeTab = "matrix";
        activeBodyList = -1;
        return "redirect:/";
    }

    @PostMapping("/newcore/prove")
    @ResponseBody
    public String prove(@RequestParam(defaultValue = "1") int count,
                        @RequestParam(defaultValue = "matrix") String tab,
                        @RequestParam(defaultValue = "-1") int bodyListIdx) {
        activeTab = tab;
        activeBodyList = bodyListIdx;
        if (!matrix.isProving()) {
            matrix.setProving(true);
            matrix.setProveTarget(count);
            matrix.setProveProgress(0);
            new Thread(() -> matrix.proveLapi(count)).start();
        }
        return "{\"started\":true}";
    }

    @GetMapping("/newcore/progress")
    @ResponseBody
    public String progress() {
        if (matrix == null) return "{\"proving\":false,\"progress\":0,\"target\":0}";
        return "{\"proving\":" + matrix.isProving() + ",\"progress\":" + matrix.getProveProgress() + ",\"target\":" + matrix.getProveTarget() + "}";
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

    public String guiString(Body body) {
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

    public String matrixNodeLabel(MatrixNode node) {
        if (node instanceof ApiNode a) return "p" + a.getIndexes().get(0).getIndex();
        if (node instanceof TransitionNode t) return "p" + t.getIndexes().get(0).getIndex() + "→p" + t.getIndexes().get(t.getIndexes().size() - 1).getIndex();
        return "";
    }

    public String getBodyNodesJson(List<MatrixNode> chain) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < chain.size(); i++) {
            if (i > 0) sb.append(",");
            MatrixNode node = chain.get(i);
            sb.append("[");
            boolean first = true;
            for (var entry : node.getBodyNodes().descendingMap().entrySet()) {
                BodyNode bn = entry.getValue();
                if (!first) sb.append(",");
                first = false;
                sb.append("{\"id\":").append(bn.getBodyNodeId())
                  .append(",\"value\":\"").append(bn.getValue()).append("\"")
                  .append(",\"factor\":\"").append(bn.getFactor()).append("\"")
                  .append(",\"proved\":").append(bn.isProved())
                  .append(",\"activeBodiesCount\":").append(bn.getActiveBodies().size())
                  .append(",\"activeBodies\":[");
                boolean firstBody = true;
                for (Body body : bn.getActiveBodies()) {
                    if (!firstBody) sb.append(",");
                    firstBody = false;
                    sb.append("{\"guiString\":\"").append(guiString(body).replace("\"", "\\\"")).append("\"")
                      .append(",\"value\":\"").append(body.getValue()).append("\"")
                      .append(",\"factor\":\"").append(body.getFactor()).append("\"")
                      .append(",\"proved\":").append(body.isProved())
                      .append(",\"deactivated\":").append(body.isDeactivated())
                      .append("}");
                }
                sb.append("]}");
            }
            sb.append("]");
        }
        sb.append("]");
        return sb.toString();
    }
}
