package com.chamcong.service;

import com.chamcong.entity.SalaryAdvance;
import com.chamcong.repository.SalaryAdvanceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
public class SalaryAdvanceService {

    private final SalaryAdvanceRepository salaryAdvanceRepository;

    public SalaryAdvanceService(SalaryAdvanceRepository salaryAdvanceRepository) {
        this.salaryAdvanceRepository = salaryAdvanceRepository;
    }

    public BigDecimal total(List<SalaryAdvance> advances) {
        return advances.stream()
                .map(SalaryAdvance::getAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal totalUnpaid(List<SalaryAdvance> advances) {
        return advances.stream()
                .filter(a -> "CHUA_TRU".equals(a.getStatus()))
                .map(SalaryAdvance::getAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal totalPaid(List<SalaryAdvance> advances) {
        return advances.stream()
                .filter(a -> "DA_TRU".equals(a.getStatus()))
                .map(SalaryAdvance::getAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public void validate(SalaryAdvance advance) {
        if (advance == null) {
            throw new IllegalArgumentException("Dữ liệu ứng lương không hợp lệ.");
        }
        if (advance.getEmployee() == null) {
            throw new IllegalArgumentException("Vui lòng chọn nhân viên.");
        }
        if (!advance.getEmployee().isActive()) {
            throw new IllegalArgumentException("Không thể tạo ứng lương cho tài khoản đã bị khóa.");
        }
        if (advance.getAdvanceDate() == null) {
            throw new IllegalArgumentException("Vui lòng chọn ngày ứng lương.");
        }
        if (advance.getAmount() == null || advance.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Số tiền ứng lương phải lớn hơn 0.");
        }
        advance.setAmount(advance.getAmount().setScale(2, RoundingMode.HALF_UP));

        if (advance.getReason() != null && advance.getReason().length() > 500) {
            throw new IllegalArgumentException("Lý do không được vượt quá 500 ký tự.");
        }
        if (advance.getNote() != null && advance.getNote().length() > 500) {
            throw new IllegalArgumentException("Ghi chú không được vượt quá 500 ký tự.");
        }
        if (!"CHUA_TRU".equals(advance.getStatus()) && !"DA_TRU".equals(advance.getStatus())) {
            throw new IllegalArgumentException("Trạng thái ứng lương không hợp lệ.");
        }
    }

    @Transactional
    public SalaryAdvance save(SalaryAdvance advance) {
        validate(advance);
        return salaryAdvanceRepository.save(advance);
    }

    @Transactional
    public void deleteById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Không xác định được khoản ứng lương.");
        }
        if (!salaryAdvanceRepository.existsById(id)) {
            throw new IllegalArgumentException("Khoản ứng lương không tồn tại.");
        }
        salaryAdvanceRepository.deleteById(id);
    }
}
