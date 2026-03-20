# HcnFilter Deep Analysis

## Purpose

HcnFilter maintains a `TreeMap<Integer, ArrayList<Hcn>>` where:
- **Key** = `lastActivePrime` (the level — how many primes the HCN uses)
- **Value** = sorted list of HCNs at that level, ordered by ascending value

The **invariant** each level should maintain:
> Within a level, HCNs are sorted by value (ascending), and each successive HCN must have a strictly higher factor than the previous one.
> In other words: `value[i] < value[i+1]` AND `factor[i] < factor[i+1]`

An HCN is "inferior" (should be denied) if there exists another HCN with:
- **smaller or equal value** AND **greater or equal factor** (dominates it)

The max level list (`filter.lastKey()`) is what `getMaxLevelHcnList()` returns — this is the "proved queue" from which `proveNextHcn()` takes the first element.

---

## Data Structure Visualization

```
filter = TreeMap {
  level 14: [hcn_a(v=100,f=500), hcn_b(v=200,f=800), hcn_c(v=300,f=1200)]
  level 15: [hcn_a, hcn_b, hcn_c, hcn_d(v=250,f=900)]  ← includes lower level entries + own
  level 16: [... includes level 15 entries + own ...]
}
```

Key design: higher levels INHERIT entries from lower levels. So level 16 contains everything from level 15 plus level-16-specific HCNs.

---

## When is each method called?

```
Matrix.proveNextHcn():
│
├─ extendMatrix succeeded?
│   YES → filterAgain(newHcns)          ← new bodies created, need to merge into existing filter
│
├─ while (maxLevelList.size == 1):
│   └─ filter(newHcns)                  ← expanding search range, fresh batch
│      └─ lowLimitUpdate(lowLimit)      ← cleanup old entries
│
└─ rmoveFirst()                         ← remove the proved HCN from the list
```

### filter() — called in the "expand range" while-loop
- Receives HCNs from a NEW range `[lowLimit, upperLimit]`
- HCNs arrive sorted by value (TreeSet)
- Assumes they are APPENDED to existing levels (all new values > existing values)
- Simple: just check last element in level, append if factor is bigger

### filterAgain() — called after extendMatrix
- Receives HCNs that may INTERLEAVE with existing entries
- New bodies were created, so new HCNs may have values BETWEEN existing ones
- Must insert at correct position and remove displaced entries
- More complex: binary-search-like insertion + cleanup

---

## filter() Flowchart

```
for each hcn (sorted by ascending value):
│
├─ level = hcn.lastActivePrime
│
├─ Level exists in filter?
│   │
│   NO ──┐
│        ├─ Previous level (level-1) exists?
│        │   │
│        │   YES → lastFromPrevious = previousLevel.last()
│        │   │     hcn.factor <= lastFromPrevious.factor?
│        │   │     │
│        │   │     YES → DENY (checkDenyTrigger) ← hcn is inferior
│        │   │     │
│        │   │     NO ──┐
│        │   │          │
│        │   NO ────────┤
│        │              │
│        │              ▼
│        │   Create new level
│        │   Copy ALL entries from level-1 into new level    ← ⚠️ ISSUE A
│        │   Append hcn
│        │   DONE
│        │
│   YES ─┤
│        │
│        ▼
│   lastInLevel = currentLevel.last()
│   hcn.factor <= lastInLevel.factor?
│   │
│   YES → DENY (checkDenyTrigger)
│   │
│   NO → Append hcn to currentLevel
│        │
│        ▼
│        Forward propagate to higher levels:
│        for each higherLevel (level+1, level+2, ...):
│            lastInHigher.factor < hcn.factor?
│            YES → append hcn to higherLevel
│            NO  → STOP propagation
```

### ⚠️ ISSUE A: Level creation only copies from level-1
If level 14 exists and level 16 is being created, it only looks at level 15.
If level 15 doesn't exist, level 16 gets created empty (no inheritance).
This could miss entries from level 14.

### ⚠️ ISSUE B: Deny check only against LAST element
When checking if hcn should be denied at an existing level, it only compares against the LAST element.
Since HCNs arrive sorted by value, this works IF the invariant holds (last = highest value = highest factor).
But if the invariant is broken, this check is insufficient.

### ⚠️ ISSUE C: Forward propagation uses strict > for factor
```java
if (hcn.getFactor().isBiggerThan(lastInHigher.getFactor()))
```
Equal factors do NOT propagate. So if level 15 has an HCN with factor=3317760,
and level 16's last entry also has factor=3317760, the level-15 entry won't propagate to level 16.
But level 16 can independently add its own entry with the same factor.
**This is the bug we observed!**

---

## filterAgain() Flowchart

```
for each hcn (sorted by ascending value):
│
├─ level = hcn.lastActivePrime
│
├─ Level exists in filter?
│   │
│   NO → handleNewLevelForFilterAgain()
│   │    │
│   │    ├─ Previous level exists?
│   │    │   YES → for each prevHcn in previousLevel:
│   │    │         prevHcn.value <= hcn.value AND prevHcn.factor >= hcn.factor?
│   │    │         YES → DENY → return false
│   │    │
│   │    ├─ Create new level
│   │    ├─ Copy ALL from level-1
│   │    └─ return true
│   │
│   YES (or after new level created) ─┐
│                                      │
│   ▼                                  ▼
│   handleExistingLevelForFilterAgain()
│   │
│   ├─ Scan currentLevel BACKWARDS from end:
│   │   Find first existingHcn where existingHcn.value <= hcn.value
│   │   │
│   │   Found? → existingHcn.factor >= hcn.factor?
│   │            YES → DENY → return false        ← hcn is dominated
│   │            NO  → break (proceed to insert)
│   │
│   ├─ findInsertIndex (first entry with value > hcn.value)
│   │   Insert hcn at that index
│   │
│   ├─ Check entries AFTER insertIndex:                    ← ⚠️ ISSUE D
│   │   while next.factor <= hcn.factor:
│   │       REMOVE next (it's now dominated by hcn)
│   │       checkDenyTrigger(removed, hcn)
│   │
│   └─ return true
│
│   ▼
│   forwardPropagateForFilterAgain()
│   │
│   for each higherLevel (level+1, level+2, ...):
│   │
│   ├─ isBlockedByExistingHcn?                             ← ⚠️ ISSUE E
│   │   for each higherHcn in higherLevel:
│   │       higherHcn.value <= hcn.value AND higherHcn.factor >= hcn.factor?
│   │       YES → BLOCKED, stop propagation
│   │
│   ├─ Insert hcn at correct position (by value)
│   │
│   └─ Remove dominated entries after insert position
│       (same logic as handleExistingLevel)
```

### ⚠️ ISSUE D: Backward scan breaks too early
```java
for (int i = currentLevel.size() - 1; i >= 0; i--) {
    if (existingHcn.getValue().isNotBiggerThan(hcn.getValue())) {
        if (existingHcn.getFactor().isNotSmallerThan(hcn.getFactor())) {
            DENY
        }
        break;  ← only checks ONE existing entry!
    }
}
```
It finds the first entry with value <= hcn.value and checks only THAT one.
But what if there's an entry further back with smaller value but higher factor that also dominates?
If the invariant holds (ascending value → ascending factor), this is fine.
But if the invariant is broken, this misses dominators.

### ⚠️ ISSUE E: isBlockedByExistingHcn scans ALL entries
Unlike the backward scan in handleExisting, this checks every entry in the higher level.
Inconsistent approach — one is thorough, the other is not.

---

## THE BUG — Detailed Trace

From the logs:
```
FILTER-ADD newLevel: [p0^10, p1^4, p2^3, p3^2, p4^2, p5^2]|14  v: 2.127e26  f: 3041280
FILTER-ADD newLevel: [p0^8, p1^4, p2^3, p3^2, p4^2, p5^1]|15   v: 2.168e26  f: 3317760   ← CORRECT HCN #245
FILTER-ADD existing: [p0^8, p1^4, p2^2, p3^2, p4^1, p5^1]|16   v: 2.326e26  f: 3317760   ← WRONG, same factor!
```

### What happened:

1. `|14` entry added to new level 14 with f=3041280
2. `|15` entry added to new level 15 with f=3317760
   - Level 15 created, copies from level 14, adds `|15` entry
   - Forward propagation: does level 16 exist?
   - If YES: check if `|15`'s factor (3317760) > last in level 16 → uses strict `>` → equal doesn't propagate
   - If NO: nothing to propagate to

3. `|16` entry arrives with f=3317760 (SAME factor as `|15`)
   - Level 16 exists already (it was created earlier)
   - Check: `|16`.factor <= lastInLevel.factor?
   - Last in level 16 has factor < 3317760 → so `|16` is NOT denied → gets added
   - **But `|15` with same factor and SMALLER value should have been in level 16 and would have blocked `|16`!**
   - `|15` never made it to level 16 because propagation uses strict `>`

### Root cause: TWO interacting problems

**Problem 1**: `filter()` propagation uses strict `>`:
```java
if (hcn.getFactor().isBiggerThan(lastInHigher.getFactor()))  // strict >
```
So equal-factor entries don't propagate upward.

**Problem 2**: Level creation only copies from level-1 AT CREATION TIME.
If level 15 gets a new entry AFTER level 16 was created, level 16 never sees it.

---

## Conceptual Model — What SHOULD happen

The filter should maintain a simple invariant across ALL levels:

> An HCN is valid (not inferior) if and only if NO other HCN exists with
> smaller-or-equal value AND greater-or-equal factor.

The level structure means: an HCN at level N is visible to all levels >= N.
So level 16's list should be the UNION of all valid HCNs from levels 0..16,
filtered to maintain the ascending-value-ascending-factor invariant.

### The fix should ensure:
1. When a new entry is added to level N, it propagates to ALL levels > N (using >= for factor, or handling equal factors: smaller value wins)
2. OR: abandon the "copy from previous level" approach and instead always build higher levels as proper supersets
3. Equal-factor HCNs: the one with SMALLER value should win (it dominates the other)

---

## lowLimitUpdate() Analysis

```
for each level (except max level):
    if last entry's value < lowLimit:
        remove entire level
    else:
        remove all entries except last where value < lowLimit
```

This is cleanup — removes entries that are below the current search range.
Seems correct but aggressive: removing non-max levels entirely could lose information
if those levels later need to receive propagated entries.

---

## rmoveFirst()

```java
filter.get(filter.lastKey()).remove(0);
```

Removes the first (smallest value) entry from the max level.
This is the proved HCN being consumed.
**No cleanup of lower levels** — the proved HCN might still exist in lower level lists.
This is probably fine since lower levels are only used for deny checks, not for proving.

---

## Summary of Issues

| # | Issue | Severity | Location |
|---|-------|----------|----------|
| 1 | Equal-factor entries don't propagate (strict >) | **HIGH — THE BUG** | filter() propagation |
| 2 | Level creation only copies from level-1, not retroactively updated | HIGH | filter() new level |
| 3 | filter() deny check only against last element | Medium | filter() existing level |
| 4 | filterAgain() backward scan breaks after first match | Medium | handleExistingLevelForFilterAgain |
| 5 | Inconsistent blocking logic between filter/filterAgain | Low | various |
| 6 | rmoveFirst doesn't clean lower levels | Low | rmoveFirst |
| 7 | lowLimitUpdate skips max level entirely | Low | lowLimitUpdate |
