package com.sucrestore.api.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class ImportSummary {

    private int totalProcessed;
    private int successCount;
    private int failureCount;
    private List<String> errorMessages = new ArrayList<>();

    public void addError(int row, String message) {
        errorMessages.add("Ligne " + row + ": " + message);
        failureCount++;
    }

    public void incrementSuccess() {
        successCount++;
    }

    public void incrementTotal() {
        totalProcessed++;
    }

    public int getErrorCount() {
        return failureCount;
    }
}
