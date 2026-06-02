package com.hcn.core;

public class HcnGenerator {
    private int basicDataId = -1;
    private Hcn lastGeneratedHcn = null;
    private HcnBody currentHcnBody;
    private Body body = null;

    public HcnGenerator(HcnBody currentHcnBody) {
        this.currentHcnBody = currentHcnBody;
    }

    public Hcn getLastGeneratedHcn() { return lastGeneratedHcn; }
    public void setLastGeneratedHcn(Hcn hcn) { this.lastGeneratedHcn = hcn; }
    public HcnBody getCurrentHcnBody() { return currentHcnBody; }
    public void setCurrentHcnBody(HcnBody currentHcnBody) { this.currentHcnBody = currentHcnBody; }
    public int getBasicDataId() { return basicDataId; }
    public void setBasicDataId(int basicDataId) { this.basicDataId = basicDataId; }

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
        HcnBody referenceBody = findReferenceBody(lapiGroup);

        if (referenceBody == null) {
            Hcn referenceHcn = lapiGroup.getLowerLapiGroup().getWalkerBody().getHcnGenerator().lastGeneratedHcn;
            Hcn newHcn = new Hcn(this, referenceHcn.getLastActivePrime() + 1);
            newHcn.setValue(referenceHcn.getValue().multiply(referenceBody.getValueMultiplier(currentHcnBody)).multiply(lapiGroup.getPrimeValue()));
            newHcn.setFactor(referenceHcn.getFactor().multiply(referenceBody.getFactorMultiplier(currentHcnBody)).multiply(new ScientificNumber(2, 0)));
            return newHcn;
        }

        Hcn referenceHcn = referenceBody.getHcnGenerator().lastGeneratedHcn;
        Hcn newHcn = new Hcn(this, referenceHcn.getLastActivePrime());
        newHcn.setValue(referenceHcn.getValue().multiply(referenceBody.getValueMultiplier(currentHcnBody)));
        newHcn.setFactor(referenceHcn.getFactor().multiply(referenceBody.getFactorMultiplier(currentHcnBody)));
        return newHcn;
    }

    private HcnBody findReferenceBody(LastActivePrimeIndexGroup lapiGroup) {
        if (currentHcnBody.getSmallerBody() == null) {
            return null;
        }
        HcnBody referenceBody = currentHcnBody.getSmallerBody();
        while (referenceBody.getHcnGenerator() == null || referenceBody.getHcnGenerator().lastGeneratedHcn == null
                || referenceBody.getHcnGenerator().lastGeneratedHcn.getLastActivePrime() != lapiGroup.getLastActivePrimeIndex()) {
            referenceBody = referenceBody.getSmallerBody();
        }
        return referenceBody;
    }
}
