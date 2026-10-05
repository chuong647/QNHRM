package com.chamcong;

import com.chamcong.entity.Attendance;
import com.chamcong.service.AttendanceService;
import com.chamcong.service.PayrollService;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QNHRMApplicationTests {

    @Test
    void calculatesAttendanceHoursWithoutDatabase() {
        Attendance attendance = new Attendance();
        attendance.setMorningIn(LocalTime.of(8, 0));
        attendance.setMorningOut(LocalTime.of(12, 0));
        attendance.setAfternoonIn(LocalTime.of(13, 30));
        attendance.setAfternoonOut(LocalTime.of(17, 30));
        attendance.setEveningIn(LocalTime.of(19, 0));
        attendance.setEveningOut(LocalTime.of(21, 0));

        double total = new AttendanceService().calculateTotalHours(attendance);

        assertEquals(10.0, total, 0.0001);
    }

    @Test
    void calculatesOvernightEveningHours() {
        Attendance attendance = new Attendance();
        attendance.setEveningIn(LocalTime.of(22, 0));
        attendance.setEveningOut(LocalTime.of(2, 0));

        double total = new AttendanceService().calculateTotalHours(attendance);

        assertEquals(4.0, total, 0.0001);
    }
    @Test
    void calculatesGrossSalaryFromHoursAndHourlyRate() {
        BigDecimal gross = new PayrollService().calculateGrossSalary(8.5, new BigDecimal("50000"));
        assertEquals(new BigDecimal("425000.00"), gross);
    }

}
