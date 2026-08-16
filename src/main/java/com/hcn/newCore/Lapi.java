package com.hcn.newCore;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;

@Getter
@Setter
@Builder
@Slf4j
public class Lapi {
    private static Lapi lowestLapi;
    private static Lapi highestLapi;
    private final Prime prime;
    private Lapi lowerLapi;
    private Lapi higherLapi;
    private Body walker;
    private ScientificNumber valueMultiplier;
    private ScientificNumber factorMultiplier;
    private final ArrayList<Hcn> generatedHcns = new ArrayList<>();

    public static Lapi getLowestLapi() {
        return lowestLapi;
    }

    public static void setLowestLapi(Lapi lowestLapi) {
        Lapi.lowestLapi = lowestLapi;
    }

    public static Lapi getHighestLapi() {
        return highestLapi;
    }

    public static void setHighestLapi(Lapi highestLapi) {
        Lapi.highestLapi = highestLapi;
    }

    public static Lapi createNextLapi() {
        Lapi nextLapi = Lapi.builder().prime(highestLapi.getPrime().getNextPrime()).lowerLapi(highestLapi)
                .valueMultiplier(highestLapi.getValueMultiplier().multiply(highestLapi.getPrime().getNextPrime().getValue()))
                .factorMultiplier(highestLapi.getFactorMultiplier().multiply(new ScientificNumber(2, 0))).build();
        highestLapi.setHigherLapi(nextLapi);
        highestLapi = nextLapi;
        return nextLapi;
    }

    public Lapi deleteLapi() {
        //hcnList.clear();
        log.debug("deleteLapi {}", prime.getIndex());
        higherLapi.setLowerLapi(null);
        Lapi newLowLapi = higherLapi;
        this.setHigherLapi(null);
        return newLowLapi;
    }

    public void hcnGenerationPhase(ScientificNumber provedLimit) {
        //log.debug("generateHcnList for prime " + prime.getIndex());
        if (walker == null || walker.isDeactivated() || walker.getLargerHcnGenerator() == null) {
            walker = restoreWalker(provedLimit);
        }
        if (walker != null) {
            //moveWalkerIfNotSuperiorCheck();
            createBaseHcnList();
            //mergeLowerHcnlist(provedLimit);
        }
        if (higherLapi != null) {higherLapi.hcnGenerationPhase(provedLimit);}
    }

    private Body restoreWalker(ScientificNumber provedLimit) {
        Body result = null;
        Body candidate = HcnGeneratorList.getSmallestBody();
        while (candidate != null) {
            if (!candidate.isDeactivated() && candidate.getLastGeneratedHcn() != null && candidate.getLastGeneratedHcn().getLapiIndex() == prime.getIndex()) {
                result = candidate;
            }
            candidate = candidate.getNextActiveBody();
        }
        if (result == null) return null;
        while (result.getLastGeneratedHcn().getValue().isNotBiggerThan(provedLimit)) {
            if (!result.equals(HcnGeneratorList.getSmallestBody())) {
                //System.out.println("1 " + result);
            }

            if (result.isNonDeactivated()) {
                //System.out.println("2 " + result);
            }
            Body next = result.getNextActiveBody();
            if (next == null) return null;
            result = next;
            if (result.getLastGeneratedHcn() == null || result.getLastGeneratedHcn().getLapiIndex() != prime.getIndex()) {
                result.generateHcn(this);
            }
        }
        return result;
    }

    private void createBaseHcnList() {
        //log.debug("createBaseHcnList for prime " + prime.getIndex());
        ScientificNumber targetValue = Matrix.getCurrentInterval().getTargetValue();
        generatedHcns.clear();
        while (walker != null) {
            if (!determineNextHcnValue().isNotBiggerThan(targetValue)) break;
            generatedHcns.add(walker.generateHcn(this));
            walker = walker.getLargerHcnGenerator();
        }
        //log.debug(RecorderList.print());
    }

    private ScientificNumber determineNextHcnValue() {
        return walker.getValue().multiply(valueMultiplier);
    }

    public void huntingPhase() {
        generatedHcns.forEach(hcn -> hcn.getBody().hunt(hcn));
        if (higherLapi != null) {higherLapi.huntingPhase();}
    }
    /*
    private void moveWalkerIfNotSuperiorCheck() {
        if (lowerLapi != null) {
            if (hcnList.get(hcnList.size() - 1).getLapi() < prime.getIndex()) {
                while (walker.getLastGeneratedHcn().getFactor().isNotBiggerThan(hcnList.get(hcnList.size() - 1).getFactor())) {
                    walker.getLastGeneratedHcn().deactivateParent();
                    walker = walker.getNextActiveBody();
                    generateHcn(walker);
                }
            }
        }
    }

    private void mergeLowerHcnlist(ScientificNumber provedLimit) {
        if (lowerLapi == null) {return;}

        int localSuperiorIndex = 0;
        int lowerLapiNextHcnIndex = lowerLapi.computeIndexForPovedLimitBefore(provedLimit);

        while (lowerLapiNextHcnIndex < lowerLapi.hcnList.size()) {
            Hcn lowerLapiNextHcn = lowerLapi.hcnList.get(lowerLapiNextHcnIndex);
            while ((hcnList.size() > localSuperiorIndex + 1) && hcnList.get(localSuperiorIndex + 1).getValue().isSmallerThan(lowerLapiNextHcn.getValue())) {
                localSuperiorIndex++;
            }
            Hcn localSuperiorHcn = hcnList.get(localSuperiorIndex);
            if (lowerLapiNextHcn.getFactor().isBiggerThan(localSuperiorHcn.getFactor())) {
                hcnList.add(localSuperiorIndex + 1, lowerLapiNextHcn);
                int indexToFactorCheck = localSuperiorIndex + 2;
                while (indexToFactorCheck < hcnList.size() && hcnList.get(indexToFactorCheck).getFactor().isNotBiggerThan(lowerLapiNextHcn.getFactor())) {
                    hcnList.get(indexToFactorCheck).deactivateParent();
                    hcnList.remove(indexToFactorCheck);
                }
            }
            lowerLapiNextHcnIndex++;
        }
    }


    private int computeIndexForPovedLimitBefore(ScientificNumber provedLimit) {
        for (int i = 0; i < hcnList.size(); i++ ) {
            if (hcnList.get(i).getValue().isBiggerThan(provedLimit)) {
                return i;
            }
        }
        return hcnList.size();
    }


     */
    public void recalculateMultipliers(Prime prevLastMatrixIndex) {

        valueMultiplier = valueMultiplier.divide(prevLastMatrixIndex.getValue());
        factorMultiplier = factorMultiplier.divide(new ScientificNumber(2, 0));
        if (higherLapi != null) {
            higherLapi.recalculateMultipliers(prevLastMatrixIndex);
        }
    }
}
