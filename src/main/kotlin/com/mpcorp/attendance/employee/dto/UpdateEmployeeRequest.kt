package com.mpcorp.attendance.employee.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class UpdateEmployeeRequest(

    @field:NotBlank
    @field:Size(max = 200)
    val fullName: String,

    @field:NotBlank
    @field:Size(max = 200)
    val position: String,

    @field:NotBlank
    @field:Size(max = 200)
    val rank: String,
)
