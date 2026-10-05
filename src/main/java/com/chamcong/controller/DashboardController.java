package com.chamcong.controller;

import com.chamcong.entity.Attendance;
import com.chamcong.entity.Employee;
import com.chamcong.entity.SalaryAdvance;
import com.chamcong.repository.AttendanceRepository;
import com.chamcong.repository.EmployeeRepository;
import com.chamcong.repository.SalaryAdvanceRepository;
import com.chamcong.service.AttendanceService;
import com.chamcong.service.PayrollService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Controller
public class DashboardController {

    private final EmployeeRepository employeeRepository;
    private final AttendanceRepository attendanceRepository;
    private final SalaryAdvanceRepository salaryAdvanceRepository;
    private final AttendanceService attendanceService;
    private final PayrollService payrollService;

    public DashboardController(
            EmployeeRepository employeeRepository,
            AttendanceRepository attendanceRepository,
            SalaryAdvanceRepository salaryAdvanceRepository,
            AttendanceService attendanceService,
            PayrollService payrollService
    ) {
        this.employeeRepository = employeeRepository;
        this.attendanceRepository = attendanceRepository;
        this.salaryAdvanceRepository = salaryAdvanceRepository;
        this.attendanceService = attendanceService;
        this.payrollService = payrollService;
    }

    @GetMapping("/")
    public String dashboard(Model model, Authentication authentication) {
        String username = authentication.getName();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        YearMonth ym = YearMonth.now();
        LocalDate from = ym.atDay(1);
        LocalDate to = ym.atEndOfMonth();

        double totalHours = 0.0;
        long attendanceDays = 0;
        long dossierRows = 0;
        BigDecimal grossSalary = BigDecimal.ZERO;
        BigDecimal totalAdvance = BigDecimal.ZERO;
        BigDecimal netSalary = BigDecimal.ZERO;
        List<Employee> activeEmployees;
        List<Attendance> attendances;

        if (isAdmin) {
            activeEmployees = employeeRepository.findByActiveTrueOrderByFullNameAsc();
            attendances = activeEmployees.stream()
                    .flatMap(e -> attendanceRepository
                            .findByEmployeeIdAndWorkDateBetweenOrderByWorkDateAsc(e.getId(), from, to).stream())
                    .toList();

            for (Employee e : activeEmployees) {
                List<Attendance> employeeAttendances = attendanceRepository
                        .findByEmployeeIdAndWorkDateBetweenOrderByWorkDateAsc(e.getId(), from, to);
                List<SalaryAdvance> advances = salaryAdvanceRepository
                        .findByEmployeeIdAndAdvanceDateBetweenOrderByAdvanceDateAsc(e.getId(), from, to);
                double hours = employeeAttendances.stream()
                        .mapToDouble(attendanceService::calculateTotalHours)
                        .sum();
                BigDecimal rate = e.getHourlyRate() == null ? BigDecimal.ZERO : e.getHourlyRate();
                BigDecimal gross = payrollService.calculateGrossSalary(hours, rate);
                BigDecimal unpaid = payrollService.calculateUnpaidAdvance(advances);
                totalHours += hours;
                grossSalary = grossSalary.add(gross);
                totalAdvance = totalAdvance.add(unpaid);
            }
            netSalary = payrollService.calculateNetSalary(grossSalary, totalAdvance);
        } else {
            Employee current = employeeRepository.findByUsername(username).orElseThrow();
            activeEmployees = List.of(current);
            attendances = attendanceRepository
                    .findByEmployeeIdAndWorkDateBetweenOrderByWorkDateAsc(current.getId(), from, to);
            List<SalaryAdvance> advances = salaryAdvanceRepository
                    .findByEmployeeIdAndAdvanceDateBetweenOrderByAdvanceDateAsc(current.getId(), from, to);
            totalHours = attendances.stream().mapToDouble(attendanceService::calculateTotalHours).sum();
            BigDecimal rate = current.getHourlyRate() == null ? BigDecimal.ZERO : current.getHourlyRate();
            grossSalary = payrollService.calculateGrossSalary(totalHours, rate);
            totalAdvance = payrollService.calculateUnpaidAdvance(advances);
            netSalary = payrollService.calculateNetSalary(grossSalary, totalAdvance);
        }

        attendanceDays = attendances.size();
        dossierRows = attendances.stream()
                .filter(a -> a.getDossierCount() != null && !a.getDossierCount().isBlank())
                .count();

        model.addAttribute("currentUser", username);
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("employeeCount", activeEmployees.size());
        model.addAttribute("attendanceDays", attendanceDays);
        model.addAttribute("totalHours", Math.round(totalHours * 100.0) / 100.0);
        model.addAttribute("totalDossiers", dossierRows);
        model.addAttribute("grossSalary", grossSalary);
        model.addAttribute("totalAdvance", totalAdvance);
        model.addAttribute("netSalary", netSalary);
        model.addAttribute("currentMonth", ym.toString());

        return "dashboard";
    }
}
