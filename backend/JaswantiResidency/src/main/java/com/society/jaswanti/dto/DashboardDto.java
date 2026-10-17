package com.society.jaswanti.dto;

import java.util.List;

public class DashboardDto {
    private List<AnnouncementDto> announcements;
    private List<ComplaintDto> complaints;
    private List<MaintenanceBillDto> bills;

    public DashboardDto() {}

    public DashboardDto(List<AnnouncementDto> announcements, List<ComplaintDto> complaints, List<MaintenanceBillDto> bills) {
        this.announcements = announcements;
        this.complaints = complaints;
        this.bills = bills;
    }

    public List<AnnouncementDto> getAnnouncements() { return announcements; }
    public void setAnnouncements(List<AnnouncementDto> announcements) { this.announcements = announcements; }

    public List<ComplaintDto> getComplaints() { return complaints; }
    public void setComplaints(List<ComplaintDto> complaints) { this.complaints = complaints; }

    public List<MaintenanceBillDto> getBills() { return bills; }
    public void setBills(List<MaintenanceBillDto> bills) { this.bills = bills; }
}
