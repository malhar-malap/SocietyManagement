package com.society.jaswanti.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * What a client may set when creating or editing a room. Binding the Room entity directly let clients set the id
 * (overwriting another room), the resident link, or the share-certificate file. Null fields are left unchanged on
 * edit, so a partial update no longer resets the NOC flag.
 */
public class RoomRequest {
    private String roomNumber;
    private String wing;
    private Integer floor;
    private String unitCategory;
    private Boolean nocApplicable;
    private BigDecimal nocAmount;
    private String shareCertificateNumber;
    private LocalDate shareCertificateIssuedDate;

    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }

    public String getWing() { return wing; }
    public void setWing(String wing) { this.wing = wing; }

    public Integer getFloor() { return floor; }
    public void setFloor(Integer floor) { this.floor = floor; }

    public String getUnitCategory() { return unitCategory; }
    public void setUnitCategory(String unitCategory) { this.unitCategory = unitCategory; }

    public Boolean getNocApplicable() { return nocApplicable; }
    public void setNocApplicable(Boolean nocApplicable) { this.nocApplicable = nocApplicable; }

    public BigDecimal getNocAmount() { return nocAmount; }
    public void setNocAmount(BigDecimal nocAmount) { this.nocAmount = nocAmount; }

    public String getShareCertificateNumber() { return shareCertificateNumber; }
    public void setShareCertificateNumber(String shareCertificateNumber) { this.shareCertificateNumber = shareCertificateNumber; }

    public LocalDate getShareCertificateIssuedDate() { return shareCertificateIssuedDate; }
    public void setShareCertificateIssuedDate(LocalDate shareCertificateIssuedDate) { this.shareCertificateIssuedDate = shareCertificateIssuedDate; }
}
