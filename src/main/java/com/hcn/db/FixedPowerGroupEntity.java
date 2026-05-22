package com.hcn.db;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "fixed_power_group")
@Data
@NoArgsConstructor
public class FixedPowerGroupEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "value_id")
    private ScientificNumberEntity value;

    @ManyToOne
    @JoinColumn(name = "factor_id")
    private ScientificNumberEntity factor;

    @ManyToOne
    @JoinColumn(name = "parent_prime_index")
    private ActivePrimeIndexEntity parentPrimeIndex;

    @ManyToOne
    @JoinColumn(name = "offspring_prime_index")
    private ActivePrimeIndexEntity offspringPrimeIndex;

    @OneToMany(mappedBy = "memberOfFixedPowerGroup", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @OrderBy("fixedPowerGroupOrder")
    private List<ActivePrimeIndexEntity> fixedPowerGroup = new ArrayList<>();
}
