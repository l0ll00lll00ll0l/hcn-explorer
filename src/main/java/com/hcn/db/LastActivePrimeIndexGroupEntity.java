package com.hcn.db;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "last_active_prime_index_group")
@Data
@NoArgsConstructor
public class LastActivePrimeIndexGroupEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int lastActivePrimeIndex;

    @ManyToOne
    @JoinColumn(name = "walker_body_id")
    private HcnBodyEntity walkerBody;

    @ManyToOne
    @JoinColumn(name = "lower_lapi_group_id")
    private LastActivePrimeIndexGroupEntity lowerLapiGroup;

    @ManyToOne
    @JoinColumn(name = "higher_lapi_group_id")
    private LastActivePrimeIndexGroupEntity higherLapiGroup;

    @ManyToMany
    @JoinTable(
            name = "lapi_hcn_list",
            joinColumns = @JoinColumn(name = "lapi_group_id"),
            inverseJoinColumns = @JoinColumn(name = "hcn_id")
    )
    @OrderColumn(name = "order_in_list")
    private List<HcnEntity> hcnList = new ArrayList<>();
}
