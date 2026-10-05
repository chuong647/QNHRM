package com.chamcong.controller;

import com.chamcong.entity.Employee;
import com.chamcong.repository.EmployeeRepository;
import com.chamcong.service.PasswordService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ChangePasswordController {

    private final EmployeeRepository employeeRepository;
    private final PasswordService passwordService;

    public ChangePasswordController(EmployeeRepository employeeRepository, PasswordService passwordService) {
        this.employeeRepository = employeeRepository;
        this.passwordService = passwordService;
    }

    @GetMapping("/doi-mat-khau")
    public String show(Authentication authentication, Model model) {
        Employee employee = employeeRepository.findByUsername(authentication.getName()).orElse(null);
        if (employee == null || !employee.isActive()) return "redirect:/login";
        model.addAttribute("employee", employee);
        return "change-password";
    }

    @PostMapping("/doi-mat-khau")
    public String change(Authentication authentication,
                         @RequestParam String currentPassword,
                         @RequestParam String newPassword,
                         @RequestParam String confirmPassword,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        Employee employee = employeeRepository.findByUsername(authentication.getName()).orElse(null);
        if (employee == null || !employee.isActive()) return "redirect:/login";
        model.addAttribute("employee", employee);

        if (newPassword.length() < 8) {
            model.addAttribute("error", "Mật khẩu mới phải có ít nhất 8 ký tự.");
            return "change-password";
        }
        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("error", "Mật khẩu mới và xác nhận không giống nhau.");
            return "change-password";
        }
        if (passwordService.matches(newPassword, employee.getPasswordHash())) {
            model.addAttribute("error", "Mật khẩu mới phải khác mật khẩu hiện tại.");
            return "change-password";
        }
        if (!passwordService.changePassword(employee, currentPassword, newPassword)) {
            model.addAttribute("error", "Mật khẩu hiện tại không chính xác.");
            return "change-password";
        }

        redirectAttributes.addFlashAttribute("success", "Đổi mật khẩu thành công.");
        return "redirect:/doi-mat-khau";
    }
}
