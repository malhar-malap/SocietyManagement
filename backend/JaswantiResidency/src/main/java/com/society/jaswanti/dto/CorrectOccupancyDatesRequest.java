package com.society.jaswanti.dto;

import java.time.LocalDate;

public class CorrectOccupancyDatesRequest {
    private LocalDate startDate;
    private LocalDate endDate;
    private String reason;

    public CorrectOccupancyDatesRequest() {}

    public CorrectOccupancyDatesRequest(LocalDate startDate, LocalDate endDate, String reason) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.reason = reason;
    }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
