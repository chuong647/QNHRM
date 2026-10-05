package com.chamcong.controller;

import com.chamcong.entity.Attendance;
import com.chamcong.entity.Employee;
import com.chamcong.entity.SalaryAdvance;
import com.chamcong.repository.AttendanceRepository;
import com.chamcong.repository.EmployeeRepository;
import com.chamcong.repository.SalaryAdvanceRepository;
import com.chamcong.service.AttendanceService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

@Controller
@RequestMapping("/cham-cong")
public class AttendanceController {

    private final EmployeeRepository employeeRepository;
    private final AttendanceRepository attendanceRepository;
    private final SalaryAdvanceRepository salaryAdvanceRepository;
    private final AttendanceService attendanceService;

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm");

    public AttendanceController(
            EmployeeRepository employeeRepository,
            AttendanceRepository attendanceRepository,
            SalaryAdvanceRepository salaryAdvanceRepository,
            AttendanceService attendanceService
    ) {
        this.employeeRepository = employeeRepository;
        this.attendanceRepository = attendanceRepository;
        this.salaryAdvanceRepository = salaryAdvanceRepository;
        this.attendanceService = attendanceService;
    }

    @GetMapping
    public String index(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) String month,
            @RequestParam(required = false) Long editId,
            @RequestParam(required = false, defaultValue = "false") boolean add,
            Model model,
            Authentication authentication
    ) {
        Employee currentUser = employeeRepository
                .findByUsername(authentication.getName())
                .orElseThrow();

        boolean isAdmin = "ADMIN".equals(currentUser.getRole());

        List<Employee> employees;
        if (isAdmin) {
            employees = employeeRepository.findByActiveTrueOrderByFullNameAsc();
        } else {
            employees = List.of(currentUser);
            employeeId = currentUser.getId();
        }

        String selectedMonth = (month == null || month.isBlank())
                ? YearMonth.now().toString()
                : month;

        model.addAttribute("employees", employees);
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("selectedMonth", selectedMonth);
        model.addAttribute("records", List.of());
        model.addAttribute("totalHours", 0.0);
        model.addAttribute("totalDays", 0);
        model.addAttribute("totalDossiers", "");
        model.addAttribute("hourlyRate", BigDecimal.ZERO);
        model.addAttribute("grossSalary", BigDecimal.ZERO.setScale(2));
        model.addAttribute("advanceDeduction", BigDecimal.ZERO.setScale(2));
        model.addAttribute("netSalary", BigDecimal.ZERO.setScale(2));
        model.addAttribute("selectedEmployee", null);
        model.addAttribute("employeeId", employeeId);
        model.addAttribute("attendanceService", attendanceService);
        model.addAttribute("openAddForm", add && isAdmin);

        // Khu vực tự chấm công cho STAFF
        if (!isAdmin) {
            LocalDate today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
            LocalTime now = LocalTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
            Attendance todayAttendance = attendanceRepository
                    .findByEmployeeIdAndWorkDate(currentUser.getId(), today)
                    .orElse(null);
            model.addAttribute("today", today);
            model.addAttribute("nowTime", now);
            model.addAttribute("todayAttendance", todayAttendance);
        }

        if (employeeId == null) {
            return "attendance";
        }

        YearMonth yearMonth;
        try {
            yearMonth = YearMonth.parse(selectedMonth);
        } catch (Exception e) {
            model.addAttribute("error", "Tháng không hợp lệ. Vui lòng chọn lại tháng.");
            return "attendance";
        }

        Employee selected = employeeRepository.findById(employeeId).orElse(null);

        if (selected == null || !selected.isActive()) {
            model.addAttribute("error", "Không tìm thấy nhân viên.");
            return "attendance";
        }

        if (!isAdmin && !Objects.equals(selected.getId(), currentUser.getId())) {
            model.addAttribute("error", "Bạn không có quyền xem chấm công của nhân viên khác.");
            model.addAttribute("selectedEmployee", currentUser);
            return "attendance";
        }

        LocalDate from = yearMonth.atDay(1);
        LocalDate to = yearMonth.atEndOfMonth();

        List<Attendance> records = attendanceRepository
                .findByEmployeeIdAndWorkDateBetweenOrderByWorkDateAsc(
                        employeeId,
                        from,
                        to
                );

        double totalHours = records.stream()
                .mapToDouble(attendanceService::calculateTotalHours)
                .sum();

        BigDecimal hourlyRate = selected.getHourlyRate() == null
                ? BigDecimal.ZERO
                : selected.getHourlyRate();

        BigDecimal grossSalary = hourlyRate
                .multiply(BigDecimal.valueOf(totalHours))
                .setScale(2, RoundingMode.HALF_UP);

        List<SalaryAdvance> advances = salaryAdvanceRepository
                .findByEmployeeIdAndAdvanceDateBetweenOrderByAdvanceDateAsc(
                        employeeId,
                        from,
                        to
                );

        BigDecimal advanceDeduction = advances.stream()
                .filter(x -> "CHUA_TRU".equals(x.getStatus()))
                .map(SalaryAdvance::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal netSalary = grossSalary
                .subtract(advanceDeduction)
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);

        String dossierSummary = records.stream()
                .map(Attendance::getDossierCount)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .reduce((a, b) -> a + ", " + b)
                .orElse("");

        model.addAttribute("records", records);
        model.addAttribute("totalHours", Math.round(totalHours * 100.0) / 100.0);
        model.addAttribute("totalDays", records.size());
        model.addAttribute("totalDossiers", dossierSummary);
        model.addAttribute("selectedEmployee", selected);
        model.addAttribute("hourlyRate", hourlyRate);
        model.addAttribute("grossSalary", grossSalary);
        model.addAttribute("advanceDeduction", advanceDeduction);
        model.addAttribute("netSalary", netSalary);
        model.addAttribute("employeeId", employeeId);

        if (editId != null && isAdmin) {
            Attendance editAttendance = attendanceRepository
                    .findWithEmployeeById(editId)
                    .orElse(null);

            if (editAttendance != null) {
                model.addAttribute("editAttendance", editAttendance);
            }
        }

        return "attendance";
    }

    @PostMapping("/luu")
    @PreAuthorize("hasRole('ADMIN')")
    public String save(
            @RequestParam(required = false) Long id,
            @RequestParam Long employeeId,
            @RequestParam String workDate,
            @RequestParam(required = false) String dossierCount,
            @RequestParam(required = false) String morningIn,
            @RequestParam(required = false) String morningOut,
            @RequestParam(required = false) String afternoonIn,
            @RequestParam(required = false) String afternoonOut,
            @RequestParam(required = false) String eveningIn,
            @RequestParam(required = false) String eveningOut,
            @RequestParam(required = false, defaultValue = "0") Integer lateMinutes,
            @RequestParam(required = false, defaultValue = "0") Integer earlyLeaveMinutes,
            @RequestParam(required = false) String note
    ) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhân viên."));

        LocalDate date = LocalDate.parse(workDate);

        Attendance attendance;
        if (id != null) {
            attendance = attendanceRepository.findWithEmployeeById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bản ghi chấm công."));
            if (attendance.getEmployee() != null && !employeeId.equals(attendance.getEmployee().getId())) {
                throw new IllegalArgumentException("Bản ghi chấm công không thuộc nhân viên được chọn.");
            }
        } else {
            attendance = attendanceRepository
                    .findByEmployeeIdAndWorkDate(employeeId, date)
                    .orElseGet(Attendance::new);
        }

        Attendance duplicate = attendanceRepository
                .findByEmployeeIdAndWorkDate(employeeId, date)
                .orElse(null);
        if (duplicate != null && (attendance.getId() == null || !duplicate.getId().equals(attendance.getId()))) {
            throw new IllegalArgumentException("Nhân viên đã có bản ghi chấm công cho ngày " + date + ".");
        }

        attendance.setEmployee(employee);
        attendance.setWorkDate(date);
        attendance.setDossierCount(dossierCount);
        attendance.setMorningIn(parseTime(morningIn, "Vào sáng"));
        attendance.setMorningOut(parseTime(morningOut, "Ra sáng"));
        attendance.setAfternoonIn(parseTime(afternoonIn, "Vào chiều"));
        attendance.setAfternoonOut(parseTime(afternoonOut, "Ra chiều"));
        attendance.setEveningIn(parseTime(eveningIn, "Vào tối"));
        attendance.setEveningOut(parseTime(eveningOut, "Ra tối"));
        int safeLateMinutes = lateMinutes == null ? 0 : lateMinutes;
        int safeEarlyLeaveMinutes = earlyLeaveMinutes == null ? 0 : earlyLeaveMinutes;
        if (safeLateMinutes < 0 || safeEarlyLeaveMinutes < 0) {
            throw new IllegalArgumentException("Đi muộn và về sớm phải là số phút không âm.");
        }
        String normalizedNote = note == null || note.isBlank() ? null : note.trim();
        if (normalizedNote != null && normalizedNote.length() > 500) {
            throw new IllegalArgumentException("Ghi chú không được vượt quá 500 ký tự.");
        }
        attendance.setLateMinutes(safeLateMinutes);
        attendance.setEarlyLeaveMinutes(safeEarlyLeaveMinutes);
        attendance.setNote(normalizedNote);

        validatePair(attendance.getMorningIn(), attendance.getMorningOut(), "sáng");
        validatePair(attendance.getAfternoonIn(), attendance.getAfternoonOut(), "chiều");
        validatePairAllowOvernight(attendance.getEveningIn(), attendance.getEveningOut(), "tối");

        if (attendance.getDossierCount() != null && attendance.getDossierCount().length() > 1000) {
            throw new IllegalArgumentException("Hồ sơ không được vượt quá 1000 ký tự.");
        }

        attendanceRepository.save(attendance);

        return "redirect:/cham-cong?employeeId=" + employeeId
                + "&month=" + date.toString().substring(0, 7);
    }

    /**
     * Xuất bảng chấm công tháng hiện tại đang chọn của một nhân viên ra Excel.
     * Chỉ ADMIN được phép sử dụng.
     */
    @GetMapping("/xuat-excel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam Long employeeId,
            @RequestParam String month
    ) throws IOException {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhân viên."));

        if (!employee.isActive()) {
            throw new IllegalArgumentException("Tài khoản nhân viên đang bị khóa.");
        }

        YearMonth yearMonth;
        try {
            yearMonth = YearMonth.parse(month);
        } catch (Exception e) {
            throw new IllegalArgumentException("Tháng xuất bảng không hợp lệ: " + month);
        }

        LocalDate from = yearMonth.atDay(1);
        LocalDate to = yearMonth.atEndOfMonth();

        List<Attendance> records = attendanceRepository
                .findByEmployeeIdAndWorkDateBetweenOrderByWorkDateAsc(
                        employeeId, from, to);

        double totalHours = records.stream()
                .mapToDouble(attendanceService::calculateTotalHours)
                .sum();

        String dossierSummary = records.stream()
                .map(Attendance::getDossierCount)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .reduce((a, b) -> a + ", " + b)
                .orElse("");

        BigDecimal hourlyRate = employee.getHourlyRate() == null
                ? BigDecimal.ZERO
                : employee.getHourlyRate();

        BigDecimal grossSalary = hourlyRate
                .multiply(BigDecimal.valueOf(totalHours))
                .setScale(2, RoundingMode.HALF_UP);

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            var sheet = workbook.createSheet("Bang cham cong");
            sheet.setDefaultRowHeightInPoints(20);

            // =========================
            // STYLES
            // =========================
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 16);

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);

            CellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setFont(titleFont);
            titleStyle.setAlignment(HorizontalAlignment.CENTER);
            titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            CellStyle labelStyle = workbook.createCellStyle();
            labelStyle.setFont(headerFont);

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerStyle.setFillForegroundColor((short) 22);
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            CellStyle centerStyle = workbook.createCellStyle();
            centerStyle.setAlignment(HorizontalAlignment.CENTER);
            centerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            centerStyle.setBorderTop(BorderStyle.THIN);
            centerStyle.setBorderBottom(BorderStyle.THIN);
            centerStyle.setBorderLeft(BorderStyle.THIN);
            centerStyle.setBorderRight(BorderStyle.THIN);

            CellStyle leftStyle = workbook.createCellStyle();
            leftStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            leftStyle.setBorderTop(BorderStyle.THIN);
            leftStyle.setBorderBottom(BorderStyle.THIN);
            leftStyle.setBorderLeft(BorderStyle.THIN);
            leftStyle.setBorderRight(BorderStyle.THIN);

            CellStyle numberStyle = workbook.createCellStyle();
            numberStyle.setAlignment(HorizontalAlignment.CENTER);
            numberStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            numberStyle.setDataFormat(workbook.createDataFormat().getFormat("0.00"));
            numberStyle.setBorderTop(BorderStyle.THIN);
            numberStyle.setBorderBottom(BorderStyle.THIN);
            numberStyle.setBorderLeft(BorderStyle.THIN);
            numberStyle.setBorderRight(BorderStyle.THIN);

            // =========================
            // TITLE
            // =========================
            var titleRow = sheet.createRow(0);
            titleRow.setHeightInPoints(28);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("BẢNG CHẤM CÔNG THÁNG " + yearMonth.getMonthValue() + "/" + yearMonth.getYear());
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 13));

            var info1 = sheet.createRow(1);
            info1.createCell(0).setCellValue("Họ và tên");
            info1.getCell(0).setCellStyle(labelStyle);
            info1.createCell(1).setCellValue(employee.getFullName());
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(1, 1, 1, 5));

            var info2 = sheet.createRow(2);
            info2.createCell(0).setCellValue("Username");
            info2.getCell(0).setCellStyle(labelStyle);
            info2.createCell(1).setCellValue(employee.getUsername());
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(2, 2, 1, 5));

            var info4 = sheet.createRow(3);
            info4.createCell(0).setCellValue("Lương/giờ");
            info4.getCell(0).setCellStyle(labelStyle);
            info4.createCell(1).setCellValue(hourlyRate.doubleValue());
            info4.getCell(1).setCellStyle(numberStyle);
            info4.createCell(2).setCellValue("Tổng giờ");
            info4.getCell(2).setCellStyle(labelStyle);
            info4.createCell(3).setCellValue(totalHours);
            info4.getCell(3).setCellStyle(numberStyle);
            info4.createCell(4).setCellValue("Lương gộp");
            info4.getCell(4).setCellStyle(labelStyle);
            info4.createCell(5).setCellValue(grossSalary.doubleValue());
            info4.getCell(5).setCellStyle(numberStyle);

            // =========================
            // TABLE
            // =========================
            String[] headers = {
                    "STT", "Ngày", "Họ và tên", "Vào sáng", "Ra sáng",
                    "Vào chiều", "Ra chiều", "Vào tối", "Ra tối", "Tổng giờ",
                    "Hồ sơ", "Đi muộn (phút)", "Về sớm (phút)", "Ghi chú"
            };

            int headerRowIndex = 5;
            var headerRow = sheet.createRow(headerRowIndex);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            headerRow.setHeightInPoints(34);

            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            int rowIndex = headerRowIndex + 1;

            for (int i = 0; i < records.size(); i++) {
                Attendance record = records.get(i);
                var row = sheet.createRow(rowIndex++);

                setCell(row, 0, String.valueOf(i + 1), centerStyle);
                setCell(row, 1, record.getWorkDate() == null ? "" : record.getWorkDate().format(dateFormatter), centerStyle);
                setCell(row, 2, employee.getFullName(), leftStyle);
                setCell(row, 3, formatTime(record.getMorningIn()), centerStyle);
                setCell(row, 4, formatTime(record.getMorningOut()), centerStyle);
                setCell(row, 5, formatTime(record.getAfternoonIn()), centerStyle);
                setCell(row, 6, formatTime(record.getAfternoonOut()), centerStyle);
                setCell(row, 7, formatTime(record.getEveningIn()), centerStyle);
                setCell(row, 8, formatTime(record.getEveningOut()), centerStyle);

                Cell hoursCell = row.createCell(9);
                hoursCell.setCellValue(attendanceService.calculateTotalHours(record));
                hoursCell.setCellStyle(numberStyle);

                setCell(row, 10, record.getDossierCount() == null ? "" : record.getDossierCount(), leftStyle);
                setCell(row, 11, String.valueOf(record.getLateMinutes() == null ? 0 : record.getLateMinutes()), centerStyle);
                setCell(row, 12, String.valueOf(record.getEarlyLeaveMinutes() == null ? 0 : record.getEarlyLeaveMinutes()), centerStyle);
                setCell(row, 13, record.getNote() == null ? "" : record.getNote(), leftStyle);
            }

            // Summary footer
            int summaryRowIndex = rowIndex + 1;
            var summaryRow = sheet.createRow(summaryRowIndex);
            summaryRow.createCell(0).setCellValue("Tổng cộng");
            summaryRow.getCell(0).setCellStyle(labelStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(summaryRowIndex, summaryRowIndex, 0, 2));

            summaryRow.createCell(3).setCellValue("Tổng giờ");
            summaryRow.getCell(3).setCellStyle(labelStyle);
            summaryRow.createCell(4).setCellValue(totalHours);
            summaryRow.getCell(4).setCellStyle(numberStyle);

            summaryRow.createCell(5).setCellValue("Số ngày");
            summaryRow.getCell(5).setCellStyle(labelStyle);
            summaryRow.createCell(6).setCellValue(records.size());
            summaryRow.getCell(6).setCellStyle(centerStyle);

            summaryRow.createCell(7).setCellValue("Hồ sơ");
            summaryRow.getCell(7).setCellStyle(labelStyle);
            summaryRow.createCell(8).setCellValue(dossierSummary);
            summaryRow.getCell(8).setCellStyle(leftStyle);

            // =========================
            // WIDTH
            // =========================
            int[] widths = {7, 14, 24, 12, 12, 12, 12, 12, 12, 12, 30, 17, 17, 30};
            for (int i = 0; i < widths.length; i++) {
                sheet.setColumnWidth(i, widths[i] * 256);
            }

            sheet.createFreezePane(0, headerRowIndex + 1);
            sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(
                    headerRowIndex,
                    Math.max(headerRowIndex, rowIndex - 1),
                    0,
                    headers.length - 1
            ));

            workbook.write(output);

            String safeUsername = employee.getUsername() == null
                    ? "nhanvien"
                    : employee.getUsername().replaceAll("[^a-zA-Z0-9_-]", "_");
            String filename = "Bang_cham_cong_" + safeUsername + "_" + yearMonth + ".xlsx";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(output.toByteArray());
        }
    }

    private static void setCell(
            org.apache.poi.ss.usermodel.Row row,
            int column,
            String value,
            CellStyle style
    ) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value == null ? "" : value);
        cell.setCellStyle(style);
    }

    private static String formatTime(LocalTime time) {
        return time == null ? "" : time.format(TIME_FORMAT);
    }

    /**
     * Nhân viên tự chấm công cho chính tài khoản đang đăng nhập.
     * Các action hợp lệ: VAO_SANG, RA_SANG, VAO_CHIEU, RA_CHIEU, VAO_TOI, RA_TOI.
     */
    @PostMapping("/tu-cham")
    @PreAuthorize("hasRole('STAFF')")
    public String selfPunch(
            @RequestParam String action,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        Employee currentUser = employeeRepository
                .findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản."));

        if (!currentUser.isActive()) {
            return "redirect:/login?error";
        }

        ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");
        LocalDate today = LocalDate.now(zone);
        LocalTime now = LocalTime.now(zone).withNano(0);

        Attendance attendance = attendanceRepository
                .findByEmployeeIdAndWorkDate(currentUser.getId(), today)
                .orElseGet(() -> {
                    Attendance a = new Attendance();
                    a.setEmployee(currentUser);
                    a.setWorkDate(today);
                    return a;
                });

        String normalized = action == null ? "" : action.trim().toUpperCase();

        switch (normalized) {
            case "VAO_SANG" -> {
                if (attendance.getMorningIn() != null) {
                    return selfPunchError(redirectAttributes, "Bạn đã chấm Vào sáng hôm nay.");
                }
                attendance.setMorningIn(now);
            }
            case "RA_SANG" -> {
                if (attendance.getMorningIn() == null) {
                    return selfPunchError(redirectAttributes, "Chưa có giờ Vào sáng, không thể chấm Ra sáng.");
                }
                if (attendance.getMorningOut() != null) {
                    return selfPunchError(redirectAttributes, "Bạn đã chấm Ra sáng hôm nay.");
                }
                attendance.setMorningOut(now);
            }
            case "VAO_CHIEU" -> {
                if (attendance.getAfternoonIn() != null) {
                    return selfPunchError(redirectAttributes, "Bạn đã chấm Vào chiều hôm nay.");
                }
                attendance.setAfternoonIn(now);
            }
            case "RA_CHIEU" -> {
                if (attendance.getAfternoonIn() == null) {
                    return selfPunchError(redirectAttributes, "Chưa có giờ Vào chiều, không thể chấm Ra chiều.");
                }
                if (attendance.getAfternoonOut() != null) {
                    return selfPunchError(redirectAttributes, "Bạn đã chấm Ra chiều hôm nay.");
                }
                attendance.setAfternoonOut(now);
            }
            case "VAO_TOI" -> {
                if (attendance.getEveningIn() != null) {
                    return selfPunchError(redirectAttributes, "Bạn đã chấm Vào tối hôm nay.");
                }
                attendance.setEveningIn(now);
            }
            case "RA_TOI" -> {
                // Bình thường: ra tối trong cùng ngày.
                if (attendance.getEveningIn() != null) {
                    if (attendance.getEveningOut() != null) {
                        return selfPunchError(redirectAttributes, "Bạn đã chấm Ra tối hôm nay.");
                    }
                    attendance.setEveningOut(now);
                    break;
                }

                // Hỗ trợ ca tối qua 00:00: chấm ra cho ca bắt đầu từ ngày hôm trước.
                if (now.isBefore(LocalTime.of(6, 0))) {
                    LocalDate yesterday = today.minusDays(1);
                    Attendance yesterdayAttendance = attendanceRepository
                            .findByEmployeeIdAndWorkDate(currentUser.getId(), yesterday)
                            .orElse(null);

                    if (yesterdayAttendance != null && yesterdayAttendance.getEveningIn() != null
                            && yesterdayAttendance.getEveningOut() == null) {
                        yesterdayAttendance.setEveningOut(now);
                        attendanceRepository.save(yesterdayAttendance);
                        redirectAttributes.addFlashAttribute(
                                "success",
                                "Đã ghi Ra tối lúc " + now.format(TIME_FORMAT)
                                        + " cho ca ngày " + yesterday + "."
                        );
                        return "redirect:/cham-cong?month=" + yesterday.toString().substring(0, 7);
                    }
                }

                return selfPunchError(redirectAttributes,
                        "Chưa có giờ Vào tối phù hợp để ghi Ra tối.");
            }
            default -> {
                return selfPunchError(redirectAttributes, "Thao tác chấm công không hợp lệ.");
            }
        }

        attendanceRepository.save(attendance);

        String label = switch (normalized) {
            case "VAO_SANG" -> "Vào sáng";
            case "RA_SANG" -> "Ra sáng";
            case "VAO_CHIEU" -> "Vào chiều";
            case "RA_CHIEU" -> "Ra chiều";
            case "VAO_TOI" -> "Vào tối";
            case "RA_TOI" -> "Ra tối";
            default -> "Chấm công";
        };

        redirectAttributes.addFlashAttribute(
                "success",
                "Đã ghi " + label + " lúc " + now.format(TIME_FORMAT) + "."
        );

        return "redirect:/cham-cong?month=" + today.toString().substring(0, 7);
    }

    private String selfPunchError(RedirectAttributes redirectAttributes, String message) {
        redirectAttributes.addFlashAttribute("error", message);
        return "redirect:/cham-cong";
    }

    /**
     * STAFF nhập chấm công bằng biểu mẫu giống Admin, nhưng backend chỉ cho phép
     * ghi dữ liệu cho chính tài khoản đang đăng nhập và chỉ cho ngày hiện tại.
     */
    @PostMapping("/tu-nhap")
    @PreAuthorize("hasRole('STAFF')")
    public String selfSave(
            Authentication authentication,
            @RequestParam String workDate,
            @RequestParam(required = false) String dossierCount,
            @RequestParam(required = false) String morningIn,
            @RequestParam(required = false) String morningOut,
            @RequestParam(required = false) String afternoonIn,
            @RequestParam(required = false) String afternoonOut,
            @RequestParam(required = false) String eveningIn,
            @RequestParam(required = false) String eveningOut,
            @RequestParam(required = false, defaultValue = "0") Integer lateMinutes,
            @RequestParam(required = false, defaultValue = "0") Integer earlyLeaveMinutes,
            @RequestParam(required = false) String note,
            RedirectAttributes redirectAttributes
    ) {
        Employee currentUser = employeeRepository
                .findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản."));

        if (!currentUser.isActive()) {
            return "redirect:/login?error";
        }

        ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");
        LocalDate today = LocalDate.now(zone);
        LocalDate date;

        try {
            date = LocalDate.parse(workDate);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ngày chấm công không hợp lệ.");
            return "redirect:/cham-cong";
        }

        if (!today.equals(date)) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Nhân viên chỉ được nhập chấm công cho ngày hiện tại. Dữ liệu ngày khác do Admin cập nhật."
            );
            return "redirect:/cham-cong?month=" + date.toString().substring(0, 7);
        }

        try {
            Attendance attendance = attendanceRepository
                    .findByEmployeeIdAndWorkDate(currentUser.getId(), date)
                    .orElseGet(() -> {
                        Attendance a = new Attendance();
                        a.setEmployee(currentUser);
                        a.setWorkDate(date);
                        return a;
                    });

            attendance.setEmployee(currentUser);
            attendance.setWorkDate(date);
            attendance.setDossierCount(dossierCount);
            attendance.setMorningIn(parseTime(morningIn, "Vào sáng"));
            attendance.setMorningOut(parseTime(morningOut, "Ra sáng"));
            attendance.setAfternoonIn(parseTime(afternoonIn, "Vào chiều"));
            attendance.setAfternoonOut(parseTime(afternoonOut, "Ra chiều"));
            attendance.setEveningIn(parseTime(eveningIn, "Vào tối"));
            attendance.setEveningOut(parseTime(eveningOut, "Ra tối"));
            attendance.setLateMinutes(lateMinutes == null ? 0 : Math.max(0, lateMinutes));
            attendance.setEarlyLeaveMinutes(earlyLeaveMinutes == null ? 0 : Math.max(0, earlyLeaveMinutes));
            attendance.setNote(note == null || note.isBlank() ? null : note.trim());

            if (attendance.getDossierCount() != null && attendance.getDossierCount().length() > 1000) {
                throw new IllegalArgumentException("Hồ sơ không được vượt quá 1000 ký tự.");
            }

            boolean hasAnyTime = attendance.getMorningIn() != null
                    || attendance.getMorningOut() != null
                    || attendance.getAfternoonIn() != null
                    || attendance.getAfternoonOut() != null
                    || attendance.getEveningIn() != null
                    || attendance.getEveningOut() != null;
            boolean hasOtherData = attendance.getDossierCount() != null && !attendance.getDossierCount().isBlank()
                    || attendance.getNote() != null && !attendance.getNote().isBlank()
                    || attendance.getLateMinutes() != null && attendance.getLateMinutes() > 0
                    || attendance.getEarlyLeaveMinutes() != null && attendance.getEarlyLeaveMinutes() > 0;
            if (!hasAnyTime && !hasOtherData) {
                throw new IllegalArgumentException("Vui lòng nhập ít nhất một mốc giờ, hồ sơ hoặc ghi chú.");
            }

            // Kiểm tra cặp giờ vào/ra để tránh dữ liệu thiếu logic.
            validatePair(attendance.getMorningIn(), attendance.getMorningOut(), "sáng");
            validatePair(attendance.getAfternoonIn(), attendance.getAfternoonOut(), "chiều");
            validatePairAllowOvernight(attendance.getEveningIn(), attendance.getEveningOut(), "tối");

            attendanceRepository.save(attendance);

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Đã lưu chấm công ngày " + date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + "."
            );
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/cham-cong?month=" + date.toString().substring(0, 7);
    }

    private void validatePair(LocalTime in, LocalTime out, String shiftName) {
        if (out != null && in == null) {
            throw new IllegalArgumentException(
                    "Ca " + shiftName + ": đã có giờ ra nhưng chưa có giờ vào."
            );
        }
        if (in != null && out != null && out.isBefore(in)) {
            throw new IllegalArgumentException(
                    "Ca " + shiftName + ": giờ ra không được sớm hơn giờ vào."
            );
        }
    }

    private void validatePairAllowOvernight(LocalTime in, LocalTime out, String shiftName) {
        if (out != null && in == null) {
            throw new IllegalArgumentException(
                    "Ca " + shiftName + ": đã có giờ ra nhưng chưa có giờ vào."
            );
        }
    }

    @PostMapping("/xoa")
    @PreAuthorize("hasRole('ADMIN')")
    public String delete(
            @RequestParam Long id,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) String month
    ) {
        attendanceRepository.deleteById(id);

        if (employeeId != null && month != null && !month.isBlank()) {
            return "redirect:/cham-cong?employeeId=" + employeeId
                    + "&month=" + month;
        }

        return "redirect:/cham-cong";
    }

    private LocalTime parseTime(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return LocalTime.parse(value.trim(), TIME_FORMAT);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    fieldName + " không hợp lệ. Vui lòng nhập HH:mm, ví dụ 08:00."
            );
        }
    }
}
