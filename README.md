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
