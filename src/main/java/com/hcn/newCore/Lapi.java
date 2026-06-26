package com.hcn.newCore;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@Slf4j
public class Lapi {
    private final Prime prime;
    private Lapi lowerLapi;
    private Lapi higherLapi;
    private Body walker;
    private final List<Hcn> hcnList = new ArrayList<>();
    private ScientificNumber valueMultiplier;
    private ScientificNumber factorMultiplier;

    public void maintainAfterInterval() {
        Hcn last = hcnList.get(hcnList.size()-1);
        hcnList.clear();
        hcnList.add(last);

        if (higherLapi != null) {
            higherLapi.maintainAfterInterval();
        }
    }

    public Lapi deleteLapi() {
        hcnList.clear();
        higherLapi.setLowerLapi(null);
        Lapi newLowLapi = higherLapi;
        this.setHigherLapi(null);
        return newLowLapi;
    }

    public void generateHcnList(ScientificNumber provedLimit, ScientificNumber targetValue, Body smallestBody) {
        if (walker == null || walker.isDeactivated() || walker.getLargerActiveBody() == null) {
            walker = restoreWalker(smallestBody, provedLimit);
        }
        if (walker != null) {
            moveWalkerIfNotSuperiorCheck();
            createBaseHcnList(targetValue);
            mergeLowerHcnlist(provedLimit);
        }
        if (higherLapi != null) {higherLapi.generateHcnList(provedLimit, targetValue, smallestBody);}
    }

    private Body restoreWalker(Body smallestBody, ScientificNumber provedLimit) {
        //log.debug("restoreWalker lapi={} smallestBody={} provedLimit={}", prime.getIndex(), smallestBody, provedLimit);
        Body result = null;
        Body candidate = smallestBody;
        while (candidate != null) {
            //log.debug("  candidate={} deactivated={} lastGenHcn={}", candidate, candidate.isDeactivated(), candidate.getLastGeneratedHcn());
            if (!candidate.isDeactivated() && candidate.getLastGeneratedHcn() != null && candidate.getLastGeneratedHcn().getLapi() == prime.getIndex()) {
                result = candidate;
                //log.debug("  -> result updated to {}", result);
            }
            candidate = candidate.getNextActiveBody();
        }
        //log.debug("  after scan: result={}", result);
        if (result == null) return null;
        while (result.getLastGeneratedHcn().getValue().isNotBiggerThan(provedLimit)) {
            Body next = result.getNextActiveBody();
            //log.debug("  advancing past provedLimit: result={} next={}", result, next);
            if (next == null) return null;
            result = next;
            if (result.getLastGeneratedHcn() == null || result.getLastGeneratedHcn().getLapi() != prime.getIndex()) {
                //log.debug("  generating hcn for result={}", result);
                generateHcn(result);
            }
        }
        //log.debug("  restoreWalker returning: {}", result);
        return result;
    }

    private void createBaseHcnList(ScientificNumber targetValue) {
        while (walker.getLastGeneratedHcn().getValue().isSmallerThan(targetValue)) {
            hcnList.add(walker.getLastGeneratedHcn());
            Body walkercandidate = walker.getLargerActiveBody();
            if (walkercandidate == null) {
                break;
            }
            walker = walkercandidate;
            generateHcn(walker);
        }
    }

    private void moveWalkerIfNotSuperiorCheck() {
        if (lowerLapi != null) {
            if (hcnList.get(hcnList.size() - 1).getLapi() < prime.getIndex()) {
                //log.debug("moveWalkerIfNotSuperiorCheck lapi={} walker={} lastHcnFactor={} hcnListLastFactor={}",
                //        prime.getIndex(), walker, walker.getLastGeneratedHcn().getFactor(), hcnList.get(hcnList.size() - 1).getFactor());
                while (walker.getLastGeneratedHcn().getFactor().isNotBiggerThan(hcnList.get(hcnList.size() - 1).getFactor())) {
                    //log.debug("  dominating walker hcn: {} moving to largerActiveBody: {}", walker.getLastGeneratedHcn(), walker.getLargerActiveBody());
                    walker.getLastGeneratedHcn().gotDominated();
                    walker = walker.getNextActiveBody();
                    //log.debug("  new walker: {}", walker);
                    generateHcn(walker);
                    //log.debug("  generated hcn for walker, lastGenHcn={}", walker.getLastGeneratedHcn());
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
                    hcnList.get(indexToFactorCheck).gotDominated();
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

    public void recalculateMultipliers(Prime prevLastMatrixIndex) {

        valueMultiplier = valueMultiplier.divide(prevLastMatrixIndex.getValue());
        factorMultiplier = factorMultiplier.divide(new ScientificNumber(2, 0));
        if (higherLapi != null) {
            higherLapi.recalculateMultipliers(prevLastMatrixIndex);
        }
    }

    public Hcn generateHcn(Body body) {

        Hcn newHcn = Hcn.builder().body(body).lapi(prime.getIndex()).value(body.getValue().multiply(valueMultiplier))
                .factor(body.getFactor().multiply(factorMultiplier)).build();

        if (body.getFirstHcn() == null || body.getFirstHcn().getLapi() > prime.getIndex()) {
            body.setFirstHcn(newHcn);
        }

        if (body.getLastGeneratedHcn() == null || body.getLastGeneratedHcn().getLapi() < prime.getIndex()) {
            body.setLastGeneratedHcn(newHcn);
        }

        return newHcn;
    }
}
