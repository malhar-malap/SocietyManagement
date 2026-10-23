package com.society.jaswanti.dto;

import java.time.LocalDate;

public class ShareCertificateDto {
    private Long roomId;
    private String roomNumber;
    private String wing;
    private String unitCategory;
    private String shareCertificateNumber;
    private LocalDate shareCertificateIssuedDate;
    private boolean hasDocument;
    private String filename;
    private String contentType;

    public ShareCertificateDto() {}

    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }

    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }

    public String getWing() { return wing; }
    public void setWing(String wing) { this.wing = wing; }

    public String getUnitCategory() { return unitCategory; }
    public void setUnitCategory(String unitCategory) { this.unitCategory = unitCategory; }

    public String getShareCertificateNumber() { return shareCertificateNumber; }
    public void setShareCertificateNumber(String shareCertificateNumber) { this.shareCertificateNumber = shareCertificateNumber; }

    public LocalDate getShareCertificateIssuedDate() { return shareCertificateIssuedDate; }
    public void setShareCertificateIssuedDate(LocalDate shareCertificateIssuedDate) { this.shareCertificateIssuedDate = shareCertificateIssuedDate; }

    public boolean isHasDocument() { return hasDocument; }
    public void setHasDocument(boolean hasDocument) { this.hasDocument = hasDocument; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
}
