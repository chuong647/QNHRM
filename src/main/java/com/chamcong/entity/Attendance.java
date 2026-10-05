package com.chamcong.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(
        name = "attendances",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_employee_date",
                columnNames = {"employee_id", "work_date"}
        )
)
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    // =========================================================
    // NHÂN VIÊN
    // =========================================================

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "employee_id",
            nullable = false
    )
    private Employee employee;


    // =========================================================
    // NGÀY CHẤM CÔNG
    // =========================================================

    @Column(
            name = "work_date",
            nullable = false
    )
    private LocalDate workDate;


    // =========================================================
    // CA SÁNG
    // =========================================================

    @Column(name = "morning_in")
    private LocalTime morningIn;

    @Column(name = "morning_out")
    private LocalTime morningOut;


    // =========================================================
    // CA CHIỀU
    // =========================================================

    @Column(name = "afternoon_in")
    private LocalTime afternoonIn;

    @Column(name = "afternoon_out")
    private LocalTime afternoonOut;


    // =========================================================
    // CA TỐI
    // =========================================================

    @Column(name = "evening_in")
    private LocalTime eveningIn;

    @Column(name = "evening_out")
    private LocalTime eveningOut;


    // =========================================================
    // HỒ SƠ
    // =========================================================
    //
    // Đây là CHUỖI, không phải số.
    //
    // Ví dụ:
    //
    // G123-G210
    //
    // hoặc:
    //
    // G123-G210, G215-G230
    //

    @Column(
            name = "dossier_info",
            nullable = false,
            length = 1000
    )
    private String dossierCount = "";


    // =========================================================
    // ĐI MUỘN
    // =========================================================

    @Column(
            name = "late_minutes",
            nullable = false
    )
    private Integer lateMinutes = 0;


    // =========================================================
    // VỀ SỚM
    // =========================================================

    @Column(
            name = "early_leave_minutes",
            nullable = false
    )
    private Integer earlyLeaveMinutes = 0;


    // =========================================================
    // GHI CHÚ
    // =========================================================

    @Column(
            name = "note",
            length = 500
    )
    private String note;


    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public Attendance() {
    }


    // =========================================================
    // GETTER
    // =========================================================

    public Long getId() {
        return id;
    }

    public Employee getEmployee() {
        return employee;
    }

    public LocalDate getWorkDate() {
        return workDate;
    }

    public LocalTime getMorningIn() {
        return morningIn;
    }

    public LocalTime getMorningOut() {
        return morningOut;
    }

    public LocalTime getAfternoonIn() {
        return afternoonIn;
    }

    public LocalTime getAfternoonOut() {
        return afternoonOut;
    }

    public LocalTime getEveningIn() {
        return eveningIn;
    }

    public LocalTime getEveningOut() {
        return eveningOut;
    }

    public String getDossierCount() {
        return dossierCount;
    }

    public Integer getLateMinutes() {
        return lateMinutes;
    }

    public Integer getEarlyLeaveMinutes() {
        return earlyLeaveMinutes;
    }

    public String getNote() {
        return note;
    }


    // =========================================================
    // SETTER
    // =========================================================

    public void setId(Long id) {
        this.id = id;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public void setWorkDate(LocalDate workDate) {
        this.workDate = workDate;
    }

    public void setMorningIn(LocalTime morningIn) {
        this.morningIn = morningIn;
    }

    public void setMorningOut(LocalTime morningOut) {
        this.morningOut = morningOut;
    }

    public void setAfternoonIn(LocalTime afternoonIn) {
        this.afternoonIn = afternoonIn;
    }

    public void setAfternoonOut(LocalTime afternoonOut) {
        this.afternoonOut = afternoonOut;
    }

    public void setEveningIn(LocalTime eveningIn) {
        this.eveningIn = eveningIn;
    }

    public void setEveningOut(LocalTime eveningOut) {
        this.eveningOut = eveningOut;
    }


    // =========================================================
    // HỒ SƠ
    // =========================================================

    public void setDossierCount(String dossierCount) {

        this.dossierCount =
                dossierCount == null
                        ? ""
                        : dossierCount.trim();
    }


    // =========================================================
    // ĐI MUỘN
    // =========================================================

    public void setLateMinutes(Integer lateMinutes) {

        this.lateMinutes =
                lateMinutes == null
                        ? 0
                        : Math.max(0, lateMinutes);
    }


    // =========================================================
    // VỀ SỚM
    // =========================================================

    public void setEarlyLeaveMinutes(Integer earlyLeaveMinutes) {

        this.earlyLeaveMinutes =
                earlyLeaveMinutes == null
                        ? 0
                        : Math.max(0, earlyLeaveMinutes);
    }


    // =========================================================
    // GHI CHÚ
    // =========================================================

    public void setNote(String note) {
        this.note = note;
    }
}