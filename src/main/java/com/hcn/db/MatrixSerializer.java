package com.hcn.db;

import com.hcn.newCore.Body;
import com.hcn.newCore.BodyNode;
import com.hcn.newCore.Hcn;
import com.hcn.newCore.Interval;
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
    private final List<Hcn> collectedHcns = new ArrayList<>();
    private final List<Body> collectedBodies = new ArrayList<>();
    private final StringBuilder bodyNodeSb = new StringBuilder("INSERT INTO tmp_body_node (id, parent_node, body_node_id, proved, value_mantissa, value_exponent, factor_mantissa, factor_exponent, active) VALUES ");
    private boolean bodyNodeSbHasRows = false;

    public List<MatrixNode> buildMatrixNodeSet(Matrix matrix) {
        List<MatrixNode> nodes = new ArrayList<>();
        MatrixNode current = matrix.getLastTransition();
        while (current.getPrevMatrixNode() != null) {
            current = current.getPrevMatrixNode();
        }
        while (current != null) {
            current.setTempId(null);
            nodes.add(current);
            current = current.getNextMatrixNode();
        }
        return nodes;
    }

    public void prepareForSave(Matrix matrix, List<MatrixNode> nodes, List<Lapi> lapis) {
        resetAllBodyTempIds(matrix, nodes, lapis);
        for (MatrixNode node : nodes) {
            assignMatrixNodeTempId(node);
            assignBodyTempIds(node);
        }
        for (Lapi lapi : lapis) {
            for (Hcn hcn : lapi.getHcnList()) assignBodyTempIdsWithParents(hcn.getBody());
        }
        if (matrix.getReferenceInterval() != null) {
            for (Hcn hcn : matrix.getReferenceInterval().getHcnList()) assignBodyTempIdsWithParents(hcn.getBody());
        }
    }

    private void assignBodyTempIdsWithParents(Body body) {
        if (body.getTempId() != null) return;
        assignBodyTempId(body);
        assignBodyNodeTempId(body.getBodyNode());
        Body parent = body.getParent();
        while (parent != null && parent.getTempId() == null) {
            assignBodyTempId(parent);
            assignBodyNodeTempId(parent.getBodyNode());
            parent = parent.getParent();
        }
    }

    private void resetBodyTempIds(Body body) {
        body.setTempId(null);
        body.getBodyNode().setTempId(null);
        if (body.getLastGeneratedHcn() != null) body.getLastGeneratedHcn().setTempId(null);
        if (body.getFirstHcn() != null) body.getFirstHcn().setTempId(null);
        if (body.getFirstSuperiorHcn() != null) body.getFirstSuperiorHcn().setTempId(null);
        if (body.getFirstDominatedHcn() != null) body.getFirstDominatedHcn().setTempId(null);
        if (body.isDeactivated()) {
            Body parent = body.getParent();
            while (parent != null) {
                parent.setTempId(null);
                parent.getBodyNode().setTempId(null);
                parent = parent.getParent();
            }
        }
    }

    private void resetAllBodyTempIds(Matrix matrix, List<MatrixNode> nodes, List<Lapi> lapis) {
        for (MatrixNode node : nodes) {
            if (node.getBodyList() == null) continue;
            Body current = node.getBodyList().getSmallestBody();
            while (current != null) {
                resetBodyTempIds(current);
                current = current.getLargerBody();
            }
        }
        for (Lapi lapi : lapis) {
            for (Hcn hcn : lapi.getHcnList()) resetBodyTempIds(hcn.getBody());
        }
        if (matrix.getReferenceInterval() != null) {
            for (Hcn hcn : matrix.getReferenceInterval().getHcnList()) resetBodyTempIds(hcn.getBody());
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
            collectedBodies.add(body);
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
            assignBodyNodeTempId(current.getBodyNode());
            if (current.isDeactivated()) {
                Body parent = current.getParent();
                while (parent != null && parent.getTempId() == null) {
                    assignBodyTempId(parent);
                    assignBodyNodeTempId(parent.getBodyNode());
                    parent = parent.getParent();
                }
            }
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
                Hcn hcn = lapi.getHcnList().get(i);
                if (!first) sb.append(", ");
                first = false;
                sb.append(String.format("(%d, %d, %d)",
                        lapi.getPrime().getIndex(),
                        i,
                        assignHcnTempId(hcn)));
            }
        }
        return first ? null : sb.toString();
    }

    private int assignBodyNodeTempId(BodyNode bodyNode) {
        if (bodyNode.getTempId() == null) {
            bodyNode.setTempId(++bodyNodeTempId);
            if (bodyNodeSbHasRows) bodyNodeSb.append(", ");
            bodyNodeSb.append(String.format("(%d, %s, %d, %b, %s, %d, %s, %d, %b)",
                    bodyNode.getTempId(),
                    bodyNode.getParentNode() != null ? bodyNode.getParentNode().getTempId() : "NULL",
                    bodyNode.getBodyNodeId(),
                    bodyNode.isProved(),
                    bodyNode.getValue().getMantissa(),
                    bodyNode.getValue().getExponent(),
                    bodyNode.getFactor().getMantissa(),
                    bodyNode.getFactor().getExponent(),
                    bodyNode.isActive()));
            bodyNodeSbHasRows = true;
        }
        return bodyNode.getTempId();
    }

    public String buildBodyNodeInsert() {
        return bodyNodeSbHasRows ? bodyNodeSb.toString() : null;
    }

    public String buildBodyInsert() {
        if (collectedBodies.isEmpty()) return null;
        StringBuilder sb = new StringBuilder("INSERT INTO tmp_body (id, body_node, value_mantissa, value_exponent, factor_mantissa, factor_exponent, parent, proved, smaller_body, larger_body, smaller_active_body, larger_active_body, last_generated_hcn, first_hcn, first_superior_hcn, first_dominated_hcn, deactivated, db_id) VALUES ");
        for (int i = 0; i < collectedBodies.size(); i++) {
            if (i > 0) sb.append(", ");
            appendBodyValues(sb, collectedBodies.get(i));
        }
        return sb.toString();
    }

    private void appendBodyValues(StringBuilder sb, Body current) {
        sb.append(String.format("(%d, %d, %s, %d, %s, %d, %s, %b, %s, %s, %s, %s, %s, %s, %s, %s, %b, %s)",
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
                current.getSmallerActiveBody() != null ? assignBodyTempId(current.getSmallerActiveBody()) : "NULL",
                current.getLargerActiveBody() != null ? assignBodyTempId(current.getLargerActiveBody()) : "NULL",
                current.getLastGeneratedHcn() != null ? assignHcnTempId(current.getLastGeneratedHcn()) : "NULL",
                current.getFirstHcn() != null ? assignHcnTempId(current.getFirstHcn()) : "NULL",
                current.getFirstSuperiorHcn() != null ? assignHcnTempId(current.getFirstSuperiorHcn()) : "NULL",
                current.getFirstDominatedHcn() != null ? assignHcnTempId(current.getFirstDominatedHcn()) : "NULL",
                current.isDeactivated(),
                current.getDbId() != null ? current.getDbId() : "NULL"));
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

    public String buildReferenceIntervalHcnInsert(Matrix matrix) {
        Interval ri = matrix.getReferenceInterval();
        if (ri == null || ri.getHcnList().isEmpty()) return null;
        StringBuilder sb = new StringBuilder("INSERT INTO tmp_reference_interval_hcn (list_position, hcn) VALUES ");
        List<Hcn> hcnList = ri.getHcnList();
        for (int i = 0; i < hcnList.size(); i++) {
            Hcn hcn = hcnList.get(i);
            if (i > 0) sb.append(", ");
            sb.append(String.format("(%d, %d)", i, assignHcnTempId(hcn)));
        }
        return sb.toString();
    }

    public String buildMatrixInsert(Matrix matrix) {
        return String.format("INSERT INTO tmp_matrix (last_transition, next_lapi, lowest_lapi, highest_lapi, lowest_proved_lapi_within_interval, proved_count, proved_limit_mantissa, proved_limit_exponent, total_time_ms, matrix_maintain_time_ms, generate_hcn_list_time_ms, db_mode, hcn_id_counter, body_id_counter, reference_interval_lapi, reference_interval_value_mantissa, reference_interval_value_exponent, reference_interval_factor_mantissa, reference_interval_factor_exponent) VALUES (%d, %s, %s, %s, %d, %d, %s, %d, %d, %d, %d, %b, %d, %d, %s, %s, %s, %s, %s)",
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
                matrix.getGenerateHcnListTimeMs(),
                matrix.isDbMode(),
                matrix.isDbMode() ? matrix.getDbInsertService().getHcnIdCounter() : 0,
                matrix.isDbMode() ? matrix.getDbInsertService().getBodyIdCounter() : 0,
                matrix.getReferenceInterval() != null ? matrix.getReferenceInterval().getLapi() : "NULL",
                matrix.getReferenceInterval() != null ? matrix.getReferenceInterval().getValue().getMantissa() : "NULL",
                matrix.getReferenceInterval() != null ? matrix.getReferenceInterval().getValue().getExponent() : "NULL",
                matrix.getReferenceInterval() != null ? matrix.getReferenceInterval().getFactor().getMantissa() : "NULL",
                matrix.getReferenceInterval() != null ? matrix.getReferenceInterval().getFactor().getExponent() : "NULL");
    }
}
