package com.hcn.db;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Table(name = "matrix")
@Data
@NoArgsConstructor
public class MatrixEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "application_id")
    private HcnApplication application;

    @ManyToOne
    @JoinColumn(name = "last_active_prime_index")
    private ActivePrimeIndexEntity lastActivePrimeIndex;

    @ManyToOne
    @JoinColumn(name = "lowest_lapi_group_id")
    private LastActivePrimeIndexGroupEntity lowestLapiGroup;

    @ManyToOne
    @JoinColumn(name = "highest_lapi_group_id")
    private LastActivePrimeIndexGroupEntity highestLapiGroup;

    @ManyToOne
    @JoinColumn(name = "next_lapi_group_id")
    private LastActivePrimeIndexGroupEntity nextLapiGroup;

    @ManyToOne
    @JoinColumn(name = "proved_limit_id")
    private ScientificNumberEntity provedLimit;

    private int provedCount;
    private int lastProvedPrimeIndex;
    private int lowestProvedLapiWithinInterval;
}
