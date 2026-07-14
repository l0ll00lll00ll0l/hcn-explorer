package com.hcn.event;

import com.hcn.newCore.ApiNode;
import com.hcn.newCore.BodyNode;
import com.hcn.newCore.TransitionNode;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class MatrixExtensionActivity extends Activity {

    private final int index;
    private final int power;
    private int createdActiveBodyCount = 0;
    private int deactivatedBodyCount = 0;

    public MatrixExtensionActivity(BodyNode createdBodyNode) {
        super();
        ActivityCenter.interruptHcnGeneration();
        if (createdBodyNode.getParentNode() instanceof ApiNode apiNode) {
            this.index = apiNode.getIndexes().get(0).getIndex();
            this.power = createdBodyNode.getBodyNodeId();
        } else {
            TransitionNode transitionNode = (TransitionNode) createdBodyNode.getParentNode();
            this.index = transitionNode.getIndexes().get(transitionNode.getIndexes().size() - 1).getIndex();
            this.power = transitionNode.getTransitionFrom();
        }
        ActivityCenter.addExtensionActivity(this);
    }

    public MatrixExtensionActivity(int index, int power, int createdActiveBodyCount, int deactivatedBodyCount, long startNanos, long finishNanos) {
        super(startNanos, finishNanos);
        this.index = index;
        this.power = power;
        this.createdActiveBodyCount = createdActiveBodyCount;
        this.deactivatedBodyCount = deactivatedBodyCount;
    }

    @Override
    public String getLabelName() { return "Extension"; }

    @Override
    public String getGuiLabel() {
        return "duration: " + formatDuration() + " | p" + index + "^" + power + " | active: " + createdActiveBodyCount + " | deact: " + deactivatedBodyCount;
    }

    public void finish() {
        super.finish();
        ActivityCenter.getDbInsertService().submitExtension(this);
        ActivityCenter.resumeHcnGeneration();
    }
}
