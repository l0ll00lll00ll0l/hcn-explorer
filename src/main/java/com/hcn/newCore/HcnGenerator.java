package com.hcn.newCore;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;

@Slf4j
@Getter
@Setter
@SuperBuilder
public class HcnGenerator implements Comparable<HcnGenerator> {

    private static HcnGenerator lastHcnGenerator = null;
    public static void reset() { lastHcnGenerator = null; }
    public static HcnGenerator getLastHcnGenerator() { return lastHcnGenerator; }

    public static void initialize() {

        int currentHcnGeneratorCount = 0;
        while (currentHcnGeneratorCount < 6) {
            Body bodyToPrepare = Body.getSmallestBufferBody();
            lastHcnGenerator = new HcnGenerator(bodyToPrepare);
            Body.shiftNextHcnGeneratorBody();
            TransitionNode.getLastTransition().deletePolledBody(bodyToPrepare);
            currentHcnGeneratorCount ++;
        }
    }

    public static void createHcnGeneratorsForBufferBodyMode() {
        Body bodyToPrepare = Body.getSmallestBufferBody();
        lastHcnGenerator = new HcnGenerator(bodyToPrepare);
        Body.shiftNextHcnGeneratorBody();
        TransitionNode.getLastTransition().deletePolledBody(bodyToPrepare);
    }

    public static void createHcnGenerators(RecorderBody recorderBody) {
        //log.debug("createHcnGenerators trigger: {} ----------------------", recorderBody.getHcnGenerator());
        while (lastHcnGenerator.getFactor().isSmallerThan(recorderBody.getHcnGenerator().getFactor().multiply(new ScientificNumber(2, 0)))) {
            Body bodyToPrepare = Body.getSmallestBufferBody();
            lastHcnGenerator = new HcnGenerator(bodyToPrepare);
            RecorderBody.placeHunter(lastHcnGenerator);
            Body.shiftNextHcnGeneratorBody();
            TransitionNode.getLastTransition().deletePolledBody(bodyToPrepare);
            //log.debug("  new HcnGenerator created: {}", lastHcnGenerator);
        }
        //log.debug("");
    }

    private final List<Integer> powerGroupPowers = new ArrayList<>();
    private final List<Integer> powerGroupIndexes = new ArrayList<>();
    private int[] head;
    private int[] tail;
    private final List<Hcn> generatedHunterHcns;
    private final HcnGenerator previousHcnGenerator;
    private ScientificNumber value;
    private ScientificNumber factor;
    private HcnGenerator nextHcnGenerator;
    private RecorderBody recorderBody;

    public HcnGenerator(Body newHunterBody) {
        newHunterBody.createPowerGroups(this);
        createHeadAndTailArrays(newHunterBody);
        //determinePowerGroups(newHunterBody);
        this.value = newHunterBody.getValue();
        this.factor = newHunterBody.getFactor();
        this.previousHcnGenerator = lastHcnGenerator;
        this.nextHcnGenerator = null;
        this.generatedHunterHcns = new ArrayList<>();
        if (lastHcnGenerator != null) previousHcnGenerator.nextHcnGenerator = this;
    }

    private void createHeadAndTailArrays(Body currentBody) {
        ArrayList<Integer> tail = new ArrayList<>();
        ArrayList<Integer> head = new ArrayList<>();
        int lastPower = 0;

        while (currentBody.getBodyNode().getParentNode() instanceof TransitionNode transitionNode) {
            tail.add(currentBody.getBodyNode().getBodyNodeId() - 1);
            currentBody = currentBody.getParent();
            lastPower = transitionNode.getTransitionFrom();
        }

        if (tail.get(tail.size() - 1) == currentBody.getBodyNode().getParentNode().getIndexes().get(0).getIndex()) {
            lastPower--;
            tail.remove(tail.size() - 1);
        }

        //carry on with apiNodes to identify last tailIndex
        while (currentBody != null) {
            int currentApiPower = currentBody.getBodyNode().getBodyNodeId();
            if (currentApiPower > lastPower + 1) {
                break;
            } else {
                if (currentApiPower > lastPower) {
                    tail.add(currentBody.getBodyNode().getParentNode().getIndexes().get(0).getIndex());
                    lastPower = currentApiPower;
                }
            }
            currentBody = currentBody.getParent();
        }

        while (currentBody != null) {
            head.add(currentBody.getBodyNode().getBodyNodeId());
            currentBody = currentBody.getParent();
        }

        this.head = new int[head.size()];
        for (int i = 0; i < head.size(); i++) {
            this.head[head.size() -1 -i] = head.get(i);
        }
        this.tail = new int[tail.size()];
        for (int i = 0; i < tail.size(); i++) {
            this.tail[tail.size() -1 -i] = tail.get(i);
        }
    }

    protected Hcn getHcn(Prime prime, HcnStatus status, RecorderBody prayBody, int intervalDistance) {
        return Hcn.builder().hcnGenerator(this).value(getHcnValue(prime)).factor(getHcnFactor(prime)).lastActivePrime(prime)
                .hcnStatus(status).intervalDistance(intervalDistance).build();
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
        return Hcn.builder().hcnGenerator(this).value(getHcnValue(prime)).factor(getHcnFactor(prime)).lastActivePrime(prime).intervalDistance(intervalDistance).build();
    }
    public Hcn generateHcnProjection(Prime prime, int hcnOrder) {
        return Hcn.builder().hcnGenerator(this).value(getHcnValue(prime)).factor(getHcnFactor(prime)).lastActivePrime(prime).order(hcnOrder).build();
    }
    public ScientificNumber getHcnValue(Prime prime) {
        return value.multiply(prime.getValueMultiplier());
    }
    public ScientificNumber getHcnFactor(Prime prime) {
        return factor.multiply(prime.getFactorMultiplier());
    }

    @Override
    public String toString() {
        /*
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < powerGroupIndexes.size(); i++) {
            sb.append(powerGroupIndexes.get(i)).append("^").append(powerGroupPowers.get(i)).append("|");
        }

        return powerGroupPowers + "|" + powerGroupIndexes + " - " + sb.deleteCharAt(sb.length() - 1);

         */
        //return Arrays.toString(head) + "|" + Arrays.toString(tail) + " - " + dbBodyTosString();
        return dbBodyTosString();
    }

    @Override
    public int compareTo(HcnGenerator other) {
        return this.value.compareTo(other.value);
    }

    private String dbBodyTosString() {
        StringBuilder sb = new StringBuilder();
        int startingRelationIndex = 0;
        int powerGroup;

        if (head.length == 0 && tail.length == 0) return "p0^1";

        if (head.length > 0) {
            powerGroup = head[0];

            for (int currentIndex = 1; currentIndex < head.length; currentIndex++) {
                int currentPower = head[currentIndex];
                if (powerGroup > currentPower) {
                    if (currentIndex - startingRelationIndex == 1) {
                        sb.append("p").append(startingRelationIndex).append("^").append(powerGroup).append(" ");
                    } else {
                        sb.append("p").append(startingRelationIndex).append("-").append(currentIndex - 1).append("^").append(powerGroup).append(" ");
                    }

                    startingRelationIndex = currentIndex;
                    powerGroup = currentPower;
                }
            }

            if (head.length == startingRelationIndex + 1) {
                sb.append("p").append(startingRelationIndex).append("^").append(powerGroup).append(" ");
            } else if (head.length > startingRelationIndex + 1) {
                sb.append("p").append(startingRelationIndex).append("-").append(head.length - 1).append("^").append(powerGroup).append(" ");
            }
        }
        startingRelationIndex = head.length;
        powerGroup = tail.length + 1;

        for (int i = 0; i < tail.length; i++) {

            int currentIndex = tail[i];

            if (startingRelationIndex == currentIndex) {
                sb.append("p").append(startingRelationIndex).append("^").append(powerGroup).append(" ");
            } else {
                sb.append("p").append(startingRelationIndex).append("-").append(currentIndex).append("^").append(powerGroup).append(" ");
            }
            startingRelationIndex = currentIndex + 1;
            powerGroup --;
        }

        return sb.toString();
    }

    /*
    public void deactivateCheck() {
        if (!this.equals(RecorderBody.getFirstRecorder().getHunters().get(1).getHcnGenerator())) {
            if (recorderBody == null) {
                body.deactivate();
            }
            this.nextHcnGenerator.deactivateCheck();
        }
    }

     */

}
