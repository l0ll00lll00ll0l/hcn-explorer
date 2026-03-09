package com.hcn.v3;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ScientificNumberTest {
    
    @Test
    public void testNormalization() {
        ScientificNumber num1 = new ScientificNumber(123.45, 0);
        assertEquals(1.2345, num1.getMantissa(), 0.0001);
        assertEquals(2, num1.getExponent());
        
        ScientificNumber num2 = new ScientificNumber(0.00567, 0);
        assertEquals(5.67, num2.getMantissa(), 0.01);
        assertEquals(-3, num2.getExponent());
    }
    
    @Test
    public void testMultiply() {
        ScientificNumber num1 = new ScientificNumber(2.5, 3);
        ScientificNumber num2 = new ScientificNumber(4.0, 2);
        ScientificNumber result = num1.multiply(num2);
        
        assertEquals(1.0, result.getMantissa(), 0.01);
        assertEquals(6, result.getExponent());
    }
    
    @Test
    public void testDivide() {
        ScientificNumber num1 = new ScientificNumber(8.0, 5);
        ScientificNumber num2 = new ScientificNumber(2.0, 3);
        ScientificNumber result = num1.divide(num2);
        
        assertEquals(4.0, result.getMantissa(), 0.01);
        assertEquals(2, result.getExponent());
    }
    
    @Test
    public void testCompareTo() {
        ScientificNumber num1 = new ScientificNumber(5.0, 10);
        ScientificNumber num2 = new ScientificNumber(3.0, 10);
        ScientificNumber num3 = new ScientificNumber(9.0, 9);
        
        assertTrue(num1.compareTo(num2) > 0);
        assertTrue(num1.compareTo(num3) > 0);
        assertTrue(num3.compareTo(num2) < 0);
    }
    
    @Test
    public void testPrecision() {
        ScientificNumber.setPrecision(5);
        ScientificNumber num = new ScientificNumber(1.23456789, 0);
        
        assertEquals(1.2346, num.getMantissa(), 0.0001);
        
        ScientificNumber.setPrecision(7);  // Reset to default
    }
    
    @Test
    public void testToString() {
        ScientificNumber num = new ScientificNumber(3.14159, 8);
        assertEquals("3.14159e8", num.toString());
    }

    @Test
    public void test() {
        for(int i=48; i<100; i++) {
            for (int j=3; j<i; j++ ) {
                ScientificNumber num = new ScientificNumber(i, 1);
                ScientificNumber num2 = new ScientificNumber(i, 1).divide(new ScientificNumber(j, 1)).multiply(new ScientificNumber(j, 1));
                assertEquals(0, num.compareTo(num2));
            }
        }
    }
}
