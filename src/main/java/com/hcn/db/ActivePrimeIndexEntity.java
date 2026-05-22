package com.hcn.db;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "active_prime_index")
@Data
@NoArgsConstructor
public class ActivePrimeIndexEntity {

    @Id
    private int index;

    @OneToMany(mappedBy = "primeIndex", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("power")
    private List<PrimeIndexPowerEntity> pips = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "smallest_body_id")
    private HcnBodyEntity smallestBody;

    private int bodyListSize;

    @ManyToOne
    @JoinColumn(name = "next_active_prime_index")
    private ActivePrimeIndexEntity nextActivePrimeIndex;

    @ManyToOne
    @JoinColumn(name = "parent_active_prime_index")
    private ActivePrimeIndexEntity parentActivePrimeIndex;

    @ManyToOne
    @JoinColumn(name = "offspring_fixed_power_group_id")
    private FixedPowerGroupEntity offspringFixedPowerGroup;

    @ManyToOne
    @JoinColumn(name = "parent_fixed_power_group_id")
    private FixedPowerGroupEntity parentFixedPowerGroup;

    @ManyToOne
    @JoinColumn(name = "member_of_fixed_power_group_id")
    private FixedPowerGroupEntity memberOfFixedPowerGroup;

    private Integer fixedPowerGroupOrder;

    public ActivePrimeIndexEntity(int index) {
        this.index = index;
    }
}
