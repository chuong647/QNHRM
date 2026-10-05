package com.chamcong.controller;

import com.chamcong.entity.Attendance;
import com.chamcong.entity.Employee;
import com.chamcong.entity.SalaryAdvance;
import com.chamcong.repository.AttendanceRepository;
import com.chamcong.repository.EmployeeRepository;
import com.chamcong.repository.SalaryAdvanceRepository;
import com.chamcong.service.AttendanceService;
import com.chamcong.service.PayrollService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.util.CellRangeAddress;
import java.io.ByteArrayOutputStream;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Controller
public class ReportController {

    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final EmployeeRepository employeeRepository;
    private final AttendanceRepository attendanceRepository;
    private final SalaryAdvanceRepository salaryAdvanceRepository;
    private final AttendanceService attendanceService;
    private final PayrollService payrollService;

    public ReportController(
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

    @GetMapping("/bao-cao")
    @PreAuthorize("hasRole('ADMIN')")
    public String report(
            @RequestParam(required = false) String month,
            Model model,
            Authentication authentication
    ) {
        YearMonth yearMonth = parseMonth(month);
        LocalDate from = yearMonth.atDay(1);
        LocalDate to = yearMonth.atEndOfMonth();

        List<Employee> employees = employeeRepository.findByActiveTrueOrderByFullNameAsc();
        List<ReportRow> rows = new ArrayList<>();

        BigDecimal totalGross = BigDecimal.ZERO;
        BigDecimal totalAdvance = BigDecimal.ZERO;
        BigDecimal totalNet = BigDecimal.ZERO;
        double totalHours = 0.0;
        int totalDays = 0;

        for (Employee employee : employees) {
            List<Attendance> attendances = attendanceRepository
                    .findByEmployeeIdAndWorkDateBetweenOrderByWorkDateAsc(employee.getId(), from, to);

            List<SalaryAdvance> advances = salaryAdvanceRepository
                    .findByEmployeeIdAndAdvanceDateBetweenOrderByAdvanceDateAsc(employee.getId(), from, to);

            double hours = attendances.stream()
                    .mapToDouble(attendanceService::calculateTotalHours)
                    .sum();

            BigDecimal rate = employee.getHourlyRate() == null ? BigDecimal.ZERO : employee.getHourlyRate();
            BigDecimal gross = payrollService.calculateGrossSalary(hours, rate);
            BigDecimal advance = payrollService.calculateTotalAdvance(advances);
            BigDecimal unpaid = payrollService.calculateUnpaidAdvance(advances);
            BigDecimal net = payrollService.calculateNetSalary(gross, unpaid);

            rows.add(new ReportRow(
                    employee.getId(),
                    employee.getFullName(),
                    employee.getPosition(),
                    hours,
                    attendances.size(),
                    rate,
                    gross,
                    advance,
                    unpaid,
                    net
            ));

            totalHours += hours;
            totalDays += attendances.size();
            totalGross = totalGross.add(gross);
            totalAdvance = totalAdvance.add(advance);
            totalNet = totalNet.add(net);
        }

        model.addAttribute("currentUser", authentication.getName());
        model.addAttribute("month", yearMonth.format(MONTH_FORMAT));
        model.addAttribute("monthLabel", String.format("Tháng %02d/%d", yearMonth.getMonthValue(), yearMonth.getYear()));
        model.addAttribute("rows", rows);
        model.addAttribute("totalHours", roundHours(totalHours));
        model.addAttribute("totalDays", totalDays);
        model.addAttribute("totalGross", money(totalGross));
        model.addAttribute("totalAdvance", money(totalAdvance));
        model.addAttribute("totalNet", money(totalNet));
        model.addAttribute("employeeCount", employees.size());

        return "report";
    }

    @GetMapping("/bao-cao/xuat-excel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportExcel(@RequestParam(required = false) String month) throws Exception {
        YearMonth ym = parseMonth(month);
        LocalDate from = ym.atDay(1);
        LocalDate to = ym.atEndOfMonth();
        List<Employee> employees = employeeRepository.findByActiveTrueOrderByFullNameAsc();

        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Bao cao luong");
            Font titleFont = workbook.createFont(); titleFont.setBold(true); titleFont.setFontHeightInPoints((short)16);
            Font headerFont = workbook.createFont(); headerFont.setBold(true);
            CellStyle title = workbook.createCellStyle(); title.setFont(titleFont); title.setAlignment(HorizontalAlignment.CENTER); title.setVerticalAlignment(VerticalAlignment.CENTER);
            CellStyle header = workbook.createCellStyle(); header.setFont(headerFont); header.setAlignment(HorizontalAlignment.CENTER); header.setVerticalAlignment(VerticalAlignment.CENTER); header.setFillForegroundColor((short) 22); header.setFillPattern(FillPatternType.SOLID_FOREGROUND); header.setBorderTop(BorderStyle.THIN); header.setBorderBottom(BorderStyle.THIN); header.setBorderLeft(BorderStyle.THIN); header.setBorderRight(BorderStyle.THIN);
            CellStyle cell = workbook.createCellStyle(); cell.setBorderTop(BorderStyle.THIN); cell.setBorderBottom(BorderStyle.THIN); cell.setBorderLeft(BorderStyle.THIN); cell.setBorderRight(BorderStyle.THIN);
            CellStyle money = workbook.createCellStyle(); money.cloneStyleFrom(cell); money.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));
            CellStyle num = workbook.createCellStyle(); num.cloneStyleFrom(cell); num.setDataFormat(workbook.createDataFormat().getFormat("0.00"));

            Row tr = sheet.createRow(0); tr.setHeightInPoints(28); Cell tc = tr.createCell(0); tc.setCellValue("BÁO CÁO LƯƠNG THÁNG " + String.format("%02d/%d", ym.getMonthValue(), ym.getYear())); tc.setCellStyle(title); sheet.addMergedRegion(new CellRangeAddress(0,0,0,9));
            String[] h={"STT","Nhân viên","Chức vụ","Ngày công","Tổng giờ","Lương/giờ","Lương gộp","Tổng ứng","Chưa trừ","Thực lĩnh"};
            Row hr=sheet.createRow(2); for(int i=0;i<h.length;i++){Cell c=hr.createCell(i);c.setCellValue(h[i]);c.setCellStyle(header);} 
            double totalHours=0; int totalDays=0; BigDecimal tg=BigDecimal.ZERO, ta=BigDecimal.ZERO, tu=BigDecimal.ZERO, tn=BigDecimal.ZERO;
            int r=3, stt=1;
            for(Employee e:employees){
                List<Attendance> as=attendanceRepository.findByEmployeeIdAndWorkDateBetweenOrderByWorkDateAsc(e.getId(),from,to);
                List<SalaryAdvance> av=salaryAdvanceRepository.findByEmployeeIdAndAdvanceDateBetweenOrderByAdvanceDateAsc(e.getId(),from,to);
                double hours=as.stream().mapToDouble(attendanceService::calculateTotalHours).sum();
                BigDecimal rate=e.getHourlyRate()==null?BigDecimal.ZERO:e.getHourlyRate();
                BigDecimal gross=payrollService.calculateGrossSalary(hours,rate); BigDecimal adv=payrollService.calculateTotalAdvance(av); BigDecimal unpaid=payrollService.calculateUnpaidAdvance(av); BigDecimal net=payrollService.calculateNetSalary(gross,unpaid);
                Row row=sheet.createRow(r++); Object[] vals={stt++, e.getFullName(), e.getPosition()==null?"":e.getPosition(), as.size(), hours, rate.doubleValue(), gross.doubleValue(), adv.doubleValue(), unpaid.doubleValue(), net.doubleValue()};
                for(int i=0;i<vals.length;i++){Cell c=row.createCell(i); if(vals[i] instanceof Number n){c.setCellValue(n.doubleValue()); c.setCellStyle((i==4)?num:(i>=5?money:cell));} else {c.setCellValue(String.valueOf(vals[i])); c.setCellStyle(cell);}}
                totalHours+=hours; totalDays+=as.size(); tg=tg.add(gross); ta=ta.add(adv); tu=tu.add(unpaid); tn=tn.add(net);
            }
            Row sr=sheet.createRow(r+1); sr.createCell(0).setCellValue("TỔNG CỘNG"); sr.getCell(0).setCellStyle(header); sr.createCell(4).setCellValue(totalDays); sr.getCell(4).setCellStyle(header); sr.createCell(5).setCellValue(totalHours); sr.getCell(5).setCellStyle(num); sr.createCell(6).setCellValue(tg.doubleValue()); sr.getCell(6).setCellStyle(money); sr.createCell(7).setCellValue(ta.doubleValue()); sr.getCell(7).setCellStyle(money); sr.createCell(8).setCellValue(tu.doubleValue()); sr.getCell(8).setCellStyle(money); sr.createCell(9).setCellValue(tn.doubleValue()); sr.getCell(9).setCellStyle(money);
            int[] widths={7,28,20,12,14,16,18,16,16,18}; for(int i=0;i<widths.length;i++)sheet.setColumnWidth(i,widths[i]*256); sheet.createFreezePane(0,3); sheet.setAutoFilter(new CellRangeAddress(2,Math.max(2,r-1),0,h.length-1));
            workbook.write(out);
            String filename="Bao_cao_luong_"+ym+".xlsx";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(out.toByteArray());
        }
    }

    private YearMonth parseMonth(String month) {
        if (month == null || month.isBlank()) {
            return YearMonth.now();
        }
        try {
            return YearMonth.parse(month, MONTH_FORMAT);
        } catch (Exception e) {
            return YearMonth.now();
        }
    }

    private double roundHours(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    public record ReportRow(
            Long employeeId,
            String fullName,
            String position,
            double totalHours,
            int attendanceDays,
            BigDecimal hourlyRate,
            BigDecimal grossSalary,
            BigDecimal totalAdvance,
            BigDecimal unpaidAdvance,
            BigDecimal netSalary
    ) {}
}
