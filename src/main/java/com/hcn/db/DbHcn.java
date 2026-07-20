package com.hcn.db;

import com.hcn.newCore.ScientificNumber;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

@Getter @Setter
public class DbHcn {
    private final long id;
    private final int bodyId;
    private final int lapi;
    private DbBody dbBody;
    private TreeMap<Integer, Integer> referenceIndexes = null;
    private ScientificNumber value;
    private ScientificNumber factor;

    public DbHcn(long id, int bodyId, int lapi) {
        this.id = id;
        this.bodyId = bodyId;
        this.lapi = lapi;
    }

    public TreeMap<Integer, Integer> getReferenceIndexes() {
        if (referenceIndexes != null) return referenceIndexes;
        referenceIndexes = new TreeMap<>();
        for (int i = 0; i < dbBody.getHead().length; i++) {
            referenceIndexes.put(i, dbBody.getHead()[i]);
        }
        referenceIndexes.put(dbBody.getHead().length, dbBody.getTail().length + 1);
        for (int i = 0; i < dbBody.getTail().length; i++) {
            referenceIndexes.put(dbBody.getTail()[i] + 1, dbBody.getTail().length - i);
        }
        referenceIndexes.put(lapi + 1, 0);
        return referenceIndexes;
    }
}
