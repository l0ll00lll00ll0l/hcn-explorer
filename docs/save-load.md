# Matrix Save / Load

## Overview

Save and load persist the full in-memory matrix state to a PostgreSQL database so computation can be resumed later. All temporary tables are prefixed `tmp_`.

---

## Save

### Step 1 — Collect active matrix nodes and their body nodes (`buildActiveMatrixNodeSet`)

Walk the full matrix node chain from the first node (the one with no `prevMatrixNode`) to `lastTransition`. For each node:
- Assign a matrix node tempId
- Write all active body nodes (those currently in the node's `bodyNodes` map) to `bodyNodeSb` with their parent matrix node tempId already set

After this step, all active matrix nodes and all active body nodes have tempIds and are written to `bodyNodeSb`.

### Step 2 — Collect lapis (`buildLapiList`)

Collect `nextLapi` plus the full lapi chain from `lowestLapi` to `highestLapi`.

### Step 3 — Assign all temp ids (`prepareForSave`)

This is the most critical step. The goal is that **every object that will be written to the DB has a tempId assigned before any SQL is built**. No tempId should be assigned lazily during SQL building.

#### 3a — Matrix nodes
All matrix nodes in the chain get a tempId assigned first, in chain order.

#### 3b — Bodies from matrix node body lists
For each matrix node, walk its `bodyList` from smallest to largest body. For each body:
- Assign a body tempId
- Assign a body node tempId (which also requires the body node's parent matrix node to already have a tempId — guaranteed by 3a)
- If the body is deactivated, also walk its full parent chain and assign tempIds to all parent bodies and their body nodes

#### 3c — Bodies from lapis and referenceInterval
For each HCN in each lapi's hcn list, and for each HCN in the referenceInterval hcn list:
- If the body doesn't have a tempId yet, assign one and walk its full parent chain

#### Known gap
Body nodes that are no longer in any matrix node's `bodyNodes` map (removed/inactive body nodes) are only reachable via bodies discovered in 3b/3c. If such a body is only discovered lazily during SQL building (via parent/smaller/larger references in `appendBodyValues`), its body node gets a tempId assigned at that point — but the body node's parent matrix node may not have a tempId yet if it was never visited in 3a.

**Intended fix**: split the loop in `prepareForSave` so all matrix node tempIds are assigned in a first pass before any body tempId assignment begins. Additionally, ensure all bodies reachable via any reference (parent, smaller, larger, smallerActive, largerActive) are discovered during `prepareForSave` rather than lazily during SQL building.

### Step 4 — Build and execute SQL inserts (controller)

Executed in this order:
1. `buildLapiInsert` — tmp_lapi
2. `buildBodyInsert` — tmp_body (may discover new bodies via parent/smaller/larger references)
3. `buildBodyNodeInsert` — tmp_body_node (uses tempIds assigned during body collection)
4. `buildMatrixNodeInsert` — tmp_matrix_node
5. `buildPrimeInsert` — tmp_prime
6. `buildLapiHcnInsert` — tmp_lapi_hcn
7. `buildHcnInsert` — tmp_hcn
8. `buildReferenceIntervalHcnInsert` — tmp_reference_interval_hcn
9. `buildMatrixInsert` — tmp_matrix

---

## Load

### Step 1 — Load prime centers

Two `PrimeCenter` instances are created:
- `lapiPrimeCenter` — seeded up to `next_lapi` prime index
- `matrixPrimeCenter` — seeded up to the max prime index found in `tmp_prime` for matrix nodes

### Step 2 — Load objects (no wiring yet)

Each object type is loaded and placed into a map keyed by its tempId:
- `matrixNodeMap` — `ApiNode` or `TransitionNode` per row in `tmp_matrix_node`
- `bodyNodeMap` — `BodyNode` per row in `tmp_body_node`, with `parentNode` wired immediately (matrix nodes are already in `matrixNodeMap`)
- `bodyMap` — `Body` per row in `tmp_body`
- `hcnMap` — `Hcn` per row in `tmp_hcn`
- `lapiMap` — `Lapi` per row in `tmp_lapi`

Active body nodes are also added to their parent matrix node's `bodyNodes` map during this step.

### Step 3 — Wire references

- Matrix nodes: prime indexes loaded, `prev`/`next`/`bodyList` wired
- Bodies: `parent`, `smaller`, `larger`, `smallerActive`, `largerActive`, `lastGeneratedHcn`, `firstHcn`, `firstSuperiorHcn`, `firstDominatedHcn`, `dbId` wired
- Lapis: `lowerLapi`, `higherLapi` wired; hcn lists populated from `tmp_lapi_hcn`
- Reference interval hcns loaded from `tmp_reference_interval_hcn`

### Step 4 — Rebuild derived collections

- `buildOffsprings` — for every non-deactivated body with a parent, add to parent's `offsprings` list
- `buildActiveBodies` — for every non-deactivated body, add to its body node's `activeBodies` list

### Step 5 — Build matrix

Restore all scalar fields from `tmp_matrix`. If `dbMode` is true, restore all DB id counters:
- `hcnIdCounter` — from last interval's `first_hcn + size - 1`
- `bodyIdCounter` — from `MAX(body.id) + 1`
- `structuralIdCounter`, `extensionIdCounter`, `hcnGenerationIdCounter`, `sqlInsertActIdCounter` — from `MAX(id) + 1` of respective activity tables

Restore `ActivityCenter.totalNanos` from `total_nanos`.

---

## Tables

| Table | Contents |
|-------|----------|
| `tmp_matrix` | Single row with all matrix scalar fields |
| `tmp_matrix_node` | One row per ApiNode or TransitionNode in the chain |
| `tmp_prime` | One row per prime index, linked to matrix node or lapi |
| `tmp_body_node` | One row per BodyNode object |
| `tmp_body` | One row per Body object |
| `tmp_hcn` | One row per Hcn object |
| `tmp_lapi` | One row per Lapi object |
| `tmp_lapi_hcn` | Junction table linking lapis to their hcn lists |
| `tmp_reference_interval_hcn` | Ordered hcn list of the reference interval |
