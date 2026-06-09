# ScientificNumber

## Purpose

`ScientificNumber` represents very large (or very small) numbers using scientific notation: `mantissa × 10^exponent`, where `1.0 ≤ |mantissa| < 10.0`. This is essential for the HCN algorithm because Highly Composite Numbers grow extremely fast — their values and divisor counts quickly exceed what `long` or `double` can represent alone.

## Structure

- `mantissa` (double): The significant digits, always normalized to `[1.0, 10.0)`.
- `exponent` (long): The power of 10. Using `long` allows representing numbers with exponents up to ~9.2×10¹⁸ digits.

## Key Operations

### Construction & Normalization

```java
new ScientificNumber(mantissa, exponent)
```

On creation, the number is automatically normalized so that `1.0 ≤ |mantissa| < 10.0`. For example, `ScientificNumber(25.0, 3)` becomes `mantissa=2.5, exponent=4`.

### Multiplication

```java
a.multiply(b) → new ScientificNumber(a.mantissa * b.mantissa, a.exponent + b.exponent)
```

Result is automatically normalized. This is the primary arithmetic operation used in HCN generation — computing values and divisor counts from multipliers.

### Division

```java
a.divide(b) → new ScientificNumber(a.mantissa / b.mantissa, a.exponent - b.exponent)
```

Used when removing a prime factor (e.g., during FPG reactivation).

### Comparison (Fuzzy)

The `compareTo` method uses a **two-level fuzzy comparison** to handle floating-point precision issues:

1. Round both mantissas to PRECISION1 (10¹³) and compare.
2. Round both mantissas to PRECISION2 (10¹⁴) and compare.
3. If both levels agree → return that result.
4. If they disagree (one says equal, other says different) → consider them equal.

This prevents false ordering from accumulated floating-point rounding errors, which is critical since the algorithm relies heavily on correct ordering of values and factors.

### Convenience Methods

- `isBiggerThan(other)` — `compareTo > 0`
- `isNotBiggerThan(other)` — `compareTo <= 0`
- `isSmallerThan(other)` — `compareTo < 0`
- `isNotSmallerThan(other)` — `compareTo >= 0`

## Display

`toString()` intelligently chooses between:
- **Long form** (e.g., `720`) when the number fits in a `long` and is shorter than scientific notation.
- **Scientific form** (e.g., `1.576e16`) otherwise, using configurable decimal places (`DISPLAY_DECIMALS`).

## Configuration

- `setPrecision(int)` — Adjust fuzzy comparison precision (default: 13 digits).
- `setDisplayDecimals(int)` — Adjust display format (default: 3 decimals).

## Usage in HCN Algorithm

- **HCN value**: The actual numeric value of the highly composite number candidate (product of prime powers).
- **HCN factor (divisor count)**: The number of divisors (product of `(power + 1)` for each prime).
- **Multipliers**: Ratios between bodies for computing HCN values from reference HCNs.
- **Prime values**: Prime numbers used as multipliers when extending to the next last-active-prime-index.
