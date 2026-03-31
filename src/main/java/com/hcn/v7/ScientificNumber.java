package com.hcn.v7;

public class ScientificNumber implements Comparable<ScientificNumber> {
    private static long PRECISION1 = 100000000000L;   // 10^13
    private static long PRECISION2 = 1000000000000L;  // 10^14
    private static int DISPLAY_DECIMALS = 3;
    
    private double mantissa;
    private long exponent;
    
    public ScientificNumber(double mantissa, long exponent) {
        this.mantissa = mantissa;
        this.exponent = exponent;
        normalize();
    }
    
    public static void setPrecision(int precision) {
        PRECISION1 = (int) Math.pow(10, precision);
        PRECISION2 = (int) Math.pow(10, precision + 1);
    }
    
    public static void setDisplayDecimals(int decimals) {
        DISPLAY_DECIMALS = decimals;
    }
    
    public static int getDisplayDecimals() {
        return DISPLAY_DECIMALS;
    }
    
    private void normalize() {
        if (mantissa == 0) {
            exponent = 0;
            return;
        }
        
        double abs = Math.abs(mantissa);
        
        if (abs >= 10.0) {
            int shift = 0;
            while (abs >= 10.0) {
                abs /= 10.0;
                shift++;
            }
            mantissa /= Math.pow(10, shift);
            exponent += shift;
        } else if (abs < 1.0) {
            int shift = 0;
            while (abs < 1.0) {
                abs *= 10.0;
                shift++;
            }
            mantissa *= Math.pow(10, shift);
            exponent -= shift;
        }
    }
    

    public ScientificNumber multiply(ScientificNumber other) {
        return new ScientificNumber(this.mantissa * other.mantissa, this.exponent + other.exponent);
    }
    
    public ScientificNumber divide(ScientificNumber other) {
        return new ScientificNumber(this.mantissa / other.mantissa, this.exponent - other.exponent);
    }
    
    @Override
    public int compareTo(ScientificNumber other) {
        if (this.exponent != other.exponent) {
            return Long.compare(this.exponent, other.exponent);
        }
        
        // Fuzzy comparison with two precision levels
        double m1_p1 = Math.round(this.mantissa * PRECISION1) / (double) PRECISION1;
        double m2_p1 = Math.round(other.mantissa * PRECISION1) / (double) PRECISION1;
        
        int p1;
        if (m1_p1 > m2_p1) {
            p1 = 1;
        } else if (m1_p1 < m2_p1) {
            p1 = -1;
        } else {
            p1 = 0;
        }
        
        double m1_p2 = Math.round(this.mantissa * PRECISION2) / (double) PRECISION2;
        double m2_p2 = Math.round(other.mantissa * PRECISION2) / (double) PRECISION2;
        
        int p2;
        if (m1_p2 > m2_p2) {
            p2 = 1;
        } else if (m1_p2 < m2_p2) {
            p2 = -1;
        } else {
            p2 = 0;
        }
        
        // If both agree, return that result
        if (p1 == p2) {
            return p1;
        }
        // If one is 0 (equal), consider them equal
        if (p1 == 0 || p2 == 0) {
            return 0;
        }
        // Shouldn't happen, but return 0 as safe default
        return 0;
    }
    
    @Override
    public String toString() {
        // Calculate scientific notation length
        String format = String.format("%%.%dfe%%d", DISPLAY_DECIMALS);
        String scientificForm = String.format(format, mantissa, exponent);
        int scientificLength = scientificForm.length();
        
        // Try to represent as long if exponent is small enough
        if (exponent >= 0 && exponent <= 18) {
            long value = Math.round(mantissa * Math.pow(10, exponent));
            String longForm = String.valueOf(value);
            
            // Use long form if it's not longer than scientific form
            if (longForm.length() <= scientificLength) {
                return longForm;
            }
        }
        
        return scientificForm;
    }
    
    public double getMantissa() {
        return mantissa;
    }
    
    public long getExponent() {
        return exponent;
    }
    
    public boolean isBiggerThan(ScientificNumber other) {
        return this.compareTo(other) > 0;
    }
    
    public boolean isNotBiggerThan(ScientificNumber other) {
        return this.compareTo(other) <= 0;
    }
    
    public boolean isSmallerThan(ScientificNumber other) {
        return this.compareTo(other) < 0;
    }
    
    public boolean isNotSmallerThan(ScientificNumber other) {
        return this.compareTo(other) >= 0;
    }
    
    public boolean isEqualTo(ScientificNumber other) {
        return this.compareTo(other) == 0;
    }
}
