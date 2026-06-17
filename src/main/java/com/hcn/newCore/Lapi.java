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
        if (walker == null || walker.isDeactivated() || walker.getNextActiveBody() == null) {
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
        //log.debug("Restoring walker required for lapi {} prev walkerBody: {}", prime.getIndex(), walker);
        Body result = null;
        Body candidate = smallestBody;
        while (candidate != null) {
            if (!candidate.isDeactivated() && candidate.getLastGeneratedHcn() != null && candidate.getLastGeneratedHcn().getLapi() == prime.getIndex()) {
                result = candidate;
            }
            candidate = candidate.getNextActiveBody();
        }
        if (result == null) {
            //log.debug("  Restoring walker for lapi {} no body was found, return null", prime.getIndex());
            return null;
        }
        //log.debug("  Restoring walker for lapi {} last ActiveBody with lapi as lastgenHcn: {}", prime.getIndex(), result);
        // advance until lastGeneratedHcn value > provedLimit
        while (result.getLastGeneratedHcn().getValue().isNotBiggerThan(provedLimit)) {
            //log.debug("  Restoring walker for lapi {} results lastgenhcn is smaller than provedlimit: {}", prime.getIndex(), result);
            Body next = result.getNextActiveBody();
            if (next == null) return null;
            result = next;
            if (result.getLastGeneratedHcn() == null || result.getLastGeneratedHcn().getLapi() != prime.getIndex()) {
                result.generateNextHcn(this);
            }
        }
        //log.debug("  Restoring walker for lapi {} new walker identified: {}", prime.getIndex(), result);
        return result;
    }

    private void createBaseHcnList(ScientificNumber targetValue) {
        while (walker.getLastGeneratedHcn().getValue().isSmallerThan(targetValue)) {
            hcnList.add(walker.getLastGeneratedHcn());
            Body walkercandidate = walker.getNextActiveBody();
            if (walkercandidate == null) {
                break;
            }
            walker = walkercandidate;
            walker.generateNextHcn(this);
        }
    }

    private void moveWalkerIfNotSuperiorCheck() {
        // first local hcns might be dominated by lower lapi hcn recorder
        if (lowerLapi != null) {
            //check needed only if local recorder is from lower lapi
            if (hcnList.get(hcnList.size() - 1).getLapi() < prime.getIndex()) {
                while (walker.getLastGeneratedHcn().getFactor().isNotBiggerThan(hcnList.get(hcnList.size() - 1).getFactor())) {
                    walker.getLastGeneratedHcn().gotDominated();
                    walker = walker.getNextActiveBody();
                    walker.generateNextHcn(this);
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
}
