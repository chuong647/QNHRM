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
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
public class PayrollSlipController {

    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final EmployeeRepository employeeRepository;
    private final AttendanceRepository attendanceRepository;
    private final SalaryAdvanceRepository salaryAdvanceRepository;
    private final AttendanceService attendanceService;
    private final PayrollService payrollService;

    public PayrollSlipController(
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

    @GetMapping("/phieu-luong")
    public String payslip(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) String month,
            Authentication authentication,
            Model model
    ) {
        Employee current = employeeRepository.findByUsername(authentication.getName())
                .orElseThrow();

        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        Employee selected = admin
                ? (employeeId == null ? current : employeeRepository.findById(employeeId).orElse(null))
                : current;

        if (selected == null || !selected.isActive()) {
            return "redirect:/";
        }

        YearMonth ym = parseMonth(month);
        LocalDate from = ym.atDay(1);
        LocalDate to = ym.atEndOfMonth();

        List<Attendance> attendances = attendanceRepository
                .findByEmployeeIdAndWorkDateBetweenOrderByWorkDateAsc(selected.getId(), from, to);
        List<SalaryAdvance> advances = salaryAdvanceRepository
                .findByEmployeeIdAndAdvanceDateBetweenOrderByAdvanceDateAsc(selected.getId(), from, to);

        double totalHours = attendances.stream()
                .mapToDouble(attendanceService::calculateTotalHours)
                .sum();

        BigDecimal hourlyRate = selected.getHourlyRate() == null ? BigDecimal.ZERO : selected.getHourlyRate();
        BigDecimal gross = payrollService.calculateGrossSalary(totalHours, hourlyRate);
        BigDecimal totalAdvance = payrollService.calculateTotalAdvance(advances);
        BigDecimal unpaidAdvance = payrollService.calculateUnpaidAdvance(advances);
        BigDecimal net = payrollService.calculateNetSalary(gross, unpaidAdvance);

        model.addAttribute("currentUser", current.getUsername());
        model.addAttribute("isAdmin", admin);
        model.addAttribute("selectedEmployee", selected);
        model.addAttribute("employees", admin ? employeeRepository.findByActiveTrueOrderByFullNameAsc() : List.of(current));
        model.addAttribute("month", ym.format(MONTH_FORMAT));
        model.addAttribute("monthLabel", String.format("Tháng %02d/%d", ym.getMonthValue(), ym.getYear()));
        model.addAttribute("attendances", attendances);
        model.addAttribute("advances", advances);
        model.addAttribute("totalHours", Math.round(totalHours * 100.0) / 100.0);
        model.addAttribute("attendanceDays", attendances.size());
        model.addAttribute("hourlyRate", hourlyRate);
        model.addAttribute("grossSalary", gross);
        model.addAttribute("totalAdvance", totalAdvance);
        model.addAttribute("unpaidAdvance", unpaidAdvance);
        model.addAttribute("netSalary", net);
        model.addAttribute("attendanceService", attendanceService);

        return "payroll-slip";
    }

    private YearMonth parseMonth(String month) {
        if (month == null || month.isBlank()) return YearMonth.now();
        try {
            return YearMonth.parse(month, MONTH_FORMAT);
        } catch (Exception e) {
            return YearMonth.now();
        }
    }
}
