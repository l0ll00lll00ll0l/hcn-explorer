package com.hcn.core;

import com.hcn.core.basicdata.Body;

import java.util.ArrayList;
import java.util.List;

public class HcnGenerator {
    private int basicDataId = -1;
    private Hcn lastGeneratedHcn = null;
    private HcnBody currentHcnBody;
    private Body body = null;
    private HcnGenerator smallerGenerator = null;
    private HcnGenerator largerGenerator = null;

    public HcnGenerator(HcnBody currentHcnBody) {
        this.currentHcnBody = currentHcnBody;
    }

    public Hcn getLastGeneratedHcn() { return lastGeneratedHcn; }
    public void setLastGeneratedHcn(Hcn hcn) { this.lastGeneratedHcn = hcn; }
    public HcnBody getCurrentHcnBody() { return currentHcnBody; }
    public void setCurrentHcnBody(HcnBody currentHcnBody) { this.currentHcnBody = currentHcnBody; }
    public int getBasicDataId() { return basicDataId; }
    public void setBasicDataId(int basicDataId) { this.basicDataId = basicDataId; }
    public HcnGenerator getSmallerGenerator() { return smallerGenerator; }
    public void setSmallerGenerator(HcnGenerator smallerGenerator) { this.smallerGenerator = smallerGenerator; }
    public HcnGenerator getLargerGenerator() { return largerGenerator; }
    public void setLargerGenerator(HcnGenerator largerGenerator) { this.largerGenerator = largerGenerator; }

    private List<LastActivePrimeIndexGroup> walkerGeneratorForLapi = new java.util.ArrayList<>();
    public List<LastActivePrimeIndexGroup> getWalkerGeneratorForLapi() { return walkerGeneratorForLapi; }
    public void addWalkerGeneratorForLapi(LastActivePrimeIndexGroup lapi) { walkerGeneratorForLapi.add(lapi); }
    public void removeWalkerGeneratorForLapi(LastActivePrimeIndexGroup lapi) { walkerGeneratorForLapi.remove(lapi); }

    public void gotDominated() {

        // remove from generator list
        if (smallerGenerator != null) {
            smallerGenerator.setLargerGenerator(largerGenerator);
        }
        if (largerGenerator != null) {
            largerGenerator.setSmallerGenerator(smallerGenerator);
        }
        smallerGenerator = null;
        largerGenerator = null;
        // deactivate on body side (pip removal + offspring cascade)
        currentHcnBody.gotDominated();
    }

    public Body getBody() {
        if (body == null) body = new Body(currentHcnBody);
        return body;
    }

    public Hcn generateNextHcn(LastActivePrimeIndexGroup lapiGroup) {
        if (canExtendFromPreviousLapi(lapiGroup)) {
            lastGeneratedHcn = extendFromPreviousLapi(lapiGroup);
        } else {
            lastGeneratedHcn = computeFromReference(lapiGroup);
        }
        return lastGeneratedHcn;
    }

    private boolean canExtendFromPreviousLapi(LastActivePrimeIndexGroup lapiGroup) {
        return lastGeneratedHcn != null
                && lastGeneratedHcn.getLastActivePrime() + 1 == lapiGroup.getLastActivePrimeIndex();
    }

    private Hcn extendFromPreviousLapi(LastActivePrimeIndexGroup lapiGroup) {
        Hcn newHcn = new Hcn(this, lastGeneratedHcn.getLastActivePrime() + 1);
        newHcn.setValue(lastGeneratedHcn.getValue().multiply(lapiGroup.getPrimeValue()));
        newHcn.setFactor(lastGeneratedHcn.getFactor().multiply(new ScientificNumber(2, 0)));
        return newHcn;
    }

    private Hcn computeFromReference(LastActivePrimeIndexGroup lapiGroup) {
        HcnGenerator referenceGenerator = findReferenceGeneratorDownwards(lapiGroup);

        if (referenceGenerator == null) {

            Hcn referenceHcn = lapiGroup.getLowerLapiGroup().getWalkerGenerator().lastGeneratedHcn;
            HcnBody referenceBody = lapiGroup.getLowerLapiGroup().getWalkerGenerator().currentHcnBody;
            ScientificNumber valMult = referenceBody.getValueMultiplier(currentHcnBody);
            ScientificNumber facMult = referenceBody.getFactorMultiplier(currentHcnBody);
            Hcn newHcn = new Hcn(this, referenceHcn.getLastActivePrime() + 1);
            newHcn.setValue(referenceHcn.getValue().multiply(valMult).multiply(lapiGroup.getPrimeValue()));
            newHcn.setFactor(referenceHcn.getFactor().multiply(facMult).multiply(new ScientificNumber(2, 0)));
            return newHcn;
        }

        Hcn referenceHcn = referenceGenerator.lastGeneratedHcn;
        ScientificNumber valMult = referenceGenerator.currentHcnBody.getValueMultiplier(currentHcnBody);
        ScientificNumber facMult = referenceGenerator.currentHcnBody.getFactorMultiplier(currentHcnBody);
        Hcn newHcn = new Hcn(this, referenceHcn.getLastActivePrime());
        newHcn.setValue(referenceHcn.getValue().multiply(valMult));
        newHcn.setFactor(referenceHcn.getFactor().multiply(facMult));
        return newHcn;
    }

    private HcnGenerator findReferenceGeneratorDownwards(LastActivePrimeIndexGroup lapiGroup) {
        if (smallerGenerator == null) {
            return null;
        }
        HcnGenerator ref = smallerGenerator;
        while (ref.lastGeneratedHcn == null
                || ref.lastGeneratedHcn.getLastActivePrime() != lapiGroup.getLastActivePrimeIndex()) {
            ref = ref.smallerGenerator;
            if (ref == null) return null;
        }
        return ref;
    }
}
