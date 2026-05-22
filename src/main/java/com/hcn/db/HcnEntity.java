package com.hcn.db;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Table(name = "hcn")
@Data
@NoArgsConstructor
public class HcnEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "body_id")
    private HcnBodyEntity body;

    private int lastActivePrime;

    @ManyToOne
    @JoinColumn(name = "value_id")
    private ScientificNumberEntity value;

    @ManyToOne
    @JoinColumn(name = "factor_id")
    private ScientificNumberEntity factor;
}
