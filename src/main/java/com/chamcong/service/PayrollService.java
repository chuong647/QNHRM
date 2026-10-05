package com.chamcong.service;

import com.chamcong.entity.SalaryAdvance;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class PayrollService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    public BigDecimal calculateGrossSalary(double totalHours, BigDecimal hourlyRate) {
        BigDecimal safeRate = hourlyRate == null ? BigDecimal.ZERO : hourlyRate;
        return safeRate
                .multiply(BigDecimal.valueOf(totalHours))
                .setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateUnpaidAdvance(List<SalaryAdvance> advances) {
        if (advances == null || advances.isEmpty()) {
            return ZERO;
        }

        return advances.stream()
                .filter(a -> "CHUA_TRU".equals(a.getStatus()))
                .map(SalaryAdvance::getAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateTotalAdvance(List<SalaryAdvance> advances) {
        if (advances == null || advances.isEmpty()) {
            return ZERO;
        }

        return advances.stream()
                .map(SalaryAdvance::getAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateNetSalary(BigDecimal grossSalary, BigDecimal unpaidAdvance) {
        BigDecimal gross = grossSalary == null ? BigDecimal.ZERO : grossSalary;
        BigDecimal unpaid = unpaidAdvance == null ? BigDecimal.ZERO : unpaidAdvance;

        return gross.subtract(unpaid)
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
