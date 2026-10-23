package com.hcn.newCore;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

@Slf4j
@Getter
@Setter
@SuperBuilder
public class HcnGenerator implements Comparable<HcnGenerator> {

    private static HcnGenerator firstHcnGenerator;
    private static int size = 0;
    public static HcnGenerator getFirstHcnGenerator() { return firstHcnGenerator; }

    public static void initialHcnGeneratorListBasedOnLastBodyList() {
        Body firstBody = TransitionNode.getLastTransition().getBodyList().getSmallestBody();
        firstHcnGenerator = HcnGenerator.builder().body(firstBody).previousHcnGenerator(null)
                .nextHcnGenerator(null).generatedHunterHcns(new ArrayList<>()).activeHunterHcns(new ArrayList<>()).build();
        firstBody.setHcnGenerator(firstHcnGenerator);
        Body secondBody = firstBody.getLargerBody();
        HcnGenerator secondHcnGenerator = HcnGenerator.builder().body(secondBody).previousHcnGenerator(firstHcnGenerator)
                .nextHcnGenerator(null).generatedHunterHcns(new ArrayList<>()).activeHunterHcns(new ArrayList<>()).build();
        secondBody.setHcnGenerator(secondHcnGenerator);
        firstHcnGenerator.setNextHcnGenerator(secondHcnGenerator);

        HcnGenerator prevHcnGenerator = secondHcnGenerator;
        while (secondBody.getLargerBody() != null) {
            secondBody = secondBody.getLargerBody();
            HcnGenerator hcnGenerator = HcnGenerator.builder().body(secondBody).previousHcnGenerator(prevHcnGenerator)
                    .nextHcnGenerator(null).generatedHunterHcns(new ArrayList<>()).activeHunterHcns(new ArrayList<>()).build();
            secondBody.setHcnGenerator(hcnGenerator);
            prevHcnGenerator.setNextHcnGenerator(hcnGenerator);
            prevHcnGenerator = hcnGenerator;
        }
    }

    protected Body body;
    private Hcn hunterHcn;
    private List<Hcn> generatedHunterHcns;
    private List<Hcn> activeHunterHcns;
    private HcnGenerator previousHcnGenerator;
    private HcnGenerator nextHcnGenerator;
    private RecorderBody recorderBody;

    public HcnGenerator(Body newHunterBody) {
        this.body = newHunterBody;
        this.previousHcnGenerator = body.getPrevActiveBody();
        this.nextHcnGenerator = previousHcnGenerator.nextHcnGenerator;
        this.generatedHunterHcns = new ArrayList<>();
        this.activeHunterHcns = new ArrayList<>();
        newHunterBody.setHcnGenerator(this);

        if (previousHcnGenerator != null) previousHcnGenerator.nextHcnGenerator = this;
        else firstHcnGenerator = this;
        if (nextHcnGenerator != null) nextHcnGenerator.previousHcnGenerator = this;

        Hcn referenceHunterHcn = getReferenceHcnForFindPray();
        RecorderBody prayCandidate = referenceHunterHcn.getPassingZone().getPrayBody();
        //log.debug(" ################################ createHcnGenerator: {}", this);
        //log.debug(" previous: {}, next: {}", previousHcnGenerator, nextHcnGenerator);
        prayCandidate.findPrayCheck(this, referenceHunterHcn.getIntervalDistance());
        //log.debug("");
    }

    public void deleteHcnGenerator() {
        if (previousHcnGenerator != null) previousHcnGenerator.setNextHcnGenerator(nextHcnGenerator);
        else firstHcnGenerator = nextHcnGenerator;

        if (nextHcnGenerator != null) nextHcnGenerator.setPreviousHcnGenerator(previousHcnGenerator);
        //else largestBody = prev;

        previousHcnGenerator = null;
        nextHcnGenerator = null;

        //log.debug(" ÷÷÷÷÷÷÷÷÷÷÷÷÷÷÷÷÷÷÷÷÷÷÷÷÷÷÷ RECORDERLIST before delete ÷÷÷÷÷÷÷÷÷÷÷÷÷÷÷÷÷÷÷÷÷÷÷÷÷÷");
        RecorderList.print();
        for (Hcn generatedHunterHcn : activeHunterHcns) {
            generatedHunterHcn.getPassingZone().deleteHunterHcn(generatedHunterHcn);
        }
        activeHunterHcns.clear();
        //log.debug(" ÷÷÷÷÷÷÷÷÷÷÷÷÷÷÷÷÷÷÷÷÷÷÷÷÷÷÷ RECORDERLIST after delete ÷÷÷÷÷÷÷÷÷÷÷÷÷÷÷÷÷÷÷÷÷÷÷÷÷÷");
        RecorderList.print();
        //deletedBody.getHcnGenerator().getHunterPassingZone().deleteHunterBodyFromBodyList(deletedBody.getHcnGenerator());
        //print();
    }

    protected Hcn getHcn(Prime prime, HcnStatus status, PassingZone passingZone, int intervalDistance) {
        return Hcn.builder().hcnGenerator(this).value(getHcnValue(prime)).factor(getHcnFactor(prime)).lastActivePrime(prime)
                .hcnStatus(status).passingZone(passingZone).intervalDistance(intervalDistance).build();
    }

    public Hcn generateHunterHcn(Prime prime, HcnStatus status, PassingZone passingZone, int intervalDistance) {
        hunterHcn = getHcn(prime, status, passingZone, intervalDistance);
        generatedHunterHcns.add(hunterHcn);
        return hunterHcn;
    }

    public ScientificNumber getHcnValue(Prime prime) {
        return body.getValue().multiply(prime.getValueMultiplier());
    }
    public ScientificNumber getHcnFactor(Prime prime) {
        return body.getFactor().multiply(prime.getFactorMultiplier());
    }

    @Override
    public String toString() {
        return body.toString();
    }

    @Override
    public int compareTo(HcnGenerator other) {
        return this.body.getValue().compareTo(other.body.getValue());
    }

    public Hcn getReferenceHcnForFindPray() {
        HcnGenerator referenceCandidate = previousHcnGenerator;
        while (referenceCandidate.hunterHcn == null) {
            referenceCandidate = referenceCandidate.previousHcnGenerator;
        }
        return referenceCandidate.hunterHcn;
    }

    public List<Integer> getIntervalDistances() {
        ArrayList<Integer> returnList = new ArrayList<>();
        activeHunterHcns.forEach(acitveHcn -> returnList.add(acitveHcn.getIntervalDistance()));
        return returnList;
    }
}
