package edu.illinois.cs.cs125.cisapi

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.net.URI

internal val xmlMapper: ObjectMapper = XmlMapper(
    JacksonXmlModule().apply {
        setXMLTextElementName("innerText")
    }
).registerKotlinModule()
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

data class CalendarYears(val label: String, val calendarYears: List<CalendarYear>) {
    data class CalendarYear(
        val id: Int,
        val href: URI,
        @JacksonXmlProperty(localName = "innerText") val year: Int
    )
}

data class ScheduleYear(val label: String, val terms: List<Term>) {
    data class Term(
        val id: String,
        val href: URI,
        @JacksonXmlProperty(localName = "innerText") val semester: String
    )
}

data class ScheduleYearSemester(val parents: Parents, val label: String, val subjects: List<Subject>) {
    data class Parents(val calendarYear: CalendarYears.CalendarYear)
    data class Subject(
        val id: String,
        val href: URI,
        @JacksonXmlProperty(localName = "innerText") val department: String
    )
}

data class ScheduleYearSemesterDepartment(
    val parents: Parents,
    val label: String,
    val collegeCode: String,
    val departmentCode: Int,
    val unitName: String,
    val contactName: String,
    val contactTitle: String,
    val addressLine1: String,
    val addressLine2: String,
    val phoneNumber: String,
    val webSiteURL: String,
    val collegeDepartmentDescription: String,
    val courses: List<Course>
) {
    data class Parents(val calendarYear: CalendarYears.CalendarYear, val term: ScheduleYear.Term)
    data class Course(
        val id: String,
        val href: URI,
        @JacksonXmlProperty(localName = "innerText") val name: String
    )
}

internal inline fun <reified T> String.fromXml(): T = xmlMapper.readValue(this, T::class.java)
