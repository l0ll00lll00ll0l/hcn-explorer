package com.hcn.db;

import com.hcn.event.ActivityCenter;
import com.hcn.newCore.*;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MatrixDeserializer {

    private final JdbcTemplate dbTemplate;
    private final DbInsertService dbInsertService;
    private PrimeCenter lapiPrimeCenter;
    private PrimeCenter matrixPrimeCenter;

    private final Map<Integer, MatrixNode> matrixNodeMap = new HashMap<>();
    private final Map<Integer, BodyNode> bodyNodeMap = new HashMap<>();
    private final Map<Integer, Body> bodyMap = new HashMap<>();
    private final Map<Integer, Hcn> hcnMap = new HashMap<>();
    private final Map<Integer, Lapi> lapiMap = new HashMap<>();

    public MatrixDeserializer(JdbcTemplate dbTemplate, DbInsertService dbInsertService) {
        this.dbTemplate = dbTemplate;
        this.dbInsertService = dbInsertService;
    }

    public Matrix load() {
        loadPrimeCenters();
        loadMatrixNodes();
        loadBodyNodes();
        loadBodies();
        loadHcns();
        loadLapis();
        loadLapiHcns();
        loadReferenceIntervalHcns();
        wireMatrixNodeReferences();
        wireBodyNodeReferences();
        wireBodyReferences();
        wireHcnReferences();
        wireLapiReferences();
        buildOffsprings();
        buildActiveBodies();
        return buildMatrix();
    }

    private void loadPrimeCenters() {
        int nextLapiPrimeIndex = dbTemplate.queryForObject(
                "SELECT next_lapi FROM tmp_matrix", Integer.class);

        lapiPrimeCenter = new PrimeCenter();
        lapiPrimeCenter.getPrime(nextLapiPrimeIndex);

        int maxMatrixPrimeIndex = dbTemplate.queryForObject(
                "SELECT MAX(index) FROM tmp_prime WHERE matrix_node_id IS NOT NULL", Integer.class);

        matrixPrimeCenter = new PrimeCenter();
        matrixPrimeCenter.getPrime(maxMatrixPrimeIndex);
    }

    private void loadMatrixNodes() {
        int lastTransitionId = dbTemplate.queryForObject(
                "SELECT last_transition FROM tmp_matrix", Integer.class);

        dbTemplate.query("SELECT * FROM tmp_matrix_node", rs -> {
            int id = rs.getInt("id");
            Integer transFrom = (Integer) rs.getObject("transition_from");
            Integer transTo = (Integer) rs.getObject("transition_to");

            MatrixNode node;
            if (transFrom != null) {
                TransitionNode tn = TransitionNode.builder()
                        .transitionFrom(transFrom)
                        .transitionTo(transTo)
                        .build();
                node = tn;
            } else {
                node = ApiNode.builder().build();
            }
            matrixNodeMap.put(id, node);
        });
    }

    private void loadBodyNodes() {
        dbTemplate.query("SELECT * FROM tmp_body_node", rs -> {
            int id = rs.getInt("id");
            int parentNodeId = rs.getInt("parent_node");
            int bodyNodeId = rs.getInt("body_node_id");
            boolean proved = rs.getBoolean("proved");
            double valueMantissa = rs.getDouble("value_mantissa");
            long valueExponent = rs.getLong("value_exponent");
            double factorMantissa = rs.getDouble("factor_mantissa");
            long factorExponent = rs.getLong("factor_exponent");
            boolean active = rs.getBoolean("active");

            MatrixNode parentNode = matrixNodeMap.get(parentNodeId);
            BodyNode bodyNode = BodyNode.builder()
                    .parentNode(parentNode)
                    .bodyNodeId(bodyNodeId)
                    .proved(proved)
                    .value(new ScientificNumber(valueMantissa, valueExponent))
                    .factor(new ScientificNumber(factorMantissa, factorExponent))
                    .build();
            bodyNodeMap.put(id, bodyNode);
            if (active) {
                parentNode.getBodyNodes().put(bodyNodeId, bodyNode);
            } else {
                parentNode.getDeactivatedBodyNodes().put(bodyNodeId, bodyNode);
            }
        });
    }

    private void loadBodies() {
        dbTemplate.query("SELECT * FROM tmp_body", rs -> {
            int id = rs.getInt("id");
            Integer bodyNodeId = (Integer) rs.getObject("body_node");
            double valueMantissa = rs.getDouble("value_mantissa");
            long valueExponent = rs.getLong("value_exponent");
            double factorMantissa = rs.getDouble("factor_mantissa");
            long factorExponent = rs.getLong("factor_exponent");
            boolean proved = rs.getBoolean("proved");
            boolean deactivated = rs.getBoolean("deactivated");

            Body body = Body.builder()
                    .bodyNode(bodyNodeId != null ? bodyNodeMap.get(bodyNodeId) : null)
                    .value(new ScientificNumber(valueMantissa, valueExponent))
                    .factor(new ScientificNumber(factorMantissa, factorExponent))
                    .proved(proved)
                    .deactivated(deactivated)
                    .build();
            bodyMap.put(id, body);
        });
    }

    private final Map<Integer, Integer> hcnLapiIndexMap = new HashMap<>();

    private void loadHcns() {
        dbTemplate.query("SELECT * FROM tmp_hcn", rs -> {
            int id = rs.getInt("id");
            int bodyId = rs.getInt("body");
            int lapi = rs.getInt("lapi");
            double valueMantissa = rs.getDouble("value_mantissa");
            long valueExponent = rs.getLong("value_exponent");
            double factorMantissa = rs.getDouble("factor_mantissa");
            long factorExponent = rs.getLong("factor_exponent");

            Hcn hcn = Hcn.builder()
                    .body(bodyMap.get(bodyId))
                    .value(new ScientificNumber(valueMantissa, valueExponent))
                    .factor(new ScientificNumber(factorMantissa, factorExponent))
                    .build();
            hcnMap.put(id, hcn);
            hcnLapiIndexMap.put(id, lapi);
        });
    }

    private void loadLapis() {
        dbTemplate.query("SELECT * FROM tmp_lapi", rs -> {
            int primeIndex = rs.getInt("prime");
            Integer walkerId = (Integer) rs.getObject("walker");
            Double vmMantissa = (Double) rs.getObject("value_multiplier_mantissa");
            Long vmExponent = (Long) rs.getObject("value_multiplier_exponent");
            Double fmMantissa = (Double) rs.getObject("factor_multiplier_mantissa");
            Long fmExponent = (Long) rs.getObject("factor_multiplier_exponent");

            Lapi lapi = Lapi.builder()
                    .prime(lapiPrimeCenter.getPrime(primeIndex))
                    .walker(walkerId != null ? bodyMap.get(walkerId) : null)
                    .valueMultiplier(vmMantissa != null ? new ScientificNumber(vmMantissa, vmExponent) : null)
                    .factorMultiplier(fmMantissa != null ? new ScientificNumber(fmMantissa, fmExponent) : null)
                    .build();
            lapiMap.put(primeIndex, lapi);
        });
    }

    private List<Hcn> referenceIntervalHcns = new ArrayList<>();

    private void loadReferenceIntervalHcns() {
        dbTemplate.query("SELECT * FROM tmp_reference_interval_hcn ORDER BY list_position", rs -> {
            referenceIntervalHcns.add(hcnMap.get(rs.getInt("hcn")));
        });
    }

    private void loadLapiHcns() {
        dbTemplate.query("SELECT * FROM tmp_lapi_hcn ORDER BY lapi_prime, list_position", rs -> {
            int lapiPrime = rs.getInt("lapi_prime");
            int hcnId = rs.getInt("hcn");
            //lapiMap.get(lapiPrime).getHcnList().add(hcnMap.get(hcnId));
        });
    }

    private void wireMatrixNodeReferences() {
        // Load primes into matrix node indexes
        dbTemplate.query("SELECT * FROM tmp_prime WHERE matrix_node_id IS NOT NULL ORDER BY matrix_node_id, index", rs -> {
            int primeIndex = rs.getInt("index");
            int matrixNodeId = rs.getInt("matrix_node_id");
            matrixNodeMap.get(matrixNodeId).getIndexes().add(matrixPrimeCenter.getPrime(primeIndex));
        });

        // Wire prev/next and bodyList
        dbTemplate.query("SELECT * FROM tmp_matrix_node", rs -> {
            int id = rs.getInt("id");
            Integer prevId = (Integer) rs.getObject("prev_matrix_node");
            Integer nextId = (Integer) rs.getObject("next_matrix_node");
            Integer bodyListId = (Integer) rs.getObject("body_list");

            MatrixNode node = matrixNodeMap.get(id);
            node.setPrevMatrixNode(prevId != null ? matrixNodeMap.get(prevId) : null);
            node.setNextMatrixNode(nextId != null ? matrixNodeMap.get(nextId) : null);
            if (bodyListId != null) {
                Body smallest = bodyMap.get(bodyListId);
                Body walker = smallest;
                int sz = 0;
                Body largest = smallest;
                while (walker != null) { sz++; largest = walker; walker = walker.getLargerBody(); }
                node.setBodyList(BodyList.builder().smallestBody(smallest).largestBody(largest).size(sz).build());
            }
        });
    }

    private void wireBodyNodeReferences() {
        // BodyNodes are already wired to their parentNode during loadBodyNodes
    }

    private void wireBodyReferences() {
        dbTemplate.query("SELECT * FROM tmp_body", rs -> {
            int id = rs.getInt("id");
            Integer parentId = (Integer) rs.getObject("parent");
            Integer smallerId = (Integer) rs.getObject("smaller_body");
            Integer largerId = (Integer) rs.getObject("larger_body");
            Integer smallerActiveId = (Integer) rs.getObject("smaller_active_body");
            Integer largerActiveId = (Integer) rs.getObject("larger_active_body");
            Integer lastGenHcnId = (Integer) rs.getObject("last_generated_hcn");
            Integer firstHcnId = (Integer) rs.getObject("first_hcn");
            Integer firstSuperiorHcnId = (Integer) rs.getObject("first_superior_hcn");
            Integer firstDominatedHcnId = (Integer) rs.getObject("first_dominated_hcn");
            Integer dbId = (Integer) rs.getObject("db_id");

            Body body = bodyMap.get(id);
            body.setParent(parentId != null ? bodyMap.get(parentId) : null);
            body.setSmallerBody(smallerId != null ? bodyMap.get(smallerId) : null);
            body.setLargerBody(largerId != null ? bodyMap.get(largerId) : null);
            body.setSmallerHcnGenerator(smallerActiveId != null ? bodyMap.get(smallerActiveId) : null);
            body.setLargerHcnGenerator(largerActiveId != null ? bodyMap.get(largerActiveId) : null);
            body.setLastGeneratedHcn(lastGenHcnId != null ? hcnMap.get(lastGenHcnId) : null);
            body.setFirstHcn(firstHcnId != null ? hcnMap.get(firstHcnId) : null);
            body.setFirstSuperiorHcn(firstSuperiorHcnId != null ? hcnMap.get(firstSuperiorHcnId) : null);
            body.setFirstDominatedHcn(firstDominatedHcnId != null ? hcnMap.get(firstDominatedHcnId) : null);
            body.setDbId(dbId);
        });
    }

    private void wireHcnReferences() {
        hcnLapiIndexMap.forEach((id, lapiIndex) -> hcnMap.get(id).setLapi(lapiMap.get(lapiIndex)));
    }

    private void wireLapiReferences() {
        dbTemplate.query("SELECT * FROM tmp_lapi", rs -> {
            int primeIndex = rs.getInt("prime");
            Integer lowerPrime = (Integer) rs.getObject("lower_lapi");
            Integer higherPrime = (Integer) rs.getObject("higher_lapi");

            Lapi lapi = lapiMap.get(primeIndex);
            lapi.setLowerLapi(lowerPrime != null ? lapiMap.get(lowerPrime) : null);
            lapi.setHigherLapi(higherPrime != null ? lapiMap.get(higherPrime) : null);
        });
    }

    private void buildOffsprings() {
        for (Body body : bodyMap.values()) {
            if (body.getBodyNode() == null || body.getParent() == null) continue;
            if (body.isDeactivated()) {
                body.getParent().getDeactivatedOffsprings().add(body);
            } else {
                body.getParent().getOffsprings().add(body);
            }
        }
    }

    private void buildActiveBodies() {
        for (Body body : bodyMap.values()) {
            if (body.getBodyNode() == null) continue; // deleted body
            if (body.isDeactivated()) {
                body.getBodyNode().getDeactivatedBodies().add(body);
            } else {
                body.getBodyNode().getActiveBodies().add(body);
            }
        }
    }

    private int queryMaxId(String table) {
        Integer max = dbTemplate.queryForObject("SELECT MAX(id) FROM " + table, Integer.class);
        return max != null ? max : -1;
    }

    private Matrix buildMatrix() {
        Map<String, Object> row = dbTemplate.queryForMap("SELECT * FROM tmp_matrix");

        int lastTransitionId = (int) row.get("last_transition");
        int nextLapiPrime = (int) row.get("next_lapi");
        int lowestLapiPrime = (int) row.get("lowest_lapi");
        int highestLapiPrime = (int) row.get("highest_lapi");
        int lowestProvedLapi = (int) row.get("lowest_proved_lapi_within_interval");
        int provedCount = (int) row.get("proved_count");
        double provedLimitMantissa = (double) row.get("proved_limit_mantissa");
        long provedLimitExponent = (long) row.get("proved_limit_exponent");
        long totalTimeMs = (long) row.get("total_time_ms");
        long matrixMaintainTimeMs = (long) row.get("matrix_maintain_time_ms");
        long generateHcnListTimeMs = (long) row.get("generate_hcn_list_time_ms");
        boolean dbMode = (boolean) row.get("db_mode");
        long totalNanos = (long) row.get("total_nanos");
        long totalMatrixNanos = (long) row.get("total_matrix_nanos");

        Matrix.lastTransition = (TransitionNode) matrixNodeMap.get(lastTransitionId);

        Matrix matrix = Matrix.builder()
                .nextLapi(lapiMap.get(nextLapiPrime))
                .lowestLapi(lapiMap.get(lowestLapiPrime))
                .highestLapi(lapiMap.get(highestLapiPrime))
                .lowestProvedLapiWithinInterval(lowestProvedLapi)
                .provedCount(provedCount)
                .provedLimit(new ScientificNumber(provedLimitMantissa, provedLimitExponent))
                .totalTimeMs(totalTimeMs)
                .matrixMaintainTimeMs(matrixMaintainTimeMs)
                .generateHcnListTimeMs(generateHcnListTimeMs)
                .dbMode(dbMode)
                .build();

        if (dbMode) {
            Integer lastFirstHcn = dbTemplate.queryForObject("SELECT first_hcn FROM interval ORDER BY lapi DESC LIMIT 1", Integer.class);
            Integer lastSize = dbTemplate.queryForObject("SELECT size FROM interval ORDER BY lapi DESC LIMIT 1", Integer.class);
            dbInsertService.setHcnIdCounter(lastFirstHcn != null ? lastFirstHcn + lastSize - 1 : 0);
            dbInsertService.setBodyIdCounter(queryMaxId("body") + 1);
            dbInsertService.setStructuralIdCounter(queryMaxId("structural_activity") + 1);
            dbInsertService.setExtensionIdCounter(queryMaxId("extension_activity") + 1);
            dbInsertService.setHcnGenerationIdCounter(queryMaxId("hcn_generation_activity") + 1);
            dbInsertService.setSqlInsertActIdCounter(queryMaxId("sql_insert_activity") + 1);
            dbInsertService.setBodyDeletionEventIdCounter(queryMaxId("body_deletion_event") + 1);
            matrix.setDbInsertService(dbInsertService);
        }

        ActivityCenter.setTotalNanos(totalNanos);
        ActivityCenter.setTotalMatrixNanos(totalMatrixNanos);

        Integer refLapi = (Integer) row.get("reference_interval_lapi");
        if (refLapi != null) {
            Double riValueMantissa = (Double) row.get("reference_interval_value_mantissa");
            Long riValueExponent = (Long) row.get("reference_interval_value_exponent");
            Double riFactorMantissa = (Double) row.get("reference_interval_factor_mantissa");
            Long riFactorExponent = (Long) row.get("reference_interval_factor_exponent");
            Interval ri = Interval.builder()
                    .lapi(refLapi)
                    .value(new ScientificNumber(riValueMantissa, riValueExponent))
                    .factor(new ScientificNumber(riFactorMantissa, riFactorExponent))
                    .hcnList(referenceIntervalHcns)
                    .build();
            ri.setReferenceInterval(ri);
            matrix.setReferenceInterval(ri);
        }

        HcnGeneratorList.initialize(Matrix.lastTransition.getBodyList().getSmallestBody());
        return matrix;
    }
}
