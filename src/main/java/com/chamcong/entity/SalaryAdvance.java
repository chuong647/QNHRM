package com.chamcong.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "salary_advances")
public class SalaryAdvance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    // =========================================================
    // NHÂN VIÊN
    // =========================================================

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;


    // =========================================================
    // NGÀY ỨNG LƯƠNG
    // =========================================================

    @Column(name = "advance_date", nullable = false)
    private LocalDate advanceDate;


    // =========================================================
    // SỐ TIỀN ỨNG
    // =========================================================

    @Column(
            name = "amount",
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal amount = BigDecimal.ZERO;


    // =========================================================
    // LÝ DO
    // =========================================================

    @Column(name = "reason", length = 500)
    private String reason;


    // =========================================================
    // TRẠNG THÁI
    // =========================================================

    /*
     * CHUA_TRU = khoản ứng chưa được trừ vào lương
     * DA_TRU   = khoản ứng đã được trừ vào lương
     */
    @Column(
            name = "status",
            nullable = false,
            length = 30
    )
    private String status = "CHUA_TRU";


    // =========================================================
    // GHI CHÚ
    // =========================================================

    @Column(name = "note", length = 500)
    private String note;


    // =========================================================
    // THỜI GIAN TẠO / CẬP NHẬT
    // =========================================================

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;


    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public SalaryAdvance() {
    }


    // =========================================================
    // GETTER / SETTER
    // =========================================================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }


    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }


    public LocalDate getAdvanceDate() {
        return advanceDate;
    }

    public void setAdvanceDate(LocalDate advanceDate) {
        this.advanceDate = advanceDate;
    }


    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }


    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }


    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }


    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }


    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }


    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }


    // =========================================================
    // JPA CALLBACK
    // =========================================================

    @PrePersist
    protected void onCreate() {

        LocalDateTime now = LocalDateTime.now();

        if (createdAt == null) {
            createdAt = now;
        }

        if (updatedAt == null) {
            updatedAt = now;
        }

        if (amount == null) {
            amount = BigDecimal.ZERO;
        }

        if (status == null || status.isBlank()) {
            status = "CHUA_TRU";
        }
    }


    @PreUpdate
    protected void onUpdate() {

        updatedAt = LocalDateTime.now();

        if (amount == null) {
            amount = BigDecimal.ZERO;
        }

        if (status == null || status.isBlank()) {
            status = "CHUA_TRU";
        }
    }
}