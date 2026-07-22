package com.hcn.event;

import lombok.Getter;

@Getter
public class SqlInsertActivity extends MainProcessActivity {

    private final SqlTable table;
    private final int rowCount;

    public SqlInsertActivity(SqlTable table, int rowCount) {
        super();
        this.table = table;
        this.rowCount = rowCount;
    }

    public SqlInsertActivity(SqlTable table, int rowCount, long startNanos, long finishNanos) {
        super(startNanos, finishNanos);
        this.table = table;
        this.rowCount = rowCount;
    }

    public void finish() {
        super.finish();
        ActivityCenter.getDbInsertService().submitSqlInsertActivity(this);
    }

    @Override
    public String getLabelName() { return "Sql Insert"; }

    @Override
    public String getGuiLabel() {
        return "duration: " + formatDuration() + " | table: " + table + " | rows: " + rowCount;
    }
}
