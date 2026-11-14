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
    private static HcnGenerator lastHcnGenerator;
    public static HcnGenerator getFirstHcnGenerator() { return firstHcnGenerator; }
    public static void reset() { firstHcnGenerator = null; }

    public static void initialize() {

        Body firstBody = TransitionNode.getLastTransition().getBodyList().getSmallestBody();
        firstHcnGenerator = HcnGenerator.builder().body(firstBody).previousHcnGenerator(null)
                .nextHcnGenerator(null).generatedHunterHcns(new ArrayList<>()).build();
        firstBody.setHcnGenerator(firstHcnGenerator);
        Body secondBody = firstBody.getLargerBody();
        lastHcnGenerator = HcnGenerator.builder().body(secondBody).previousHcnGenerator(firstHcnGenerator)
                .nextHcnGenerator(null).generatedHunterHcns(new ArrayList<>()).build();
        secondBody.setHcnGenerator(lastHcnGenerator);
        firstHcnGenerator.setNextHcnGenerator(lastHcnGenerator);

        while (secondBody.getLargerBody() != Body.getSmallestBufferBody()) {
            secondBody = secondBody.getLargerBody();
            HcnGenerator hcnGenerator = HcnGenerator.builder().body(secondBody).previousHcnGenerator(lastHcnGenerator)
                    .nextHcnGenerator(null).generatedHunterHcns(new ArrayList<>()).build();
            secondBody.setHcnGenerator(hcnGenerator);
            lastHcnGenerator.setNextHcnGenerator(hcnGenerator);
            lastHcnGenerator = hcnGenerator;
        }
    }

    protected Body body;
    private List<Hcn> generatedHunterHcns;
    private HcnGenerator previousHcnGenerator;
    private HcnGenerator nextHcnGenerator;
    private RecorderBody recorderBody;

    public HcnGenerator(Body newHunterBody) {

        this.body = newHunterBody;
        this.previousHcnGenerator = lastHcnGenerator;
        this.nextHcnGenerator = null;
        this.generatedHunterHcns = new ArrayList<>();
        newHunterBody.setHcnGenerator(this);
        previousHcnGenerator.nextHcnGenerator = this;
        RecorderBody.placeHunter(this);
    }

    public static void createHcnGenerators(RecorderBody recorderBody) {
        log.debug("createHcnGenerators trigger: {} ----------------------", recorderBody.getHcnGenerator());
        while (lastHcnGenerator.getBody().getFactor().isSmallerThan(recorderBody.getHcnGenerator().body.getFactor().multiply(new ScientificNumber(2, 0)))) {
            Body newHunterBody = Body.getSmallestBufferBody();
            Body.shiftNextHcnGeneratorBody();
            lastHcnGenerator = new HcnGenerator(newHunterBody);
            log.debug("  new HcnGenerator created: {}", lastHcnGenerator);
        }
        log.debug("");
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

    public void deactivateCheck() {
        if (!this.equals(RecorderBody.getFirstRecorder().getHunters().get(1).getHcnGenerator())) {
            if (recorderBody == null) {
                body.deactivate();
            }
            this.nextHcnGenerator.deactivateCheck();
        }
    }
}
