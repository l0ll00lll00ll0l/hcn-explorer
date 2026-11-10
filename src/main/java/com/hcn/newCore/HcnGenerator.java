package com.hcn.newCore;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Getter
@Setter
@SuperBuilder
public class HcnGenerator implements Comparable<HcnGenerator> {

    private static HcnGenerator firstHcnGenerator;
    public static HcnGenerator getFirstHcnGenerator() { return firstHcnGenerator; }

    public static void reset() { firstHcnGenerator = null; }

    public static void initialize() {

        Body firstBody = TransitionNode.getLastTransition().getBodyList().getSmallestBody();
        firstHcnGenerator = HcnGenerator.builder().body(firstBody).previousHcnGenerator(null)
                .nextHcnGenerator(null).generatedHunterHcns(new ArrayList<>()).referencedHcns(new ArrayList<>()).build();
        firstBody.setHcnGenerator(firstHcnGenerator);
        Body secondBody = firstBody.getLargerBody();
        HcnGenerator secondHcnGenerator = HcnGenerator.builder().body(secondBody).previousHcnGenerator(firstHcnGenerator)
                .nextHcnGenerator(null).generatedHunterHcns(new ArrayList<>()).referencedHcns(new ArrayList<>()).build();
        secondBody.setHcnGenerator(secondHcnGenerator);
        firstHcnGenerator.setNextHcnGenerator(secondHcnGenerator);

        HcnGenerator prevHcnGenerator = secondHcnGenerator;
        while (secondBody.getLargerBody() != null) {
            secondBody = secondBody.getLargerBody();
            HcnGenerator hcnGenerator = HcnGenerator.builder().body(secondBody).previousHcnGenerator(prevHcnGenerator)
                    .nextHcnGenerator(null).generatedHunterHcns(new ArrayList<>()).referencedHcns(new ArrayList<>()).build();
            secondBody.setHcnGenerator(hcnGenerator);
            prevHcnGenerator.setNextHcnGenerator(hcnGenerator);
            prevHcnGenerator = hcnGenerator;
        }
    }

    protected Body body;
    private List<Hcn> generatedHunterHcns;
    private List<Hcn> referencedHcns;
    private HcnGenerator previousHcnGenerator;
    private HcnGenerator nextHcnGenerator;
    private RecorderBody recorderBody;

    public HcnGenerator(Body newHunterBody) {

        this.body = newHunterBody;
        this.previousHcnGenerator = body.getPrevActiveBody();
        this.nextHcnGenerator = previousHcnGenerator.nextHcnGenerator;
        this.generatedHunterHcns = new ArrayList<>();
        this.referencedHcns = new ArrayList<>();
        newHunterBody.setHcnGenerator(this);

        if (previousHcnGenerator != null) previousHcnGenerator.nextHcnGenerator = this;
        else firstHcnGenerator = this;
        if (nextHcnGenerator != null) nextHcnGenerator.previousHcnGenerator = this;

        Hcn referenceHunterHcn = getReferenceHcnForFindPray();
        RecorderBody prayCandidate = referenceHunterHcn.getPrayBody();
        prayCandidate.findPrayCheck(this, referenceHunterHcn.getIntervalDistance());
    }

    public void deleteHcnGenerator() {

        if (previousHcnGenerator != null) previousHcnGenerator.setNextHcnGenerator(nextHcnGenerator);
        else firstHcnGenerator = nextHcnGenerator;

        if (nextHcnGenerator != null) nextHcnGenerator.setPreviousHcnGenerator(previousHcnGenerator);

        previousHcnGenerator = null;
        nextHcnGenerator = null;

        for (Hcn referenceHcn : referencedHcns) {
            referenceHcn.getPrayBody().deleteHunterHcn(referenceHcn);
        }
        referencedHcns.clear();
    }

    protected Hcn getHcn(Prime prime, HcnStatus status, RecorderBody prayBody, int intervalDistance) {
        return Hcn.builder().hcnGenerator(this).value(getHcnValue(prime)).factor(getHcnFactor(prime)).lastActivePrime(prime)
                .hcnStatus(status).prayBody(prayBody).intervalDistance(intervalDistance).build();
    }

    public Hcn generateHunterHcn(Prime prime, HcnStatus status, RecorderBody prayBody, int intervalDistance) {
        Hcn hunterHcn = getHcn(prime, status, prayBody, intervalDistance);
        generatedHunterHcns.add(hunterHcn);
        return hunterHcn;
    }

    public Hcn generateReferencedHcn(Prime prime, RecorderBody prayBody, int intervalDistance) {
        Hcn hunterHcn = generateHunterHcn(prime, HcnStatus.REFERENCED_HUNTER, prayBody, intervalDistance);
        referencedHcns.add(hunterHcn);
        return hunterHcn;
    }

    public Hcn generateTempHcn(Prime prime, RecorderBody prayBody, int intervalDistance) {
        return Hcn.builder().hcnGenerator(this).value(getHcnValue(prime)).factor(getHcnFactor(prime)).lastActivePrime(prime).prayBody(prayBody).intervalDistance(intervalDistance).build();
    }
    public Hcn generateHcnProjection(Prime prime, int hcnOrder) {
        return Hcn.builder().hcnGenerator(this).value(getHcnValue(prime)).factor(getHcnFactor(prime)).lastActivePrime(prime).order(hcnOrder).build();
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
        while (referenceCandidate.referencedHcns.isEmpty()) {
            referenceCandidate = referenceCandidate.previousHcnGenerator;
        }
        return referenceCandidate.referencedHcns.get(0);
    }

    public List<Integer> getIntervalDistances() {
        ArrayList<Integer> returnList = new ArrayList<>();
        referencedHcns.forEach(acitveHcn -> returnList.add(acitveHcn.getIntervalDistance()));
        return returnList;
    }
}
