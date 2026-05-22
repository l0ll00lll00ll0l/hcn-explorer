package com.hcn.db;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "prime_index_power")
@Data
@NoArgsConstructor
public class PrimeIndexPowerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "prime_index")
    private ActivePrimeIndexEntity primeIndex;

    private int power;
    private boolean proved;

    @OneToMany(mappedBy = "pip", fetch = FetchType.EAGER)
    private List<HcnBodyEntity> activeHcnBodies = new ArrayList<>();

    public PrimeIndexPowerEntity(ActivePrimeIndexEntity primeIndex, int power, boolean proved) {
        this.primeIndex = primeIndex;
        this.power = power;
        this.proved = proved;
    }
}
