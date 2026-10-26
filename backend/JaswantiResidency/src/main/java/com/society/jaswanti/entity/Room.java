package com.society.jaswanti.entity;

import org.hibernate.Length;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "rooms")
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_number", nullable = false)
    private String roomNumber;

    @Column(nullable = false)
    private String wing;

    @Column(nullable = false)
    private Integer floor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resident_id")
    private User resident;

    @Column(name = "unit_category", nullable = false)
    private String unitCategory = "RESIDENTIAL";

    @Column(name = "noc_applicable", nullable = false)
    private boolean nocApplicable = false;

    @Column(name = "noc_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal nocAmount = BigDecimal.ZERO;

    @Column(name = "share_certificate_number")
    private String shareCertificateNumber;

    @Column(name = "share_certificate_issued_date")
    private LocalDate shareCertificateIssuedDate;

    @Lob
    @Column(name = "share_certificate_data", length = Length.LONG32) // maps to LONGBLOB on MySQL, not the 255-byte TINYBLOB default
    private byte[] shareCertificateData;

    @Column(name = "share_certificate_filename")
    private String shareCertificateFilename;

    @Column(name = "share_certificate_content_type")
    private String shareCertificateContentType;

    // Constructors
    public Room() {}

    public Room(String roomNumber, String wing, Integer floor) {
        this.roomNumber = roomNumber;
        this.wing = wing;
        this.floor = floor;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }

    public String getWing() { return wing; }
    public void setWing(String wing) { this.wing = wing; }

    public Integer getFloor() { return floor; }
    public void setFloor(Integer floor) { this.floor = floor; }

    public User getResident() { return resident; }
    public void setResident(User resident) { this.resident = resident; }

    public String getUnitCategory() { return unitCategory; }
    public void setUnitCategory(String unitCategory) { this.unitCategory = unitCategory; }

    public boolean isNocApplicable() { return nocApplicable; }
    public void setNocApplicable(boolean nocApplicable) { this.nocApplicable = nocApplicable; }

    public BigDecimal getNocAmount() { return nocAmount; }
    public void setNocAmount(BigDecimal nocAmount) { this.nocAmount = nocAmount; }

    public String getShareCertificateNumber() { return shareCertificateNumber; }
    public void setShareCertificateNumber(String shareCertificateNumber) { this.shareCertificateNumber = shareCertificateNumber; }

    public LocalDate getShareCertificateIssuedDate() { return shareCertificateIssuedDate; }
    public void setShareCertificateIssuedDate(LocalDate shareCertificateIssuedDate) { this.shareCertificateIssuedDate = shareCertificateIssuedDate; }

    public byte[] getShareCertificateData() { return shareCertificateData; }
    public void setShareCertificateData(byte[] shareCertificateData) { this.shareCertificateData = shareCertificateData; }

    public String getShareCertificateFilename() { return shareCertificateFilename; }
    public void setShareCertificateFilename(String shareCertificateFilename) { this.shareCertificateFilename = shareCertificateFilename; }

    public String getShareCertificateContentType() { return shareCertificateContentType; }
    public void setShareCertificateContentType(String shareCertificateContentType) { this.shareCertificateContentType = shareCertificateContentType; }
}
