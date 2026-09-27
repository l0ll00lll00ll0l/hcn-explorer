package com.hcn.newCore;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

@Getter
@Setter
@Builder
@Slf4j
public class RecorderBody {

    private Body body;
    private RecorderBody previousRecorder;
    private RecorderBody nextRecorder;
    private final List<HunterBody> hunters = new ArrayList<>();
    private int intervalLapiDistance;
    @Builder.Default
    private Hcn firstDominatedHcn = null;
    @Builder.Default
    private Integer firstSuperiorHcn = null;
    private RecorderBody prayBody = null;

    public ScientificNumber getCurrentFactor() {
            return getHcnFactor(Prime.getPrimeByLapiDistance(intervalLapiDistance));
    }

    public Hcn getCurrentHcn() {
        return generateHcn(Prime.getPrimeByLapiDistance(intervalLapiDistance));
    }

    public Hcn generateRecorderHcn() {
        int requestedHighestLapiDiff = intervalLapiDistance;
        if (RecorderList.isCurrentRecorderFirstRecorder()) {
            requestedHighestLapiDiff--;
        }
        Prime hcnProducerPrime = Prime.getPrimeByLapiDistance(requestedHighestLapiDiff);
        return Hcn.builder().recorderBody(this).lastActivePrime(hcnProducerPrime).value(getHcnValue(hcnProducerPrime)).factor(getHcnFactor(hcnProducerPrime)).build();
    }

    public List<HunterHcn> generateSmallerHunterHcns(Hcn recorderHcn) {

        TreeSet<HunterHcn> sortedHunterHcns = new TreeSet<>();
        hunters.forEach(hunterBody -> {
            HunterHcn hunterHcn = hunterBody.getProcessingHunterHcn();

            if (hunterHcn.getValue().isSmallerThan(recorderHcn.getValue())) {
                log.debug(" hunterBody ADDED: {}", hunterBody);
                sortedHunterHcns.add(hunterHcn);
            }
        });
        sortedHunterHcns.forEach(hunterHcn -> {
            hunters.remove(hunterHcn.getHunterBody());
        });
        return sortedHunterHcns.stream().toList();
    }

    public Hcn generateHcn(Prime prime) {
        return  Hcn.builder().recorderBody(this).lastActivePrime(prime).value(getHcnValue(prime)).factor(getHcnFactor(prime)).build();
    }

    public ScientificNumber getHcnValue(Prime prime) {
        return body.getValue().multiply(prime.getValueMultiplier());
    }

    public ScientificNumber getHcnFactor(Prime prime) {
        return body.getFactor().multiply(prime.getFactorMultiplier());
    }

    @Override
    public String toString() {
        return body.toString() + " intervalLapiDistance: " + intervalLapiDistance;
    }
}
