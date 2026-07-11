# Highly Composite Numbers (HCN) Explorer

A Spring Boot application for finding and analyzing Highly Composite Numbers using prime factorization.

## What are Highly Composite Numbers?

Highly Composite Numbers (HCNs) are positive integers with more divisors than any smaller positive integer. For example: 1, 2, 4, 6, 12, 24, 36, 48, 60, 120...

## Features

- Efficient HCN detection using prime factorization matrix
- Web UI for real-time visualization
- Multiple display formats (full notation and compact)
- Optional value and divisor count display
- Active count tracking for optimization
- Automatic filtering of inferior candidates

## Technology Stack

- Java 17+
- Spring Boot
- Thymeleaf
- Maven

## Running the Application

```bash
mvn spring-boot:run
```

Access the application at: http://localhost:9090

## Algorithm

The application uses a matrix-based approach where:
- Each row represents a prime number (p0=2, p1=3, p2=5, ...)
- Each cell contains powers of that prime
- HCNs follow the rule: if prime pᵢ has exponent n, all primes p₀...pᵢ₋₁ must have exponent ≥ n
- Candidates are filtered by divisor count to eliminate inferior numbers

## DB Mode

When DB mode is enabled, the application persists all generated HCNs, bodies, and intervals into a PostgreSQL database for later analysis.

### DB Insertion Strategy

The goal is to minimize total time spent on database insertion by keeping the DB busy as much as possible and avoiding large idle gaps between inserts.

#### Feeding the service

The matrix computation runs on the main thread and produces `Interval` objects one by one as lapis are processed. Each completed `Interval` is submitted to `DbInsertService` via `submit(Interval)`, which places it on an internal `LinkedBlockingQueue`. This decouples matrix computation from DB insertion — the main thread never blocks waiting for the DB.

#### Interval consumer

A single dedicated consumer thread drains the interval queue. For each `Interval` it:
1. Assigns DB ids to any new bodies
2. Appends rows to 3 independent `StringBuilder` buffers: one for `body`, one for `hcn`, one for `interval`
3. Increments the corresponding row counter (`bodyCount`, `hcnCount`, `intervalCount`)
4. Checks each counter independently against a threshold

#### Buffer threshold and flush

Each of the 3 buffers is monitored independently. When a buffer's row counter reaches the threshold, its current SQL string is submitted to that table's dedicated insert queue and the buffer is immediately reset. This means:
- The 3 buffers can hit their thresholds at different times and flush independently
- The interval consumer thread is never blocked — it submits to the insert queue and continues

#### 3 parallel insert queues

There are 3 separate `LinkedBlockingQueue<String>` instances, one per table (`body`, `hcn`, `interval`), each drained by its own dedicated consumer thread. Each consumer executes insert SQL strings sequentially, so there is never concurrent insertion into the same table. However since the 3 queues run on independent threads, inserts into `body`, `hcn`, and `interval` can and do overlap freely.

No foreign key constraints exist on these tables — ids are assigned by the application — so the 3 tables are fully independent and parallel insertion is safe.

#### Final flush

When matrix computation completes, `finalFlush(lapiId)` is called. It:
1. Waits for the interval consumer to finish processing all submitted intervals (up to `lapiId`)
2. Submits the remaining content of all 3 buffers to their respective insert queues
3. Waits for all 3 insert queues to drain completely

```
Main thread          Interval consumer        Insert queue consumers
───────────          ─────────────────        ──────────────────────
submit(interval) ──► process(interval)
                     append to buffers
                     bodyCount >= threshold ──► body queue ──► execute INSERT body
                     hcnCount >= threshold  ──► hcn queue  ──► execute INSERT hcn
                     intervalCount >= thr.  ──► intv queue ──► execute INSERT interval
...
finalFlush() ──────► wait lastLapi
                     flush remaining ────────► all 3 queues drain
```
