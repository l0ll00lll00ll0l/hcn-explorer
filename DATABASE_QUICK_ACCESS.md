# Database Quick Access

## Connection

- Host: `localhost`
- Port: `5433`
- User: `hcn`
- Password: `hcn`
- Default DB: `hcn_1` (may vary, pattern is `hcn_N`)

## psql command template

```bash
psql -h localhost -p 5433 -U hcn -d hcn_1 -c "SQL HERE" -W <<< "hcn"
```

## Tables

### `body_lifecycle`
| Column | Type |
|---|---|
| `body_id` | integer PK |
| `first_hcn_lapi` | integer |
| `first_superior_hcn_lapi` | integer |
| `first_dominated_hcn_lapi` | integer |

### `body`
| Column | Type |
|---|---|
| `id` | integer PK |
| `head` | integer[] |
| `tail` | integer[] |

### `hcn`
| Column | Type |
|---|---|
| `id` | bigint PK |
| `body` | integer |
| `lapi` | integer |

### `interval`
| Column | Type |
|---|---|
| `lapi` | integer PK |
| `value_mantissa` | double |
| `value_exponent` | bigint |
| `factor_mantissa` | double |
| `factor_exponent` | bigint |
| `first_hcn` | bigint |
| `size` | integer |
| `reference_interval` | integer |

### `structural_activity` / `matrix_main_activity` / `sql_insert_activity`
Activity tables — see `SqlTable` enum for full list.

## Useful queries

### Max diff between first_hcn_lapi and first_dominated_hcn_lapi
```sql
SELECT body_id, first_hcn_lapi, first_dominated_hcn_lapi,
       (first_dominated_hcn_lapi - first_hcn_lapi) AS diff
FROM body_lifecycle
WHERE first_hcn_lapi IS NOT NULL AND first_dominated_hcn_lapi IS NOT NULL
ORDER BY diff DESC LIMIT 5;
```

### List all databases
```bash
psql -h localhost -p 5433 -U hcn -d postgres -c "\l" -W <<< "hcn"
```

### List all tables in a DB
```bash
psql -h localhost -p 5433 -U hcn -d hcn_1 -c "\dt" -W <<< "hcn"
```
