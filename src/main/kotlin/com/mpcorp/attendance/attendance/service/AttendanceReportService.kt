package com.mpcorp.attendance.attendance.service

import com.mpcorp.attendance.attendance.entity.AttendanceEvent
import com.mpcorp.attendance.attendance.entity.AttendanceType
import com.mpcorp.attendance.attendance.repository.AttendanceEventRepository
import com.mpcorp.attendance.employee.entity.Employee
import com.mpcorp.attendance.employee.repository.EmployeeRepository
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.ByteArrayOutputStream
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** The reporting window the admin picks: a single day, the week, or the month around a reference date. */
enum class ReportPeriod {
    DAY,
    WEEK,
    MONTH;

    /** Half-open [from, to) instant range covering this period around [date] in [zone]. */
    fun range(date: LocalDate, zone: ZoneId): Pair<Instant, Instant> {
        val fromDate: LocalDate
        val toDate: LocalDate
        when (this) {
            DAY -> {
                fromDate = date
                toDate = date.plusDays(1)
            }
            WEEK -> {
                // ISO week: Monday through Sunday containing [date].
                fromDate = date.minusDays((date.dayOfWeek.value - DayOfWeek.MONDAY.value).toLong())
                toDate = fromDate.plusWeeks(1)
            }
            MONTH -> {
                fromDate = date.withDayOfMonth(1)
                toDate = fromDate.plusMonths(1)
            }
        }
        return fromDate.atStartOfDay(zone).toInstant() to toDate.atStartOfDay(zone).toInstant()
    }
}

/** Built Excel report: the workbook bytes and a suggested download filename. */
data class ReportFile(val bytes: ByteArray, val filename: String)

/**
 * Exports attendance punches to an .xlsx file. Scope is either every employee
 * (empty [employeeCodes]) or the officers matching the given codes, over the
 * day/week/month window around a reference date.
 */
@Service
class AttendanceReportService(
    private val attendanceEventRepository: AttendanceEventRepository,
    private val employeeRepository: EmployeeRepository,
    private val businessZone: ZoneId,
) {

    @Transactional(readOnly = true)
    fun buildExcel(period: ReportPeriod, date: LocalDate, employeeCodes: List<String>): ReportFile {
        val (from, to) = period.range(date, businessZone)

        val events = if (employeeCodes.isEmpty()) {
            attendanceEventRepository.findForReport(from, to)
        } else {
            val ids = employeeRepository
                .findByEmployeeCodeLowerIn(employeeCodes.map { it.lowercase() })
                .mapNotNull { it.id }
            if (ids.isEmpty()) emptyList() else attendanceEventRepository.findForReportByEmployees(ids, from, to)
        }

        val employees = employeeRepository.findAllById(events.map { it.employeeId }.toSet())
            .associateBy { it.id }

        return ReportFile(writeWorkbook(events, employees), buildFilename(period, date))
    }

    private fun writeWorkbook(events: List<AttendanceEvent>, employees: Map<Long?, Employee>): ByteArray {
        XSSFWorkbook().use { wb ->
            val sheet = wb.createSheet("Chấm công")

            val headerStyle = wb.createCellStyle().apply {
                setFont(wb.createFont().apply { bold = true })
                alignment = HorizontalAlignment.CENTER
            }
            val headerRow = sheet.createRow(0)
            HEADERS.forEachIndexed { i, title ->
                headerRow.createCell(i).apply {
                    setCellValue(title)
                    cellStyle = headerStyle
                }
            }

            val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").withZone(businessZone)
            events.forEachIndexed { index, event ->
                val employee = employees[event.employeeId]
                val row = sheet.createRow(index + 1)
                row.createCell(0).setCellValue(employee?.employeeCode ?: "#${event.employeeId}")
                row.createCell(1).setCellValue(employee?.fullName ?: "")
                row.createCell(2).setCellValue(event.deviceId.toString())
                row.createCell(3).setCellValue(typeLabel(event.type))
                row.createCell(4).setCellValue(formatter.format(event.eventTime))
            }

            // Fixed widths (1/256th of a character) — avoids autoSizeColumn's AWT/font
            // dependency, which is unreliable in a headless server.
            COLUMN_WIDTHS.forEachIndexed { i, chars -> sheet.setColumnWidth(i, chars * 256) }

            val out = ByteArrayOutputStream()
            wb.write(out)
            return out.toByteArray()
        }
    }

    private fun buildFilename(period: ReportPeriod, date: LocalDate): String {
        val suffix = when (period) {
            ReportPeriod.DAY -> "ngay-$date"
            ReportPeriod.WEEK -> "tuan-$date"
            ReportPeriod.MONTH -> "thang-${date.year}-%02d".format(date.monthValue)
        }
        return "bao-cao-cham-cong_$suffix.xlsx"
    }

    private fun typeLabel(type: AttendanceType): String = when (type) {
        AttendanceType.CHECK_IN -> "Vào ca"
        AttendanceType.CHECK_OUT -> "Ra ca"
    }

    private companion object {
        val HEADERS = listOf("Mã cán bộ", "Họ và tên", "Thiết bị", "Loại", "Thời điểm")
        val COLUMN_WIDTHS = listOf(15, 28, 12, 12, 22)
    }
}
