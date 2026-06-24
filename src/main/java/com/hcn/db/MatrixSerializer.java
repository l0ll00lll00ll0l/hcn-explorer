package com.hcn.db;

import com.hcn.newCore.Body;
import com.hcn.newCore.BodyNode;
import com.hcn.newCore.Hcn;
import com.hcn.newCore.Lapi;
import com.hcn.newCore.Matrix;
import com.hcn.newCore.MatrixNode;
import com.hcn.newCore.Prime;
import com.hcn.newCore.TransitionNode;

import java.util.ArrayList;
import java.util.List;

public class MatrixSerializer {

    private int matrixNodeTempId = 0;
    private int bodyTempId = 0;
    private int bodyNodeTempId = 0;
    private int hcnTempId = 0;
    private final List<BodyNode> orphanBodyNodes = new ArrayList<>();
    private final List<Hcn> collectedHcns = new ArrayList<>();
    private final List<Body> orphanBodies = new ArrayList<>();

    public List<MatrixNode> buildMatrixNodeSet(Matrix matrix) {
        List<MatrixNode> nodes = new ArrayList<>();
        MatrixNode current = matrix.getLastTransition();
        while (current.getPrevMatrixNode() != null) {
            current = current.getPrevMatrixNode();
        }
        while (current != null) {
            current.setTempId(null);
            resetBodyTempIds(current);
            nodes.add(current);
            current = current.getNextMatrixNode();
        }
        for (MatrixNode node : nodes) {
            assignMatrixNodeTempId(node);
            assignBodyTempIds(node);
        }
        return nodes;
    }

    private void resetBodyTempIds(MatrixNode node) {
        if (node.getBodyList() == null) return;
        Body current = node.getBodyList().getSmallestBody();
        while (current != null) {
            current.setTempId(null);
            current.getBodyNode().setTempId(null);
            if (current.getLastGeneratedHcn() != null) current.getLastGeneratedHcn().setTempId(null);
            if (current.getFirstHcn() != null) current.getFirstHcn().setTempId(null);
            if (current.getFirstSuperiorHcn() != null) current.getFirstSuperiorHcn().setTempId(null);
            // reset parent chain
            Body parent = current.getParent();
            while (parent != null && parent.getTempId() != null) {
                parent.setTempId(null);
                parent.getBodyNode().setTempId(null);
                parent = parent.getParent();
            }
            current = current.getLargerBody();
        }
    }

    private int assignMatrixNodeTempId(MatrixNode node) {
        if (node.getTempId() == null) {
            node.setTempId(++matrixNodeTempId);
        }
        return node.getTempId();
    }

    private int assignBodyTempId(Body body) {
        if (body.getTempId() == null) {
            body.setTempId(++bodyTempId);
        }
        return body.getTempId();
    }

    private int assignHcnTempId(Hcn hcn) {
        if (hcn.getTempId() == null) {
            hcn.setTempId(++hcnTempId);
            collectedHcns.add(hcn);
        }
        return hcn.getTempId();
    }

    private void assignBodyTempIds(MatrixNode node) {
        if (node.getBodyList() == null) return;
        Body current = node.getBodyList().getSmallestBody();
        while (current != null) {
            assignBodyTempId(current);
            current = current.getLargerBody();
        }
    }

    public String buildMatrixNodeInsert(List<MatrixNode> nodes) {
        StringBuilder sb = new StringBuilder("INSERT INTO tmp_matrix_node (id, prev_matrix_node, next_matrix_node, body_list, transition_from, transition_to) VALUES ");
        for (int i = 0; i < nodes.size(); i++) {
            MatrixNode node = nodes.get(i);
            Integer prevId = node.getPrevMatrixNode() != null ? assignMatrixNodeTempId(node.getPrevMatrixNode()) : null;
            Integer nextId = node.getNextMatrixNode() != null ? assignMatrixNodeTempId(node.getNextMatrixNode()) : null;
            Integer bodyListId = node.getBodyList() != null ? node.getBodyList().getSmallestBody().getTempId() : null;
            Integer transFrom = null;
            Integer transTo = null;
            if (node instanceof TransitionNode t) {
                transFrom = t.getTransitionFrom();
                transTo = t.getTransitionTo();
            }
            if (i > 0) sb.append(", ");
            sb.append(String.format("(%d, %s, %s, %s, %s, %s)",
                    node.getTempId(),
                    prevId != null ? prevId.toString() : "NULL",
                    nextId != null ? nextId.toString() : "NULL",
                    bodyListId != null ? bodyListId.toString() : "NULL",
                    transFrom != null ? transFrom.toString() : "NULL",
                    transTo != null ? transTo.toString() : "NULL"));
        }
        return sb.toString();
    }

    public String buildPrimeInsert(List<MatrixNode> nodes, List<Lapi> lapis) {
        StringBuilder sb = new StringBuilder("INSERT INTO tmp_prime (index, int_value, value_mantissa, value_exponent, matrix_node_id) VALUES ");
        boolean first = true;
        for (MatrixNode node : nodes) {
            for (Prime prime : node.getIndexes()) {
                if (!first) sb.append(", ");
                first = false;
                sb.append(String.format("(%d, %d, %s, %d, %d)",
                        prime.getIndex(),
                        prime.getIntValue(),
                        prime.getValue().getMantissa(),
                        prime.getValue().getExponent(),
                        node.getTempId()));
            }
        }
        for (Lapi lapi : lapis) {
            if (!first) sb.append(", ");
            first = false;
            sb.append(String.format("(%d, %d, %s, %d, NULL)",
                    lapi.getPrime().getIndex(),
                    lapi.getPrime().getIntValue(),
                    lapi.getPrime().getValue().getMantissa(),
                    lapi.getPrime().getValue().getExponent()));
        }
        return sb.toString();
    }

    public List<Lapi> buildLapiList(Matrix matrix) {
        List<Lapi> lapis = new ArrayList<>();
        if (matrix.getNextLapi() != null) lapis.add(matrix.getNextLapi());
        Lapi current = matrix.getLowestLapi();
        while (current != null) {
            lapis.add(current);
            current = current.getHigherLapi();
        }
        return lapis;
    }

    public String buildLapiInsert(List<Lapi> lapis) {
        StringBuilder sb = new StringBuilder("INSERT INTO tmp_lapi (prime, lower_lapi, higher_lapi, walker, value_multiplier_mantissa, value_multiplier_exponent, factor_multiplier_mantissa, factor_multiplier_exponent) VALUES ");
        for (int i = 0; i < lapis.size(); i++) {
            Lapi lapi = lapis.get(i);
            if (i > 0) sb.append(", ");
            sb.append(String.format("(%d, %s, %s, %s, %s, %s, %s, %s)",
                    lapi.getPrime().getIndex(),
                    lapi.getLowerLapi() != null ? lapi.getLowerLapi().getPrime().getIndex() : "NULL",
                    lapi.getHigherLapi() != null ? lapi.getHigherLapi().getPrime().getIndex() : "NULL",
                    lapi.getWalker() != null ? assignBodyTempId(lapi.getWalker()) : "NULL",
                    lapi.getValueMultiplier() != null ? lapi.getValueMultiplier().getMantissa() : "NULL",
                    lapi.getValueMultiplier() != null ? lapi.getValueMultiplier().getExponent() : "NULL",
                    lapi.getFactorMultiplier() != null ? lapi.getFactorMultiplier().getMantissa() : "NULL",
                    lapi.getFactorMultiplier() != null ? lapi.getFactorMultiplier().getExponent() : "NULL"));
        }
        return sb.toString();
    }

    public String buildLapiHcnInsert(List<Lapi> lapis) {
        StringBuilder sb = new StringBuilder("INSERT INTO tmp_lapi_hcn (lapi_prime, list_position, hcn) VALUES ");
        boolean first = true;
        for (Lapi lapi : lapis) {
            for (int i = 0; i < lapi.getHcnList().size(); i++) {
                if (!first) sb.append(", ");
                first = false;
                sb.append(String.format("(%d, %d, %d)",
                        lapi.getPrime().getIndex(),
                        i,
                        assignHcnTempId(lapi.getHcnList().get(i))));
            }
        }
        return first ? null : sb.toString();
    }

    private int assignBodyNodeTempId(BodyNode bodyNode) {
        if (bodyNode.getTempId() == null) {
            bodyNode.setTempId(++bodyNodeTempId);
        }
        return bodyNode.getTempId();
    }

    public String buildBodyNodeInsert(List<MatrixNode> nodes) {
        List<BodyNode> allBodyNodes = new ArrayList<>();
        for (MatrixNode node : nodes) {
            for (BodyNode bodyNode : node.getBodyNodes().values()) {
                assignBodyNodeTempId(bodyNode);
                allBodyNodes.add(bodyNode);
            }
        }
        return buildBodyNodeInsertFromList(allBodyNodes, true);
    }
    public String buildBodyInsert(List<MatrixNode> nodes) {
        StringBuilder sb = new StringBuilder("INSERT INTO tmp_body (id, body_node, value_mantissa, value_exponent, factor_mantissa, factor_exponent, parent, proved, smaller_body, larger_body, last_generated_hcn, first_hcn, first_superior_hcn, deactivated) VALUES ");
        boolean first = true;
        List<Body> insertedBodies = new ArrayList<>();
        for (MatrixNode node : nodes) {
            if (node.getBodyList() == null) continue;
            Body current = node.getBodyList().getSmallestBody();
            while (current != null) {
                if (!first) sb.append(", ");
                first = false;
                appendBodyValues(sb, current);
                insertedBodies.add(current);
                current = current.getLargerBody();
            }
        }
        // collect orphan bodies reachable via parent chains
        collectOrphanBodies(insertedBodies);
        for (Body orphan : orphanBodies) {
            sb.append(", ");
            appendBodyValues(sb, orphan);
        }
        return sb.toString();
    }

    private void collectOrphanBodies(List<Body> insertedBodies) {
        java.util.Set<Body> inserted = new java.util.HashSet<>(insertedBodies);
        java.util.Set<Body> checked = new java.util.HashSet<>(inserted);
        for (Body body : insertedBodies) {
            Body parent = body.getParent();
            while (parent != null && !checked.contains(parent)) {
                checked.add(parent);
                if (!inserted.contains(parent)) {
                    assignBodyTempId(parent);
                    orphanBodies.add(parent);
                    if (parent.getBodyNode().getTempId() == null) {
                        assignBodyNodeTempId(parent.getBodyNode());
                        orphanBodyNodes.add(parent.getBodyNode());
                    }
                }
                parent = parent.getParent();
            }
        }
    }

    private void appendBodyValues(StringBuilder sb, Body current) {
        if (current.getBodyNode().getTempId() == null) {
            assignBodyNodeTempId(current.getBodyNode());
            orphanBodyNodes.add(current.getBodyNode());
        }
        sb.append(String.format("(%d, %d, %s, %d, %s, %d, %s, %b, %s, %s, %s, %s, %s, %b)",
                assignBodyTempId(current),
                current.getBodyNode().getTempId(),
                current.getValue().getMantissa(),
                current.getValue().getExponent(),
                current.getFactor().getMantissa(),
                current.getFactor().getExponent(),
                current.getParent() != null ? assignBodyTempId(current.getParent()) : "NULL",
                current.isProved(),
                current.getSmallerBody() != null ? assignBodyTempId(current.getSmallerBody()) : "NULL",
                current.getLargerBody() != null ? assignBodyTempId(current.getLargerBody()) : "NULL",
                current.getLastGeneratedHcn() != null ? assignHcnTempId(current.getLastGeneratedHcn()) : "NULL",
                current.getFirstHcn() != null ? assignHcnTempId(current.getFirstHcn()) : "NULL",
                current.getFirstSuperiorHcn() != null ? assignHcnTempId(current.getFirstSuperiorHcn()) : "NULL",
                current.isDeactivated()));
    }

    public String buildOrphanBodyNodeInsert() {
        if (orphanBodyNodes.isEmpty()) return null;
        return buildBodyNodeInsertFromList(orphanBodyNodes, false);
    }

    private String buildBodyNodeInsertFromList(List<BodyNode> bodyNodes, boolean active) {
        StringBuilder sb = new StringBuilder("INSERT INTO tmp_body_node (id, parent_node, body_node_id, proved, value_mantissa, value_exponent, factor_mantissa, factor_exponent, active) VALUES ");
        for (int i = 0; i < bodyNodes.size(); i++) {
            BodyNode bodyNode = bodyNodes.get(i);
            if (i > 0) sb.append(", ");
            sb.append(String.format("(%d, %s, %d, %b, %s, %d, %s, %d, %b)",
                    bodyNode.getTempId(),
                    bodyNode.getParentNode() != null ? bodyNode.getParentNode().getTempId() : "NULL",
                    bodyNode.getBodyNodeId(),
                    bodyNode.isProved(),
                    bodyNode.getValue().getMantissa(),
                    bodyNode.getValue().getExponent(),
                    bodyNode.getFactor().getMantissa(),
                    bodyNode.getFactor().getExponent(),
                    active));
        }
        return sb.toString();
    }

    public String buildHcnInsert() {
        if (collectedHcns.isEmpty()) return null;
        StringBuilder sb = new StringBuilder("INSERT INTO tmp_hcn (id, body, lapi, value_mantissa, value_exponent, factor_mantissa, factor_exponent) VALUES ");
        for (int i = 0; i < collectedHcns.size(); i++) {
            Hcn hcn = collectedHcns.get(i);
            if (i > 0) sb.append(", ");
            sb.append(String.format("(%d, %d, %d, %s, %d, %s, %d)",
                    hcn.getTempId(),
                    assignBodyTempId(hcn.getBody()),
                    hcn.getLapi(),
                    hcn.getValue().getMantissa(),
                    hcn.getValue().getExponent(),
                    hcn.getFactor().getMantissa(),
                    hcn.getFactor().getExponent()));
        }
        return sb.toString();
    }

    public String buildMatrixInsert(Matrix matrix) {
        return String.format("INSERT INTO tmp_matrix (last_transition, next_lapi, lowest_lapi, highest_lapi, lowest_proved_lapi_within_interval, proved_count, proved_limit_mantissa, proved_limit_exponent, total_time_ms, matrix_maintain_time_ms, generate_hcn_list_time_ms) VALUES (%d, %s, %s, %s, %d, %d, %s, %d, %d, %d, %d)",
                assignMatrixNodeTempId(matrix.getLastTransition()),
                matrix.getNextLapi() != null ? matrix.getNextLapi().getPrime().getIndex() : "NULL",
                matrix.getLowestLapi() != null ? matrix.getLowestLapi().getPrime().getIndex() : "NULL",
                matrix.getHighestLapi() != null ? matrix.getHighestLapi().getPrime().getIndex() : "NULL",
                matrix.getLowestProvedLapiWithinInterval(),
                matrix.getProvedCount(),
                matrix.getProvedLimit().getMantissa(),
                matrix.getProvedLimit().getExponent(),
                matrix.getTotalTimeMs(),
                matrix.getMatrixMaintainTimeMs(),
                matrix.getGenerateHcnListTimeMs());
    }
}
