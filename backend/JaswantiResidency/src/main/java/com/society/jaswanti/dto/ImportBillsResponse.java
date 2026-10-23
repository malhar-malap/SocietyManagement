package com.society.jaswanti.dto;

import java.util.ArrayList;
import java.util.List;

public class ImportBillsResponse {
    private int totalRows;
    private int createdCount;
    private int skippedCount;
    private List<String> errors = new ArrayList<>();

    public ImportBillsResponse() {}

    public ImportBillsResponse(int totalRows, int createdCount, int skippedCount, List<String> errors) {
        this.totalRows = totalRows;
        this.createdCount = createdCount;
        this.skippedCount = skippedCount;
        this.errors = errors != null ? errors : new ArrayList<>();
    }

    public int getTotalRows() { return totalRows; }
    public void setTotalRows(int totalRows) { this.totalRows = totalRows; }

    public int getCreatedCount() { return createdCount; }
    public void setCreatedCount(int createdCount) { this.createdCount = createdCount; }

    public int getSkippedCount() { return skippedCount; }
    public void setSkippedCount(int skippedCount) { this.skippedCount = skippedCount; }

    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors; }
}
