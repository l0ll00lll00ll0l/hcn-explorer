package com.hcn.controller;

import com.hcn.newCore.*;
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
    private int activeBodyList = -1; // -1 means last

    @GetMapping("/newcore")
    public String index(Model model) {
        if (matrix == null) {
            matrix = Matrix.builder().build();
            matrix.initialize();
        }
        model.addAttribute("matrix", matrix);
        model.addAttribute("matrixChain", getMatrixChain());
        model.addAttribute("activeTab", activeTab);
        model.addAttribute("activeBodyList", activeBodyList == -1 ? getMatrixChain().size() - 1 : activeBodyList);
        return "newcore";
    }

    @GetMapping("/newcore/quit")
    public String quit() {
        matrix = null;
        activeTab = "matrix";
        activeBodyList = -1;
        return "redirect:/";
    }

    @PostMapping("/newcore/prove")
    public String prove(@RequestParam(defaultValue = "1") int count,
                        @RequestParam(defaultValue = "matrix") String tab,
                        @RequestParam(defaultValue = "-1") int bodyListIdx) {
        activeTab = tab;
        activeBodyList = bodyListIdx;
        if (!matrix.isProving()) {
            new Thread(() -> matrix.proveLapi(count)).start();
        }
        return "redirect:/newcore";
    }

    @GetMapping("/newcore/progress")
    @ResponseBody
    public String progress() {
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
            sb.append("p").append(apiNode.getIndex()).append("^").append(node.getBodyNodeId());
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
}
