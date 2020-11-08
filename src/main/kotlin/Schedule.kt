package edu.illinois.cs.cs125.cisapi

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.result.Result
import java.net.URI

internal val xmlMapper: ObjectMapper = XmlMapper(
    JacksonXmlModule().apply {
        setXMLTextElementName("innerText")
    }
).registerKotlinModule()//.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

internal val jsonMapper = ObjectMapper().also {
    it.setSerializationInclusion(JsonInclude.Include.NON_NULL)
}

private fun String.fixLink() =
    replace("http://cis.local/cisapi/", "https://courses.illinois.edu/cisapp/explorer/").let {
        if (!it.endsWith(".xml")) {
            "$it.xml"
        } else {
            it
        }
    }

data class Schedule(val label: String, val calendarYears: List<CalendarYear>) {
    data class CalendarYear(
        val id: Int,
        @get:JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        val href: URI,
        @JacksonXmlProperty(localName = "innerText") val year: Int
    ) {
        override fun toString() = year.toString()
    }

    companion object {
        fun fetch(): Schedule {
            val (_, _, result) = Fuel.get("https://courses.illinois.edu/cisapp/explorer/schedule.xml").responseString()
            when (result) {
                is Result.Failure -> throw(result.getException())
                is Result.Success -> return result.get().fromXml()
            }
        }
    }
}

data class ScheduleYear(val id: String, val label: String, val terms: List<Term>) {
    data class Term(
        val id: String,
        @get:JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        val href: URI,
        @JacksonXmlProperty(localName = "innerText") val semester: String
    ) {
        override fun toString() = semester
    }

    companion object {
        init {
            FuelManager.instance.basePath = "https://courses.illinois.edu/cisapp/explorer/"
        }

        fun fetch(year: String): ScheduleYear {
            val (_, _, result) = Fuel.get("schedule/$year.xml").responseString()
            when (result) {
                is Result.Failure -> throw(result.getException())
                is Result.Success -> return result.get().fromXml()
            }
        }
    }
}

data class ScheduleYearSemester(val id: String, val parents: Parents, val label: String, val subjects: List<Subject>) {
    data class Parents(val calendarYear: Schedule.CalendarYear)
    data class Subject(
        val id: String,
        @get:JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        val href: URI,
        @JacksonXmlProperty(localName = "innerText") val department: String
    )

    fun departments() =
        subjects.map { ScheduleYearSemesterDepartment.fetch(it.href.toString()) }

    companion object {
        init {
            FuelManager.instance.basePath = "https://courses.illinois.edu/cisapp/explorer/"
        }

        fun fetch(href: String): ScheduleYearSemester {
            val (_, _, result) = Fuel.get(href).responseString()
            when (result) {
                is Result.Failure -> throw(result.getException())
                is Result.Success -> return result.get().fromXml()
            }
        }

        fun fetch(year: String, semester: String): ScheduleYearSemester = fetch(
            "https://courses.illinois.edu/cisapp/explorer/schedule/$year/$semester.xml"
        )
    }
}

data class ScheduleYearSemesterDepartment(
    val id: String,
    val parents: Parents,
    val label: String,
    val collegeCode: String,
    val departmentCode: Int,
    val unitName: String,
    val contactName: String?,
    val contactTitle: String,
    val addressLine1: String?,
    val addressLine2: String,
    val phoneNumber: String?,
    val webSiteURL: String,
    val collegeDepartmentDescription: String,
    val subjectComment: String?,
    val courses: List<Course>
) {
    data class Parents(val calendarYear: Schedule.CalendarYear, val term: ScheduleYear.Term)
    data class Course(
        val id: String,
        @get:JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        val href: URI,
        @JacksonXmlProperty(localName = "innerText") val name: String
    )

    fun courses() = courses.map { edu.illinois.cs.cs125.cisapi.Course.fetch(it.href.toString()) }

    companion object {
        fun fetch(href: String): ScheduleYearSemesterDepartment {
            val (_, _, result) = Fuel.get(href).responseString()
            when (result) {
                is Result.Failure -> error(href)
                is Result.Success -> return result.get().fromXml()
            }
        }

        fun fetch(year: String, semester: String, department: String) = fetch(
            "https://courses.illinois.edu/cisapp/explorer/schedule/$year/$semester/$department.xml"
        )
    }
}

data class Course(
    @get:JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    val id: String,
    @get:JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    val href: String,
    val parents: Parents,
    @get:JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    val label: String,
    val description: String?,
    val creditHours: String,
    val courseSectionInformation: String?,
    val sectionDegreeAttributes: String?,
    val classScheduleInformation: String?,
    val sectionRegistrationNotes: String?,
    val sectionCappArea: String?,
    val sectionApprovalCode: String?,
    val genEdCategories: List<Category> = listOf(),
    val sectionDateRange: String?,
    val sectionDeptRestriction: String?,
    val sectionFeeAmount: String?,
    val courseCoRequisite: String?,
    val sectionDescription: String?,
    @get:JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    val sections: List<Section>
) {
    @get:JsonProperty(access = JsonProperty.Access.READ_ONLY)
    val year: String
        get() = parents.calendarYear.year.toString()

    @get:JsonProperty(access = JsonProperty.Access.READ_ONLY)
    val semester: String
        get() = parents.term.semester.split(" ").first().toLowerCase()

    @get:JsonProperty(access = JsonProperty.Access.READ_ONLY)
    val department: String
        get() = id.split(" ")[0]

    @get:JsonProperty(access = JsonProperty.Access.READ_ONLY)
    val number: String
        get() = id.split(" ")[1]

    @get:JsonProperty(access = JsonProperty.Access.READ_ONLY)
    val title: String
        get() = label

    @get:JsonProperty("sections", access = JsonProperty.Access.READ_ONLY)
    val sectionInfo: List<edu.illinois.cs.cs125.cisapi.Section> by lazy { sections() }

    data class Parents(
        val calendarYear: Schedule.CalendarYear,
        val term: ScheduleYear.Term,
        val subject: ScheduleYearSemester.Subject
    )

    data class Category(val id: String, val description: String, val genEdAttributes: List<Attribute>) {
        data class Attribute(val code: String, @JacksonXmlProperty(localName = "innerText") val name: String)
    }

    data class Section(
        val id: String,
        @get:JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        val href: URI,
        @JacksonXmlProperty(localName = "innerText") val name: String?
    )

    fun sections() = sections.map { edu.illinois.cs.cs125.cisapi.Section.fetch(it.href.toString()) }
    fun toJSON() = jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(this)

    companion object {
        fun fetch(href: String): Course {
            val (_, _, result) = Fuel.get(href.fixLink()).responseString()
            when (result) {
                is Result.Failure -> throw(result.getException())
                is Result.Success -> return result.get().fromXml()
            }
        }

        fun fetch(year: String, semester: String, department: String, number: String) = fetch(
            "https://courses.illinois.edu/cisapp/explorer/schedule/$year/$semester/$department/$number.xml"
        )
    }
}

data class CourseSummary(
    val year: String,
    val semester: String,
    val department: String,
    val number: String,
    val title: String
) {
    constructor(course: Course) : this(
        course.year,
        course.semester,
        course.department,
        course.number,
        course.title
    )
}

data class Section(
    val id: String,
    @get:JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    val href: String,
    val parents: Parents,
    val sectionNumber: String?,
    val statusCode: String,
    val sectionText: String?,
    val sectionNotes: String?,
    val partOfTerm: String?,
    val sectionStatusCode: String,
    val enrollmentStatus: String,
    val startDate: String?,
    val endDate: String?,
    val sectionCappArea: String?,
    val creditHours: String?,
    val sectionTitle: String?,
    val specialApproval: String?,
    val sectionDeptRestriction: String?,
    val sectionFeeAmount: String?,
    val sectionDateRange: String?,
    val sectionDegreeAttributes: String?,
    val sectionCoRequest: String?,
    val meetings: List<Meeting>
) {
    data class Parents(
        val calendarYear: Schedule.CalendarYear,
        val term: ScheduleYear.Term,
        val subject: ScheduleYearSemester.Subject,
        val course: ScheduleYearSemesterDepartment.Course
    )

    data class Meeting(
        val id: String,
        val type: String,
        val start: String,
        val end: String?,
        val daysOfTheWeek: String?,
        val instructors: List<Instructor>?,
        val roomNumber: String?,
        val buildingName: String?,
        val meetingDateRange: String?
    ) {
        data class Instructor(
            val lastName: String,
            val firstName: String,
            @JacksonXmlProperty(localName = "innerText") val name: String
        )
    }

    companion object {
        fun fetch(href: String): Section {
            val (_, _, result) = Fuel.get(href.fixLink()).responseString()
            when (result) {
                is Result.Failure -> error(href)
                is Result.Success -> return result.get().fromXml()
            }
        }

        fun fetch(year: String, semester: String, department: String, number: String, CRN: String) = fetch(
            "https://courses.illinois.edu/cisapp/explorer/schedule/$year/$semester/$department/$number/$CRN.xml"
        )
    }
}

internal inline fun <reified T> String.fromXml(): T = xmlMapper.readValue(this, T::class.java)

