package com.hcn.newCore;

import lombok.experimental.SuperBuilder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

@Getter
@Setter
@SuperBuilder
@Slf4j
public class RecorderBody extends HcnGenerator{

    private RecorderBody previousRecorder;
    private RecorderBody nextRecorder;
    private final List<HunterBody> hunters = new ArrayList<>();
    private final List<RecorderBody> recorderHunters = new ArrayList<>();
    private Hcn firstDominatedHcn = null;
    private Hcn firstSuperiorHcn = null;
    private RecorderBody prayBody = null;

    public ScientificNumber getCurrentHcnFactor() {
            return getHcnFactor(Prime.getPrimeByLapiDistance(intervalLapiDistance));
    }

    public Hcn getCurrentHcn() {
        Prime prime = Prime.getPrimeByLapiDistance(intervalLapiDistance);
        return Hcn.builder().hcnGenerator(this).lastActivePrime(prime).value(getHcnValue(prime)).factor(getHcnFactor(prime)).build();
    }

    public List<Hcn> generateSmallerHunterHcns(Hcn recorderHcn) {

        TreeSet<Hcn> sortedHunterHcns = new TreeSet<>();
        hunters.forEach(hunterBody -> {
            Hcn hunterHcn = hunterBody.getProcessingHcn();

            if (hunterHcn.getValue().isSmallerThan(recorderHcn.getValue())) {
                log.debug(" hunterBody ADDED: {}", hunterBody);
                sortedHunterHcns.add(hunterHcn);
            }
        });
        sortedHunterHcns.forEach(hunterHcn -> hunters.remove(hunterHcn.getHcnGenerator()));
        return sortedHunterHcns.stream().toList();
    }

    public List<Hcn> generatePassingRecorderHunters(Hcn recorderHcn) {
        TreeSet<Hcn> sortedRecorderHcns = new TreeSet<>();
        log.debug(" recorderHcn: {} | recorderHunters.size(): {}", recorderHcn.getValue().toString(), recorderHunters.size());
        recorderHunters.forEach(recorderBody -> {
            if (recorderHcn.getValue().isSmallerThan(recorderHcn.getValue())) {
                log.debug(" recorderBody ADDED: {} | {}", recorderBody, recorderHcn.getValue().toString());
                sortedRecorderHcns.add(recorderHcn);
            } else {
                log.debug(" recorderBody SKIPPED: {} | {}", recorderBody, recorderHcn.getValue().toString());
            }
        });
        sortedRecorderHcns.forEach(hcn -> recorderHunters.remove(hcn.getHcnGenerator()));
        return sortedRecorderHcns.stream().toList();
    }

    public List<Hcn> getPassingHcns(Hcn recorderHcn) {
        TreeSet<Hcn> sortedHcns = new TreeSet<>();
        hunters.forEach(hunterBody -> {
            Hcn hunterHcn = hunterBody.getProcessingHcn();
            if (hunterHcn.getValue().isSmallerThan(recorderHcn.getValue())) {
                sortedHcns.add(hunterHcn);
            }
        });
        recorderHunters.forEach(recorderBody -> {
            Hcn recorderHunterHcn = recorderBody.getProcessingHcn();
            if (recorderHunterHcn.getValue().isSmallerThan(recorderHcn.getValue())) {
                sortedHcns.add(recorderHunterHcn);
            }
        });
        sortedHcns.forEach(hcn -> {
            if (hcn.getHcnGenerator() instanceof RecorderBody) {
                recorderHunters.remove(hcn.getHcnGenerator());
            } else {
                hunters.remove(hcn.getHcnGenerator());
            }
        });
        return sortedHcns.stream().toList();
    }

    @Override
    public String toString() {
        return body.toString() + " intervalLapiDistance: " + intervalLapiDistance;
    }
}
