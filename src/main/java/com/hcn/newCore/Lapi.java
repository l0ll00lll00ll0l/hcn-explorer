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

    /*
    public static Lapi createNextLapi() {
        Lapi nextLapi = Lapi.builder().prime(highestLapi.getPrime().getNextPrime()).lowerLapi(highestLapi)
                .valueMultiplier(highestLapi.getValueMultiplier().multiply(highestLapi.getPrime().getNextPrime().getValue()))
                .factorMultiplier(highestLapi.getFactorMultiplier().multiply(new ScientificNumber(2, 0))).build();
        highestLapi.setHigherLapi(nextLapi);
        highestLapi = nextLapi;
        return nextLapi;
    }



    public static void highestLapiWalkerDeleted() {
        //log.debug(" NEW CHECK");
        //log.debug(" provedLimit= {}", Matrix.getProvedLimit());
        //log.debug(" targetValue= {}", Matrix.getTargetValue());
        //log.debug(" highestLapi.determineNextHcnValue()= {}", highestLapi.determineNextHcnValue());

        if (highestLapi.determineNextHcnValue().isBiggerThan(Matrix.getTargetValue())) {
            highestLapi = highestLapi.getLowerLapi();
            highestLapi.setHigherLapi(null);
        }
    }
 */
    public static void deleteLapisUnder() {
        //log.debug("DeletingLapi Under {}", Interval.getCurrentInterval().getLowestRecorderLapi());

        while (lowestLapi.prime.getIndex() < Interval.getCurrentInterval().getLowestRecorderLapi()) {
            //log.debug(" deleting lowestLapi= {}", lowestLapi.getPrime().getIndex());
            lowestLapi = lowestLapi.getHigherLapi();
            lowestLapi.getLowerLapi().setHigherLapi(null);
            lowestLapi.setLowerLapi(null);
        }
    }
    /*
        private void hcnGenerationPhase() {
            //log.debug("generateHcnList for prime {}, walker: {}", prime.getIndex(), walker);

            if (walker == null || walker.isDeactivated()) {
                //log.debug(" Restore required for lapi {}", prime.getIndex());
                restoreWalker();
            }

            if (walker != null) {
                createBaseHcnList();
            }

            if (lowerLapi != null) {
                lowerLapi.generatedHcns.clear();
                lowerLapi.hcnGenerationPhase();
            }
        }


        public static void generateHcnsUntilTargetValue() {

            Prime nextPrime = highestLapi.getPrime().getNextPrime();
            ScientificNumber nextLapiEnterValue = nextPrime.getValue().multiply(highestLapi.valueMultiplier).multiply(HcnGeneratorList.getSmallestBody().getValue());
            //log.debug("nextLapiEnterValue {}", nextLapiEnterValue);
            if (nextLapiEnterValue.isNotBiggerThan(Matrix.getTargetValue())) {
                highestLapi.setHigherLapi(Lapi.builder().prime(nextPrime).lowerLapi(highestLapi).walker(HcnGeneratorList.getSmallestBody())
                        .valueMultiplier(highestLapi.valueMultiplier.multiply(nextPrime.getValue()))
                        .factorMultiplier(highestLapi.getFactorMultiplier().multiply(new ScientificNumber(2.0, 0)))
                        .build());
                highestLapi = highestLapi.getHigherLapi();
                highestLapi.generatedHcns.add(highestLapi.walker.generateHcn(Prime.getHighestHcnProducerPrime()));
            } else {
                lowestLapi.generatedHcns.clear();
            }
            highestLapi.hcnGenerationPhase();
        }

        public void restoreWalker() {
            if (walker == null) {
                Body tempWalker = HcnGeneratorList.getLargestBody();
                ScientificNumber potentialValue = tempWalker.getValue().multiply(valueMultiplier);
                //log.debug("restore null walker tempwalker: {}, potentialValue: {}", tempWalker, potentialValue);
                while (potentialValue.isBiggerThan(Matrix.getProvedLimit())) {
                    tempWalker = tempWalker.getSmallerHcnGenerator();
                    potentialValue = tempWalker.getValue().multiply(valueMultiplier);
                    //log.debug(" inner restore null walker tempwalker: {}, potentialValue: {}", tempWalker, potentialValue);
                }
                //walker = tempWalker.getLargerHcnGenerator();
                //log.debug("Wlaker set: {} for provedLimit: {}", walker, Matrix.getProvedLimit());
            }
        }

        private void createBaseHcnList() {
            //log.debug("createBaseHcnList for prime " + prime.getIndex());
            ScientificNumber targetValue = Matrix.getTargetValue();
            if (determineNextHcnValue().isBiggerThan(targetValue)) return;

            while (walker != null) {
                walker = walker.getLargerHcnGenerator();
                generatedHcns.add(walker.generateHcn(null));
                if (walker.getLargerHcnGenerator() == null) {
                    break;
                }
                if (determineNextHcnValue().isBiggerThan(targetValue)) break;
                //log.debug("1 Walker is: {}", walker);
            }
            //log.debug("Walker is: {}", walker);
        }

        private ScientificNumber determineNextHcnValue() {
            return walker.getLargerHcnGenerator().getValue().multiply(valueMultiplier);
        }



    public static void huntingPhase() {
        highestLapi.hunting();
    }

    private void hunting() {
        generatedHcns.forEach(hcn -> hcn.getBody().hunt(hcn));
        if (lowerLapi != null) {lowerLapi.hunting();}
    }

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

}
