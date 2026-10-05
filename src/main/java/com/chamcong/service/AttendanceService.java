package com.chamcong.service;

import com.chamcong.entity.Attendance;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalTime;

@Service
public class AttendanceService {

    public double calculateTotalHours(Attendance attendance) {

        double total = 0;

        total += calculateShift(
                attendance.getMorningIn(),
                attendance.getMorningOut()
        );

        total += calculateShift(
                attendance.getAfternoonIn(),
                attendance.getAfternoonOut()
        );

        total += calculateShift(
                attendance.getEveningIn(),
                attendance.getEveningOut()
        );

        return Math.round(total * 100.0) / 100.0;
    }

    private double calculateShift(
            LocalTime start,
            LocalTime end
    ) {

        if (start == null || end == null) {
            return 0;
        }

        long seconds;

        if (end.isBefore(start)) {

            seconds = Duration.between(
                    start,
                    end
            ).getSeconds();

            seconds += 24 * 60 * 60;

        } else {

            seconds = Duration.between(
                    start,
                    end
            ).getSeconds();
        }

        return seconds / 3600.0;
    }
}