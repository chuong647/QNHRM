package com.chamcong.controller;

import com.chamcong.entity.Employee;
import com.chamcong.repository.EmployeeRepository;
import com.chamcong.repository.AttendanceRepository;
import com.chamcong.repository.SalaryAdvanceRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Controller
@RequestMapping("/nhan-vien")
public class EmployeeController {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final AttendanceRepository attendanceRepository;
    private final SalaryAdvanceRepository salaryAdvanceRepository;

    public EmployeeController(EmployeeRepository employeeRepository,
                              PasswordEncoder passwordEncoder,
                              AttendanceRepository attendanceRepository,
                              SalaryAdvanceRepository salaryAdvanceRepository) {
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
        this.attendanceRepository = attendanceRepository;
        this.salaryAdvanceRepository = salaryAdvanceRepository;
    }

    @GetMapping
    public String index(@RequestParam(value = "q", required = false) String q,
                        @RequestParam(value = "editId", required = false) Long editId,
                        Model model) {

        String keyword = q == null ? "" : q.trim();

        List<Employee> employees = employeeRepository.findAll().stream()
                .filter(e -> keyword.isBlank()
                        || contains(e.getFullName(), keyword)
                        || contains(e.getUsername(), keyword)
                        || contains(e.getPosition(), keyword))
                .sorted(Comparator.comparing(
                        e -> e.getFullName() == null ? "" : e.getFullName(),
                        String.CASE_INSENSITIVE_ORDER))
                .toList();

        Employee editEmployee = null;
        if (editId != null) {
            editEmployee = employeeRepository.findById(editId).orElse(null);
            if (editEmployee == null) {
                model.addAttribute("error", "Không tìm thấy nhân viên cần sửa.");
            }
        }

        model.addAttribute("employees", employees);
        model.addAttribute("editEmployee", editEmployee);
        model.addAttribute("keyword", keyword);
        model.addAttribute("openForm", editEmployee != null);
        model.addAttribute("employeeCount", employees.size());
        model.addAttribute("activeCount",
                employees.stream().filter(Employee::isActive).count());
        model.addAttribute("adminCount",
                employees.stream().filter(e -> "ADMIN".equals(e.getRole())).count());

        return "employees";
    }

    @PostMapping("/save")
    @PreAuthorize("hasRole('ADMIN')")
    public String save(@RequestParam(value = "id", required = false) Long id,
                       @RequestParam("fullName") String fullName,
                       @RequestParam("username") String username,
                       @RequestParam(value = "newPassword", required = false) String newPassword,
                       @RequestParam(value = "hourlyRate", required = false) String hourlyRateRaw,
                       @RequestParam(value = "position", required = false) String position,
                       @RequestParam(value = "role", required = false, defaultValue = "STAFF") String role,
                       @RequestParam(value = "active", required = false) String activeRaw,
                       Authentication authentication,
                       RedirectAttributes redirectAttributes) {

        String normalizedName = trim(fullName);
        String normalizedUsername = trim(username).toLowerCase();
        String normalizedPosition = trimToNull(position);
        String normalizedRole = trim(role).toUpperCase();
        boolean active = "on".equalsIgnoreCase(activeRaw) || "true".equalsIgnoreCase(activeRaw);

        if (normalizedName.isBlank()) {
            return fail(redirectAttributes, id, "Họ và tên không được để trống.");
        }
        if (normalizedName.length() > 150) {
            return fail(redirectAttributes, id, "Họ và tên không được vượt quá 150 ký tự.");
        }

        if (normalizedUsername.isBlank()) {
            return fail(redirectAttributes, id, "Tên đăng nhập không được để trống.");
        }
        if (!normalizedUsername.matches("[a-z0-9._-]{3,80}")) {
            return fail(redirectAttributes, id, "Username chỉ được chứa a-z, 0-9, dấu chấm, gạch dưới hoặc gạch ngang; dài 3-80 ký tự.");
        }

        if (!normalizedRole.equals("ADMIN") && !normalizedRole.equals("STAFF")) {
            return fail(redirectAttributes, id, "Quyền chỉ được là ADMIN hoặc STAFF.");
        }

        BigDecimal hourlyRate;
        try {
            String raw = trim(hourlyRateRaw);
            hourlyRate = raw.isBlank() ? BigDecimal.ZERO : new BigDecimal(raw);
            if (hourlyRate.signum() < 0) {
                return fail(redirectAttributes, id, "Mức lương theo giờ không được âm.");
            }
        } catch (NumberFormatException ex) {
            return fail(redirectAttributes, id, "Mức lương theo giờ không hợp lệ.");
        }

        Employee employee;
        if (id == null) {
            employee = new Employee();
            if (newPassword == null || newPassword.isBlank()) {
                return fail(redirectAttributes, null, "Nhân viên mới phải có mật khẩu.");
            }
            if (newPassword.length() < 8) {
                return fail(redirectAttributes, null, "Mật khẩu phải có ít nhất 8 ký tự.");
            }
            employee.setPasswordHash(passwordEncoder.encode(newPassword));
        } else {
            employee = employeeRepository.findById(id).orElse(null);
            if (employee == null) {
                return fail(redirectAttributes, null, "Không tìm thấy nhân viên cần cập nhật.");
            }

            boolean isCurrentUser = authentication != null
                    && employee.getUsername() != null
                    && employee.getUsername().equalsIgnoreCase(authentication.getName());

            if (isCurrentUser && (!active || "STAFF".equals(normalizedRole))) {
                return fail(redirectAttributes, id, "Không thể khóa hoặc hạ quyền tài khoản đang đăng nhập.");
            }

            if (employee.isActive()
                    && "ADMIN".equals(employee.getRole())
                    && (!active || "STAFF".equals(normalizedRole))) {
                long activeAdmins = employeeRepository.findAll().stream()
                        .filter(Employee::isActive)
                        .filter(e -> "ADMIN".equals(e.getRole()))
                        .count();
                if (activeAdmins <= 1) {
                    return fail(redirectAttributes, id, "Không thể loại bỏ ADMIN cuối cùng đang hoạt động.");
                }
            }

            if (newPassword != null && !newPassword.isBlank()) {
                if (newPassword.length() < 8) {
                    return fail(redirectAttributes, id, "Mật khẩu mới phải có ít nhất 8 ký tự.");
                }
                employee.setPasswordHash(passwordEncoder.encode(newPassword));
            }
        }

        var existing = employeeRepository.findByUsername(normalizedUsername).orElse(null);
        if (existing != null && !existing.getId().equals(employee.getId())) {
            return fail(redirectAttributes, id, "Tên đăng nhập đã tồn tại.");
        }

        employee.setFullName(normalizedName);
        employee.setUsername(normalizedUsername);
        employee.setHourlyRate(hourlyRate);
        if (normalizedPosition != null && normalizedPosition.length() > 100) {
            return fail(redirectAttributes, id, "Chức vụ không được vượt quá 100 ký tự.");
        }
        employee.setPosition(normalizedPosition);
        employee.setRole(normalizedRole);
        employee.setActive(active);

        employeeRepository.save(employee);

        redirectAttributes.addFlashAttribute(
                "success",
                id == null ? "Đã thêm nhân viên thành công." : "Đã cập nhật nhân viên thành công."
        );

        return "redirect:/nhan-vien";
    }


    @PostMapping("/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String delete(@RequestParam("id") Long id,
                         Authentication authentication,
                         RedirectAttributes redirectAttributes) {

        Employee employee = employeeRepository.findById(id).orElse(null);

        if (employee == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy nhân viên cần xóa.");
            return "redirect:/nhan-vien";
        }

        // Không cho xóa tài khoản đang đăng nhập.
        if (authentication != null
                && employee.getUsername() != null
                && employee.getUsername().equalsIgnoreCase(authentication.getName())) {
            redirectAttributes.addFlashAttribute("error",
                    "Không thể xóa tài khoản đang đăng nhập. Hãy dùng chức năng Khóa nếu cần.");
            return "redirect:/nhan-vien";
        }

        // Không xóa ADMIN cuối cùng đang hoạt động.
        if (employee.isActive() && "ADMIN".equals(employee.getRole())) {
            long activeAdmins = employeeRepository.findAll().stream()
                    .filter(Employee::isActive)
                    .filter(e -> "ADMIN".equals(e.getRole()))
                    .count();
            if (activeAdmins <= 1) {
                redirectAttributes.addFlashAttribute("error",
                        "Không thể xóa ADMIN cuối cùng đang hoạt động.");
                return "redirect:/nhan-vien";
            }
        }

        // Database đang dùng ON DELETE RESTRICT cho dữ liệu chấm công/ứng lương.
        // Chỉ cho xóa cứng khi nhân viên chưa có dữ liệu liên quan.
        boolean hasAttendance = attendanceRepository.existsByEmployeeId(id);
        boolean hasSalaryAdvance = salaryAdvanceRepository.existsByEmployeeId(id);

        if (hasAttendance || hasSalaryAdvance) {
            redirectAttributes.addFlashAttribute("error",
                    "Không thể xóa nhân viên vì đã có dữ liệu chấm công hoặc ứng lương. "
                            + "Hãy dùng Khóa tài khoản để giữ lịch sử.");
            return "redirect:/nhan-vien";
        }

        employeeRepository.delete(employee);

        redirectAttributes.addFlashAttribute("success",
                "Đã xóa nhân viên \"" + employee.getFullName() + "\".");

        return "redirect:/nhan-vien";
    }

    @PostMapping("/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    public String toggleActive(@RequestParam("id") Long id,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        Employee employee = employeeRepository.findById(id).orElse(null);

        if (employee == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy nhân viên.");
            return "redirect:/nhan-vien";
        }

        // Không cho ADMIN tự khóa chính tài khoản đang đăng nhập.
        if (employee.isActive()
                && authentication != null
                && employee.getUsername() != null
                && employee.getUsername().equalsIgnoreCase(authentication.getName())) {
            redirectAttributes.addFlashAttribute("error", "Không thể khóa tài khoản đang đăng nhập.");
            return "redirect:/nhan-vien";
        }

        // Luôn giữ lại ít nhất một ADMIN đang hoạt động để hệ thống không bị mất tài khoản quản trị.
        if (employee.isActive() && "ADMIN".equals(employee.getRole())) {
            long activeAdmins = employeeRepository.findAll().stream()
                    .filter(Employee::isActive)
                    .filter(e -> "ADMIN".equals(e.getRole()))
                    .count();
            if (activeAdmins <= 1) {
                redirectAttributes.addFlashAttribute("error", "Không thể khóa ADMIN cuối cùng đang hoạt động.");
                return "redirect:/nhan-vien";
            }
        }

        employee.setActive(!employee.isActive());
        employeeRepository.save(employee);

        redirectAttributes.addFlashAttribute(
                "success",
                employee.isActive()
                        ? "Đã mở lại tài khoản nhân viên."
                        : "Đã khóa tài khoản nhân viên."
        );

        return "redirect:/nhan-vien";
    }

    private String fail(RedirectAttributes attributes, Long editId, String message) {
        attributes.addFlashAttribute("error", message);
        if (editId != null) {
            attributes.addAttribute("editId", editId);
        }
        return "redirect:/nhan-vien";
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase().contains(keyword.toLowerCase());
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private String trimToNull(String value) {
        String result = trim(value);
        return result.isBlank() ? null : result;
    }
}
