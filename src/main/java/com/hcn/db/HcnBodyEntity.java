package com.hcn.db;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "hcn_body")
@Data
@NoArgsConstructor
public class HcnBodyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "parent_id")
    private HcnBodyEntity parent;

    @OneToMany(mappedBy = "parent", fetch = FetchType.EAGER)
    private List<HcnBodyEntity> offsprings = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "pip_id")
    private PrimeIndexPowerEntity pip;

    private boolean proved;

    @ManyToOne
    @JoinColumn(name = "value_id")
    private ScientificNumberEntity value;

    @ManyToOne
    @JoinColumn(name = "factor_id")
    private ScientificNumberEntity factor;

    @ManyToOne
    @JoinColumn(name = "smaller_body_id")
    private HcnBodyEntity smallerBody;

    @ManyToOne
    @JoinColumn(name = "larger_body_id")
    private HcnBodyEntity largerBody;

    @ManyToOne
    @JoinColumn(name = "last_generated_hcn_id")
    private HcnEntity lastGeneratedHcn;
}
