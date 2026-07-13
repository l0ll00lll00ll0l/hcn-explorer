package com.hcn.event;

import lombok.Getter;

@Getter
public class SqlInsertActivity extends Activity {

    private final SqlTable table;
    private final int rowCount;

    public SqlInsertActivity(SqlTable table, int rowCount) {
        super();
        this.table = table;
        this.rowCount = rowCount;
        ActivityCenter.getSqlInsertActivities().add(this);
    }

    @Override
    public String getLabelName() { return "Sql Insert"; }

    @Override
    public String getGuiLabel() {
        return "duration: " + formatDuration() + " | table: " + table + " | rows: " + rowCount;
    }
}
