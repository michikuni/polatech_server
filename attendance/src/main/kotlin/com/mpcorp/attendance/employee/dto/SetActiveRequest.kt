package com.mpcorp.attendance.employee.dto

import jakarta.validation.constraints.NotNull

data class SetActiveRequest(

    @field:NotNull
    val active: Boolean?,
)
