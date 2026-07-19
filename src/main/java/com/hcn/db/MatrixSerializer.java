package com.hcn.db;

import com.hcn.event.ActivityCenter;
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
    private final List<Body> collectedMatrixBodies = new ArrayList<>();
    private final List<Body> collectedDeletedBodies = new ArrayList<>();
    private final List<BodyNode> collectedBodyNodes = new ArrayList<>();
    private final List<MatrixNode> collectedMatrixNodes = new ArrayList<>();

    public void assignTempIds(Matrix matrix) {

        resetLapiBodiesAndHcns(matrix);
        resetRefIntervalBodiesAndHcns(matrix);
        resetHcnGeneratorHcns(matrix);

        reassignMatrixObjects(matrix);
        reassignLapiBodiesAndHcns(matrix.getLowestLapi());
        reassignRefIntervalBodiesAndHcns(matrix.getReferenceInterval());
    }

    private void reassignRefIntervalBodiesAndHcns(Interval referenceInterval) {
        if (referenceInterval != null) {
            assignHcnIdsAndDeletedBodyIds(referenceInterval.getHcnList());
        }
    }

    private void reassignLapiBodiesAndHcns(Lapi lowestLapi) {
        Lapi currentLapi = lowestLapi;
        while (currentLapi != null) {
            assignHcnIdsAndDeletedBodyIds(currentLapi.getHcnList());
            currentLapi = currentLapi.getHigherLapi();
        }
    }

    private void assignHcnIdsAndDeletedBodyIds(List<Hcn> hcnList) {
        for (Hcn hcn : hcnList) {
            if (hcn.getBody().getTempId() == null) {
                hcn.getBody().setTempId(++bodyTempId);
                collectedDeletedBodies.add(hcn.getBody());
            }
            if (hcn.getTempId() == null) {
                hcn.setTempId(++hcnTempId);
                collectedHcns.add(hcn);
            }
        }
    }

    private void reassignMatrixObjects(Matrix matrix) {
        // There are now orphanBodies in matrix bodylists anymore, so automatic reassign of
        MatrixNode currentNode = matrix.getLastTransition();
        while (currentNode != null) {
            currentNode.getDeactivatedBodyNodes().values().forEach(bodyNode -> {
                bodyNode.getDeactivatedBodies().forEach(this::assignBodyAndHcnIds);
                bodyNode.setTempId(++bodyNodeTempId);
                collectedBodyNodes.add(bodyNode);
            });
            currentNode.getBodyNodes().values().forEach(bodyNode -> {
                bodyNode.getActiveBodies().forEach(this::assignBodyAndHcnIds);
                bodyNode.getDeactivatedBodies().forEach(this::assignBodyAndHcnIds);
                bodyNode.setTempId(++bodyNodeTempId);
                collectedBodyNodes.add(bodyNode);
            });
            currentNode.setTempId(++matrixNodeTempId);
            collectedMatrixNodes.add(currentNode);
            currentNode = currentNode.getPrevMatrixNode();
        }
    }

    private void resetHcnGeneratorHcns(Matrix matrix) {
        Body hcnGenerator = matrix.getLastTransition().getBodyList().getSmallestBody();
        while (hcnGenerator != null) {
            resetBodyHcnTempIds(hcnGenerator);
            hcnGenerator = hcnGenerator.getLargerBody();
        }
    }

    private void resetRefIntervalBodiesAndHcns(Matrix matrix) {
        if (matrix.getReferenceInterval() != null) {
            for (Hcn hcn : matrix.getReferenceInterval().getHcnList()) {
                hcn.getBody().setTempId(null);
                resetBodyHcnTempIds(hcn.getBody());
            }
        }
    }

    private void resetLapiBodiesAndHcns(Matrix matrix) {
        // Since orphan bodies can be in interval hcnlist or lapi hcnlists
        // we need to make sure their tempIds are null, also resetting all previous tempids
        Lapi currentLapi = matrix.getHighestLapi();
        while (currentLapi != null) {
            for (Hcn hcn : currentLapi.getHcnList()){
                hcn.getBody().setTempId(null);
                resetBodyHcnTempIds(hcn.getBody());
            }
            currentLapi = currentLapi.getLowerLapi();
        }
    }

    private void assignBodyAndHcnIds(Body body) {
        body.setTempId(++bodyTempId);
        collectedMatrixBodies.add(body);

        if (body.getLastGeneratedHcn() != null && body.getLastGeneratedHcn().getTempId() == null) {
            body.getLastGeneratedHcn().setTempId(++hcnTempId);
            collectedHcns.add(body.getLastGeneratedHcn());
        }

        if (body.getFirstHcn() != null && body.getFirstHcn().getTempId() == null) {
            body.getFirstHcn().setTempId(++hcnTempId);
            collectedHcns.add(body.getFirstHcn());
        }

        if (body.getFirstSuperiorHcn() != null && body.getFirstSuperiorHcn().getTempId() == null) {
            body.getFirstSuperiorHcn().setTempId(++hcnTempId);
            collectedHcns.add(body.getFirstSuperiorHcn());
        }
        if (body.getFirstDominatedHcn() != null && body.getFirstDominatedHcn().getTempId() == null) {
            body.getFirstDominatedHcn().setTempId(++hcnTempId);
            collectedHcns.add(body.getFirstDominatedHcn());
        }
    }

    private void resetBodyHcnTempIds(Body body) {
        if (body.getLastGeneratedHcn() != null) body.getLastGeneratedHcn().setTempId(null);
        if (body.getFirstHcn() != null) body.getFirstHcn().setTempId(null);
        if (body.getFirstSuperiorHcn() != null) body.getFirstSuperiorHcn().setTempId(null);
        if (body.getFirstDominatedHcn() != null) body.getFirstDominatedHcn().setTempId(null);
    }

    public String buildMatrixNodeInsert() {
        StringBuilder sb = new StringBuilder("INSERT INTO tmp_matrix_node (id, prev_matrix_node, next_matrix_node, body_list, transition_from, transition_to) VALUES ");
        for (int i = 0; i < collectedMatrixNodes.size(); i++) {
            MatrixNode node = collectedMatrixNodes.get(i);
            Integer prevId = node.getPrevMatrixNode() != null ? node.getPrevMatrixNode().getTempId() : null;
            Integer nextId = node.getNextMatrixNode() != null ? node.getNextMatrixNode().getTempId() : null;
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
                    prevId != null ? prevId : "NULL",
                    nextId != null ? nextId : "NULL",
                    bodyListId != null ? bodyListId : "NULL",
                    transFrom != null ? transFrom : "NULL",
                    transTo != null ? transTo : "NULL"));
        }
        return sb.toString();
    }

    public String buildPrimeInsert(Matrix matrix) {
        StringBuilder sb = new StringBuilder("INSERT INTO tmp_prime (index, int_value, value_mantissa, value_exponent, matrix_node_id) VALUES ");
        boolean first = true;
        java.util.Set<Integer> insertedPrimeIndexes = new java.util.HashSet<>();
        MatrixNode currentNode = matrix.getLastTransition();
        while (currentNode != null) {
            for (Prime prime : currentNode.getIndexes()) {
                if (!first) sb.append(", ");
                first = false;
                sb.append(String.format("(%d, %d, %s, %d, %d)",
                        prime.getIndex(),
                        prime.getIntValue(),
                        prime.getValue().getMantissa(),
                        prime.getValue().getExponent(),
                        currentNode.getTempId()));
                insertedPrimeIndexes.add(prime.getIndex());
            }
            currentNode = currentNode.getPrevMatrixNode();
        }

        if (matrix.getNextLapi() != null) {
            Prime np = matrix.getNextLapi().getPrime();
            if (!insertedPrimeIndexes.contains(np.getIndex())) {
                if (!first) sb.append(", ");
                first = false;
                sb.append(String.format("(%d, %d, %s, %d, NULL)", np.getIndex(), np.getIntValue(), np.getValue().getMantissa(), np.getValue().getExponent()));
            }
        }
        Lapi currentLapi = matrix.getLowestLapi();
        while (currentLapi != null) {
            if (!insertedPrimeIndexes.contains(currentLapi.getPrime().getIndex())) {
                if (!first) sb.append(", ");
                first = false;
                sb.append(String.format("(%d, %d, %s, %d, NULL)",
                        currentLapi.getPrime().getIndex(),
                        currentLapi.getPrime().getIntValue(),
                        currentLapi.getPrime().getValue().getMantissa(),
                        currentLapi.getPrime().getValue().getExponent()));
            }
            currentLapi = currentLapi.getHigherLapi();
        }

        return sb.toString();
    }

    public String buildLapiInsert(Matrix matrix) {
        StringBuilder sb = new StringBuilder("INSERT INTO tmp_lapi (prime, lower_lapi, higher_lapi, walker, value_multiplier_mantissa, value_multiplier_exponent, factor_multiplier_mantissa, factor_multiplier_exponent) VALUES ");
        boolean first = true;
        if (matrix.getNextLapi() != null) {
            first = false;
            Lapi nl = matrix.getNextLapi();
            sb.append(String.format("(%d, %s, %s, %s, %s, %s, %s, %s)",
                    nl.getPrime().getIndex(),
                    nl.getLowerLapi() != null ? nl.getLowerLapi().getPrime().getIndex() : "NULL",
                    nl.getHigherLapi() != null ? nl.getHigherLapi().getPrime().getIndex() : "NULL",
                    nl.getWalker() != null ? nl.getWalker().getTempId() : "NULL",
                    nl.getValueMultiplier() != null ? nl.getValueMultiplier().getMantissa() : "NULL",
                    nl.getValueMultiplier() != null ? nl.getValueMultiplier().getExponent() : "NULL",
                    nl.getFactorMultiplier() != null ? nl.getFactorMultiplier().getMantissa() : "NULL",
                    nl.getFactorMultiplier() != null ? nl.getFactorMultiplier().getExponent() : "NULL"));
        }
        Lapi lapi = matrix.getLowestLapi();
        while (lapi != null) {
            if (!first) sb.append(", ");
            first = false;
            sb.append(String.format("(%d, %s, %s, %s, %s, %s, %s, %s)",
                    lapi.getPrime().getIndex(),
                    lapi.getLowerLapi() != null ? lapi.getLowerLapi().getPrime().getIndex() : "NULL",
                    lapi.getHigherLapi() != null ? lapi.getHigherLapi().getPrime().getIndex() : "NULL",
                    lapi.getWalker() != null ? lapi.getWalker().getTempId() : "NULL",
                    lapi.getValueMultiplier() != null ? lapi.getValueMultiplier().getMantissa() : "NULL",
                    lapi.getValueMultiplier() != null ? lapi.getValueMultiplier().getExponent() : "NULL",
                    lapi.getFactorMultiplier() != null ? lapi.getFactorMultiplier().getMantissa() : "NULL",
                    lapi.getFactorMultiplier() != null ? lapi.getFactorMultiplier().getExponent() : "NULL"));
            lapi = lapi.getHigherLapi();
        }
        return sb.toString();
    }

    public String buildLapiHcnInsert(Matrix matrix) {
        StringBuilder sb = new StringBuilder("INSERT INTO tmp_lapi_hcn (lapi_prime, list_position, hcn) VALUES ");
        boolean first = true;
        if (matrix.getNextLapi() != null) {
            for (int i = 0; i < matrix.getNextLapi().getHcnList().size(); i++) {
                Hcn hcn = matrix.getNextLapi().getHcnList().get(i);
                if (!first) sb.append(", ");
                first = false;
                sb.append(String.format("(%d, %d, %d)", matrix.getNextLapi().getPrime().getIndex(), i, hcn.getTempId()));
            }
        }
        Lapi lapi = matrix.getLowestLapi();
        while (lapi != null) {
            for (int i = 0; i < lapi.getHcnList().size(); i++) {
                Hcn hcn = lapi.getHcnList().get(i);
                if (!first) sb.append(", ");
                first = false;
                sb.append(String.format("(%d, %d, %d)", lapi.getPrime().getIndex(), i, hcn.getTempId()));
            }
            lapi = lapi.getHigherLapi();
        }
        return first ? null : sb.toString();
    }

    public String buildBodyNodeInsert() {
        StringBuilder bodyNodeSb = new StringBuilder("INSERT INTO tmp_body_node (id, parent_node, body_node_id, proved, value_mantissa, value_exponent, factor_mantissa, factor_exponent, active) VALUES ");

        boolean first = true;
        for (BodyNode bodyNode : collectedBodyNodes) {
            if (!first) bodyNodeSb.append(", ");
            first = false;
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
        }
        return bodyNodeSb.toString();
    }

    public String buildBodyInsert() {
        StringBuilder sb = new StringBuilder("INSERT INTO tmp_body (id, body_node, value_mantissa, value_exponent, factor_mantissa, factor_exponent, parent, proved, smaller_body, larger_body, smaller_active_body, larger_active_body, last_generated_hcn, first_hcn, first_superior_hcn, first_dominated_hcn, deactivated, db_id) VALUES ");
        for (int i = 0; i < collectedMatrixBodies.size(); i++) {
            if (i > 0) sb.append(", ");
            appendBodyValues(sb, collectedMatrixBodies.get(i));
        }

        for (Body body : collectedDeletedBodies) {
            sb.append(", ");
            appendDeletedBodyValues(sb, body);
        }
        return sb.toString();
    }

    private void appendBodyValues(StringBuilder sb, Body current) {
        sb.append(String.format("(%d, %d, %s, %d, %s, %d, %s, %b, %s, %s, %s, %s, %s, %s, %s, %s, %b, %s)",
                current.getTempId(),
                current.getBodyNode().getTempId(),
                current.getValue().getMantissa(),
                current.getValue().getExponent(),
                current.getFactor().getMantissa(),
                current.getFactor().getExponent(),
                current.getParent() != null ? current.getParent().getTempId() : "NULL",
                current.isProved(),
                current.getSmallerBody() != null ? current.getSmallerBody().getTempId() : "NULL",
                current.getLargerBody() != null ? current.getLargerBody().getTempId() : "NULL",
                current.getSmallerHcnGenerator() != null ? current.getSmallerHcnGenerator().getTempId() : "NULL",
                current.getLargerHcnGenerator() != null ? current.getLargerHcnGenerator().getTempId() : "NULL",
                current.getLastGeneratedHcn() != null ? current.getLastGeneratedHcn().getTempId() : "NULL",
                current.getFirstHcn() != null ? current.getFirstHcn().getTempId() : "NULL",
                current.getFirstSuperiorHcn() != null ? current.getFirstSuperiorHcn().getTempId() : "NULL",
                current.getFirstDominatedHcn() != null ? current.getFirstDominatedHcn().getTempId() : "NULL",
                current.isDeactivated(),
                current.getDbId() != null ? current.getDbId() : "NULL"));
    }

    private void appendDeletedBodyValues(StringBuilder sb, Body current) {
        sb.append(String.format("(%d, NULL, %s, %d, %s, %d, NULL, %b, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, %b, %s)",
                current.getTempId(),
                current.getValue().getMantissa(),
                current.getValue().getExponent(),
                current.getFactor().getMantissa(),
                current.getFactor().getExponent(),
                current.isProved(),
                current.isDeactivated(),
                current.getDbId() != null ? current.getDbId() : "NULL"));
    }

    public String buildHcnInsert() {
        StringBuilder sb = new StringBuilder("INSERT INTO tmp_hcn (id, body, lapi, value_mantissa, value_exponent, factor_mantissa, factor_exponent) VALUES ");
        for (int i = 0; i < collectedHcns.size(); i++) {
            Hcn hcn = collectedHcns.get(i);
            if (i > 0) sb.append(", ");
            sb.append(String.format("(%d, %d, %d, %s, %d, %s, %d)",
                    hcn.getTempId(),
                    hcn.getBody().getTempId(),
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
            sb.append(String.format("(%d, %d)", i, hcn.getTempId()));
        }
        return sb.toString();
    }

    public String buildMatrixInsert(Matrix matrix) {
        return String.format("INSERT INTO tmp_matrix (last_transition, next_lapi, lowest_lapi, highest_lapi, lowest_proved_lapi_within_interval, proved_count, proved_limit_mantissa, proved_limit_exponent, total_time_ms, matrix_maintain_time_ms, generate_hcn_list_time_ms, db_mode, total_nanos, reference_interval_lapi, reference_interval_value_mantissa, reference_interval_value_exponent, reference_interval_factor_mantissa, reference_interval_factor_exponent) VALUES (%d, %s, %s, %s, %d, %d, %s, %d, %d, %d, %d, %b, %d, %s, %s, %s, %s, %s)",
                matrix.getLastTransition().getTempId(),
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
                ActivityCenter.getTotalNanos(),
                matrix.getReferenceInterval() != null ? matrix.getReferenceInterval().getLapi() : "NULL",
                matrix.getReferenceInterval() != null ? matrix.getReferenceInterval().getValue().getMantissa() : "NULL",
                matrix.getReferenceInterval() != null ? matrix.getReferenceInterval().getValue().getExponent() : "NULL",
                matrix.getReferenceInterval() != null ? matrix.getReferenceInterval().getFactor().getMantissa() : "NULL",
                matrix.getReferenceInterval() != null ? matrix.getReferenceInterval().getFactor().getExponent() : "NULL");
    }
}
