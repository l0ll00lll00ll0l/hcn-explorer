package com.hcn.event;

import com.hcn.newCore.Body;
import lombok.Getter;

import java.util.List;

@Getter
public class BodyDeletionEvent extends Event {

    private final List<Body> deletedBodies;

    public BodyDeletionEvent(List<Body> deletedBodies) {
        super();
        this.deletedBodies = deletedBodies;
    }
}
