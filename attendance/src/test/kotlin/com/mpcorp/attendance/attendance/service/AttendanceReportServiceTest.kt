package com.mpcorp.attendance.attendance.service

import com.mpcorp.attendance.attendance.entity.AttendanceEvent
import com.mpcorp.attendance.attendance.entity.AttendanceType
import com.mpcorp.attendance.attendance.repository.AttendanceEventRepository
import com.mpcorp.attendance.employee.entity.Employee
import com.mpcorp.attendance.employee.repository.EmployeeRepository
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.mockito.ArgumentMatchers.any
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import java.io.ByteArrayInputStream
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AttendanceReportServiceTest {

    private val attendanceEventRepository = mock(AttendanceEventRepository::class.java)
    private val employeeRepository = mock(EmployeeRepository::class.java)
    private val zone = ZoneId.of("Asia/Ho_Chi_Minh")
    private val service = AttendanceReportService(attendanceEventRepository, employeeRepository, zone)

    private val date = LocalDate.of(2026, 6, 29)

    private fun at(hour: Int, minute: Int): Instant =
        date.atTime(hour, minute).atZone(zone).toInstant()

    private fun event(empId: Long, devId: Long, type: AttendanceType, time: Instant) =
        AttendanceEvent(employeeId = empId, deviceId = devId, type = type, eventTime = time, challengeId = 1L)
            .apply { id = time.toEpochMilli() }

    private fun employee(id: Long, code: String, name: String) =
        Employee(employeeCode = code, fullName = name, position = "P", rank = "R", active = true)
            .apply { this.id = id }

    // --- Period range computation ---------------------------------------

    @Test
    fun `DAY range covers a single business day`() {
        val (from, to) = ReportPeriod.DAY.range(date, zone)
        assertEquals(date.atStartOfDay(zone).toInstant(), from)
        assertEquals(date.plusDays(1).atStartOfDay(zone).toInstant(), to)
    }

    @Test
    fun `WEEK range is Monday through the next Monday around the date`() {
        // 2026-07-01 is a Wednesday; its ISO week runs Mon 2026-06-29 .. Mon 2026-07-06.
        val (from, to) = ReportPeriod.WEEK.range(LocalDate.of(2026, 7, 1), zone)
        assertEquals(LocalDate.of(2026, 6, 29).atStartOfDay(zone).toInstant(), from)
        assertEquals(LocalDate.of(2026, 7, 6).atStartOfDay(zone).toInstant(), to)
    }

    @Test
    fun `MONTH range spans the whole calendar month`() {
        val (from, to) = ReportPeriod.MONTH.range(LocalDate.of(2026, 7, 15), zone)
        assertEquals(LocalDate.of(2026, 7, 1).atStartOfDay(zone).toInstant(), from)
        assertEquals(LocalDate.of(2026, 8, 1).atStartOfDay(zone).toInstant(), to)
    }

    // --- Workbook content -----------------------------------------------

    @Test
    fun `all-employee report writes a header plus one row per punch`() {
        val from = date.atStartOfDay(zone).toInstant()
        val to = date.plusDays(1).atStartOfDay(zone).toInstant()
        val checkIn = event(5L, 3L, AttendanceType.CHECK_IN, at(9, 0))
        val checkOut = event(5L, 3L, AttendanceType.CHECK_OUT, at(17, 30))
        given(attendanceEventRepository.findForReport(from, to)).willReturn(listOf(checkOut, checkIn))
        given(employeeRepository.findAllById(any())).willReturn(listOf(employee(5L, "CB005", "Nguyễn Văn A")))

        val file = service.buildExcel(ReportPeriod.DAY, date, emptyList())

        assertEquals("bao-cao-cham-cong_ngay-2026-06-29.xlsx", file.filename)
        val sheet = XSSFWorkbook(ByteArrayInputStream(file.bytes)).getSheetAt(0)

        val header = sheet.getRow(0)
        assertEquals("Mã cán bộ", header.getCell(0).stringCellValue)
        assertEquals("Họ và tên", header.getCell(1).stringCellValue)
        assertEquals("Thiết bị", header.getCell(2).stringCellValue)
        assertEquals("Loại", header.getCell(3).stringCellValue)
        assertEquals("Thời điểm", header.getCell(4).stringCellValue)

        // Rows follow the query order (newest first): check-out then check-in.
        val r1 = sheet.getRow(1)
        assertEquals("CB005", r1.getCell(0).stringCellValue)
        assertEquals("Nguyễn Văn A", r1.getCell(1).stringCellValue)
        assertEquals("3", r1.getCell(2).stringCellValue)
        assertEquals("Ra ca", r1.getCell(3).stringCellValue)
        assertEquals("29/06/2026 17:30:00", r1.getCell(4).stringCellValue)

        assertEquals("Vào ca", sheet.getRow(2).getCell(3).stringCellValue)
        assertNull(sheet.getRow(3))
    }

    @Test
    fun `selecting officer codes resolves them and filters by id`() {
        val from = date.atStartOfDay(zone).toInstant()
        val to = date.plusDays(1).atStartOfDay(zone).toInstant()
        val checkIn = event(5L, 3L, AttendanceType.CHECK_IN, at(9, 0))
        given(employeeRepository.findByEmployeeCodeLowerIn(listOf("cb005")))
            .willReturn(listOf(employee(5L, "CB005", "Nguyễn Văn A")))
        given(attendanceEventRepository.findForReportByEmployees(listOf(5L), from, to)).willReturn(listOf(checkIn))
        given(employeeRepository.findAllById(any())).willReturn(listOf(employee(5L, "CB005", "Nguyễn Văn A")))

        val file = service.buildExcel(ReportPeriod.DAY, date, listOf("CB005"))

        val sheet = XSSFWorkbook(ByteArrayInputStream(file.bytes)).getSheetAt(0)
        assertEquals("CB005", sheet.getRow(1).getCell(0).stringCellValue)
        assertNull(sheet.getRow(2))
    }

    @Test
    fun `unknown officer codes produce a header-only report`() {
        given(employeeRepository.findByEmployeeCodeLowerIn(listOf("xxx"))).willReturn(emptyList())
        given(employeeRepository.findAllById(any())).willReturn(emptyList())

        val file = service.buildExcel(ReportPeriod.DAY, date, listOf("XXX"))

        val sheet = XSSFWorkbook(ByteArrayInputStream(file.bytes)).getSheetAt(0)
        assertEquals("Mã cán bộ", sheet.getRow(0).getCell(0).stringCellValue)
        assertNull(sheet.getRow(1))
    }
}
