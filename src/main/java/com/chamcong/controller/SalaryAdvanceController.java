package com.chamcong.controller;

import com.chamcong.entity.Attendance;
import com.chamcong.entity.Employee;
import com.chamcong.entity.SalaryAdvance;
import com.chamcong.repository.AttendanceRepository;
import com.chamcong.repository.EmployeeRepository;
import com.chamcong.repository.SalaryAdvanceRepository;
import com.chamcong.service.AttendanceService;
import com.chamcong.service.PayrollService;
import com.chamcong.service.SalaryAdvanceService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@RequestMapping("/ung-luong")
public class SalaryAdvanceController {

    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final SalaryAdvanceRepository salaryAdvanceRepository;
    private final EmployeeRepository employeeRepository;
    private final AttendanceRepository attendanceRepository;
    private final SalaryAdvanceService salaryAdvanceService;
    private final AttendanceService attendanceService;
    private final PayrollService payrollService;

    public SalaryAdvanceController(
            SalaryAdvanceRepository salaryAdvanceRepository,
            EmployeeRepository employeeRepository,
            AttendanceRepository attendanceRepository,
            SalaryAdvanceService salaryAdvanceService,
            AttendanceService attendanceService,
            PayrollService payrollService
    ) {
        this.salaryAdvanceRepository = salaryAdvanceRepository;
        this.employeeRepository = employeeRepository;
        this.attendanceRepository = attendanceRepository;
        this.salaryAdvanceService = salaryAdvanceService;
        this.attendanceService = attendanceService;
        this.payrollService = payrollService;
    }

    @GetMapping
    public String index(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) String month,
            @RequestParam(required = false) Long editId,
            @RequestParam(defaultValue = "false") boolean add,
            Authentication authentication,
            Model model
    ) {
        if (authentication == null) {
            return "redirect:/login";
        }

        String username = authentication.getName();
        Employee currentEmployee = employeeRepository.findByUsername(username).orElse(null);

        if (currentEmployee == null || !currentEmployee.isActive()) {
            return "redirect:/login";
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        YearMonth selectedMonth = parseMonth(month);

        List<Employee> employees = isAdmin
                ? employeeRepository.findByActiveTrueOrderByFullNameAsc()
                : List.of(currentEmployee);

        Employee selectedEmployee;
        if (isAdmin) {
            selectedEmployee = employeeId == null
                    ? null
                    : employeeRepository.findById(employeeId).orElse(null);
        } else {
            selectedEmployee = currentEmployee;
            employeeId = currentEmployee.getId();
        }

        List<SalaryAdvance> advances = List.of();
        List<Attendance> attendances = List.of();

        double totalHours = 0.0;
        BigDecimal hourlyRate = BigDecimal.ZERO;
        BigDecimal grossSalary = BigDecimal.ZERO;
        BigDecimal totalAdvance = BigDecimal.ZERO;
        BigDecimal totalUnpaid = BigDecimal.ZERO;
        BigDecimal netSalary = BigDecimal.ZERO;

        if (selectedEmployee != null) {
            LocalDate start = selectedMonth.atDay(1);
            LocalDate end = selectedMonth.atEndOfMonth();

            advances = salaryAdvanceRepository
                    .findByEmployeeIdAndAdvanceDateBetweenOrderByAdvanceDateAsc(
                            selectedEmployee.getId(), start, end);

            attendances = attendanceRepository
                    .findByEmployeeIdAndWorkDateBetweenOrderByWorkDateAsc(
                            selectedEmployee.getId(), start, end);

            totalHours = attendances.stream()
                    .mapToDouble(attendanceService::calculateTotalHours)
                    .sum();

            hourlyRate = selectedEmployee.getHourlyRate() == null
                    ? BigDecimal.ZERO
                    : selectedEmployee.getHourlyRate();

            grossSalary = payrollService.calculateGrossSalary(totalHours, hourlyRate);
            totalAdvance = payrollService.calculateTotalAdvance(advances);
            totalUnpaid = payrollService.calculateUnpaidAdvance(advances);
            netSalary = payrollService.calculateNetSalary(grossSalary, totalUnpaid);
        }

        SalaryAdvance editAdvance = null;
        if (isAdmin && editId != null) {
            editAdvance = salaryAdvanceRepository.findById(editId).orElse(null);
        }

        // Khi bấm Thêm mà chưa chọn nhân viên, giữ form mở để admin chọn ngay trong form.
        boolean openAddForm = isAdmin && add;

        model.addAttribute("currentUser", username);
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("employees", employees);
        model.addAttribute("selectedEmployee", selectedEmployee);
        model.addAttribute("selectedEmployeeId", employeeId);
        model.addAttribute("selectedMonth", selectedMonth.format(MONTH_FORMAT));
        model.addAttribute("advances", advances);
        model.addAttribute("editAdvance", editAdvance);
        model.addAttribute("openAddForm", openAddForm);

        model.addAttribute("totalAdvance", totalAdvance);
        model.addAttribute("totalUnpaid", totalUnpaid);
        model.addAttribute("totalPaid", salaryAdvanceService.totalPaid(advances));
        model.addAttribute("totalHours", totalHours);
        model.addAttribute("hourlyRate", hourlyRate);
        model.addAttribute("grossSalary", grossSalary);
        model.addAttribute("netSalary", netSalary);
        model.addAttribute("attendanceDays", attendances.size());

        return "salary-advances";
    }

    @PostMapping("/luu")
    @PreAuthorize("hasRole('ADMIN')")
    public String save(
            @RequestParam(required = false) Long id,
            @RequestParam Long employeeId,
            @RequestParam String advanceDate,
            @RequestParam String amount,
            @RequestParam(required = false) String reason,
            @RequestParam(defaultValue = "CHUA_TRU") String status,
            @RequestParam(required = false) String note,
            @RequestParam(required = false) String month,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Employee employee = employeeRepository.findById(employeeId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhân viên."));

            if (!employee.isActive()) {
                throw new IllegalArgumentException("Không thể thao tác với nhân viên đã bị khóa.");
            }

            LocalDate date = LocalDate.parse(advanceDate);
            YearMonth selectedMonth = parseMonth(month);

            if (!YearMonth.from(date).equals(selectedMonth)) {
                throw new IllegalArgumentException(
                        "Ngày ứng lương phải nằm trong tháng đang chọn (" + selectedMonth.format(MONTH_FORMAT) + ")."
                );
            }

            SalaryAdvance advance;
            if (id == null) {
                advance = new SalaryAdvance();
            } else {
                advance = salaryAdvanceRepository.findById(id)
                        .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khoản ứng lương."));
            }

            advance.setEmployee(employee);
            advance.setAdvanceDate(date);
            advance.setAmount(parseAmount(amount));
            advance.setReason(blankToNull(reason));
            advance.setStatus(status);
            advance.setNote(blankToNull(note));

            salaryAdvanceService.save(advance);

            redirectAttributes.addFlashAttribute(
                    "success",
                    id == null ? "Đã thêm khoản ứng lương." : "Đã cập nhật khoản ứng lương."
            );
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage() == null ? "Không thể lưu khoản ứng lương." : e.getMessage()
            );
        }

        return redirectToList(employeeId, month);
    }

    @PostMapping("/trang-thai")
    @PreAuthorize("hasRole('ADMIN')")
    public String changeStatus(
            @RequestParam Long id,
            @RequestParam String status,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) String month,
            RedirectAttributes redirectAttributes
    ) {
        try {
            SalaryAdvance advance = salaryAdvanceRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khoản ứng lương."));

            if (!"CHUA_TRU".equals(status) && !"DA_TRU".equals(status)) {
                throw new IllegalArgumentException("Trạng thái không hợp lệ.");
            }

            advance.setStatus(status);
            salaryAdvanceService.save(advance);

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Đã chuyển trạng thái khoản ứng sang " + ("DA_TRU".equals(status) ? "Đã trừ" : "Chưa trừ") + "."
            );
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage() == null ? "Không thể cập nhật trạng thái." : e.getMessage()
            );
        }

        return redirectToList(employeeId, month);
    }

    @PostMapping("/xoa")
    @PreAuthorize("hasRole('ADMIN')")
    public String delete(
            @RequestParam Long id,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) String month,
            RedirectAttributes redirectAttributes
    ) {
        try {
            SalaryAdvance advance = salaryAdvanceRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khoản ứng lương."));

            salaryAdvanceService.deleteById(advance.getId());
            redirectAttributes.addFlashAttribute("success", "Đã xóa khoản ứng lương.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Không thể xóa khoản ứng lương: " + (e.getMessage() == null ? "lỗi không xác định." : e.getMessage())
            );
        }

        return redirectToList(employeeId, month);
    }

    private String redirectToList(Long employeeId, String month) {
        YearMonth selectedMonth = parseMonth(month);
        StringBuilder url = new StringBuilder("redirect:/ung-luong?month=")
                .append(selectedMonth.format(MONTH_FORMAT));

        if (employeeId != null) {
            url.append("&employeeId=").append(employeeId);
        }

        return url.toString();
    }

    private BigDecimal parseAmount(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập số tiền ứng lương.");
        }

        String normalized = value.trim().replace(" ", "");

        // Hỗ trợ cả 1000000, 1000000.50 và 1000000,50.
        if (normalized.contains(",") && normalized.contains(".")) {
            normalized = normalized.replace(",", "");
        } else if (normalized.contains(",")) {
            normalized = normalized.replace(',', '.');
        }

        try {
            return new BigDecimal(normalized);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Số tiền ứng lương không hợp lệ.");
        }
    }

    private YearMonth parseMonth(String month) {
        if (month == null || month.isBlank()) {
            return YearMonth.now();
        }

        try {
            return YearMonth.parse(month);
        } catch (Exception e) {
            return YearMonth.now();
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
