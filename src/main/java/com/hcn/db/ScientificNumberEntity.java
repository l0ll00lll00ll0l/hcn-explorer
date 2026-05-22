package com.hcn.db;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Table(name = "scientific_number")
@Data
@NoArgsConstructor
public class ScientificNumberEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private double mantissa;
    private long exponent;

    public ScientificNumberEntity(double mantissa, long exponent) {
        this.mantissa = mantissa;
        this.exponent = exponent;
    }
}
