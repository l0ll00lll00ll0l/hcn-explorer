package com.hcn.newCore;

import java.util.List;

public interface MatrixNode {

    MatrixNode getPrevMatrixNode();
    void setPrevMatrixNode(MatrixNode node);

    MatrixNode getNextMatrixNode();
    void setNextMatrixNode(MatrixNode node);

    void deactivatedMaintain();

    void generateNewBodies(List<Body> successfullyAddedLocalBodies);
}
