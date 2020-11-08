import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import edu.illinois.cs.cs125.cisapi.Course
import edu.illinois.cs.cs125.cisapi.CourseSummary
import edu.illinois.cs.cs125.cisapi.Schedule
import edu.illinois.cs.cs125.cisapi.ScheduleYear
import edu.illinois.cs.cs125.cisapi.ScheduleYearSemester
import edu.illinois.cs.cs125.cisapi.ScheduleYearSemesterDepartment
import edu.illinois.cs.cs125.cisapi.Section
import edu.illinois.cs.cs125.cisapi.fromXml
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.collections.shouldHaveAtLeastSize
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.string.shouldStartWith
import java.io.File

private fun String.load() = TestSchedule::class.java.getResource("/$this").readText()

@Suppress("BlockingMethodInNonBlockingContext")
class TestSchedule : StringSpec({
    "should load local schedule.xml properly" {
        "schedule.xml".load().fromXml<Schedule>().also { schedule ->
            schedule.calendarYears shouldHaveSize 17
            schedule.calendarYears.map { year -> year.id }.sorted() shouldContainInOrder (2004..2020).toList()
            schedule.calendarYears.forEach {
                it.href.toString() shouldEndWith "${it.year}.xml"
            }
        }
    }
    "should load remote schedule.xml properly" {
        Schedule.fetch().also { schedule ->
            schedule.calendarYears shouldHaveSize 18
            schedule.calendarYears.map { year -> year.id }.sorted() shouldContainInOrder (2004..2020).toList()
            schedule.calendarYears.forEach {
                it.href.toString() shouldEndWith "${it.year}.xml"
            }
        }
    }
    "should load local 2020.xml properly" {
        "schedule_2020.xml".load().fromXml<ScheduleYear>().also { year ->
            year.terms shouldHaveSize 4
            year.terms.forEach {
                it.id shouldStartWith "12020"
            }
        }
    }
    "should load remote 2020.xml properly" {
        ScheduleYear.fetch("2020").also { year ->
            year.terms shouldHaveSize 4
            year.terms.forEach {
                it.id shouldStartWith "12020"
            }
        }
    }
    "should load local 2020/fall.xml properly" {
        "schedule_2020_fall.xml".load().fromXml<ScheduleYearSemester>().also { semester ->
            semester.subjects.size shouldBeGreaterThan 0
            semester.subjects.find { it.id == "CS" }?.department shouldBe "Computer Science"
            semester.parents.calendarYear.year shouldBe 2020
        }
    }
    "should load remote 2020/fall.xml properly" {
        ScheduleYearSemester.fetch("2020", "fall").also { semester ->
            semester.subjects.size shouldBeGreaterThan 0
            semester.subjects.find { it.id == "CS" }?.department shouldBe "Computer Science"
            semester.parents.calendarYear.year shouldBe 2020
        }
    }
    "should load local 2020/fall/CS.xml properly" {
        "schedule_2020_fall_CS.xml".load().fromXml<ScheduleYearSemesterDepartment>().also { department ->
            department.contactName shouldBe "Nancy Amato"
            department.courses.find { it.id == "125" }?.name shouldBe "Intro to Computer Science"
            department.parents.calendarYear.year shouldBe 2020
            department.parents.term.semester shouldBe "Fall 2020"
        }
    }
    "should load remote 2020/fall/CS.xml properly" {
        ScheduleYearSemesterDepartment.fetch("2020", "fall", "CS").also { department ->
            department.contactName shouldBe "Nancy Amato"
            department.courses.find { it.id == "125" }?.name shouldBe "Intro to Computer Science"
            department.parents.calendarYear.year shouldBe 2020
            department.parents.term.semester shouldBe "Fall 2020"
        }
    }
    "should load local 2020/fall/CS/100.xml properly" {
        "schedule_2020_fall_CS_100.xml".load().fromXml<Course>().also { course ->
            course.label shouldBe "Freshman Orientation"
            course.href shouldNotBe null
            course.description shouldStartWith "Introduction to Computer Science as a field and career"
            course.creditHours shouldBe "1 hours."
        }
    }
    "should load remote 2020/fall/CS/100.xml properly" {
        Course.fetch("2020", "fall", "CS", "100").also { course ->
            course.label shouldBe "Freshman Orientation"
            course.description shouldStartWith "Introduction to Computer Science as a field and career"
            course.creditHours shouldBe "1 hours."
        }
    }
    "should load local 2020/fall/CS/100/30094.xml properly" {
        "schedule_2020_fall_CS_100_30094.xml".load().fromXml<Section>().also { section ->
            section shouldNotBe null
        }
    }
    "should load remote 2020/fall/CS/100/30094.xml properly" {
        Section.fetch("2020", "fall", "CS", "100", "30094").also { section ->
            section shouldNotBe null
        }
    }

    "!should fetch all departments" {
        ScheduleYearSemester.fetch("2020", "fall").departments().also { departments ->
            departments shouldHaveAtLeastSize 0
        }
    }
    "!should fetch all department courses" {
        ScheduleYearSemesterDepartment.fetch("2020", "fall", "CS").courses().also { courses ->
            courses shouldHaveAtLeastSize 0
        }
    }
    "!should fetch all course sections" {
        Course.fetch("2020", "fall", "CS", "125").sections().also { sections ->
            sections shouldHaveAtLeastSize 0
        }
    }
    "!should fetch all department courses and sections" {
        ScheduleYearSemesterDepartment.fetch("2020", "fall", "CS").courses()
            .flatMap { it.sections() }
            .also { sections ->
                sections shouldHaveAtLeastSize 0
            }
    }
    "!should fetch and save all courses" {
        val courses = ScheduleYearSemester.fetch("2019", "fall").departments().flatMap { it.courses() }
        val mapper = ObjectMapper().also {
            it.setSerializationInclusion(JsonInclude.Include.NON_NULL)
        }
        mapper.writerWithDefaultPrettyPrinter()
            .writeValue(File(TestSchedule::class.java.getResource("/").path + "courses.json"), courses)
    }
    "!should fetch and save all course summaries" {
        val year = "2020"
        val semester = "fall"
        val courses = ScheduleYearSemester.fetch("2020", "fall").departments()
            .flatMap { it.courses() }
            .map { CourseSummary(it) }
        val mapper = ObjectMapper().also {
            it.setSerializationInclusion(JsonInclude.Include.NON_NULL)
        }
        mapper.writerWithDefaultPrettyPrinter().writeValue(
            File(
                TestSchedule::class.java.getResource("/").path + "${year}_${semester}_summary.json"
            ), courses
        )
    }
    "f:should fetch and save all CS courses" {
        val year = "2020"
        val semester = "fall"

        val courses = ScheduleYearSemesterDepartment.fetch(year, semester, "CS").courses().shuffled()
        val mapper = ObjectMapper().also {
            it.setSerializationInclusion(JsonInclude.Include.NON_NULL)
        }
        mapper.writerWithDefaultPrettyPrinter().writeValue(
            File(TestSchedule::class.java.getResource("/").path + "${year}_${semester}_CS.json"), courses
        )
    }
    "f:should fetch and save all CS course summaries" {
        val year = "2020"
        val semester = "fall"

        val courses =
            ScheduleYearSemesterDepartment.fetch("2020", "fall", "CS").courses().shuffled().map { CourseSummary(it) }
        val mapper = ObjectMapper().also {
            it.setSerializationInclusion(JsonInclude.Include.NON_NULL)
        }
        mapper.writerWithDefaultPrettyPrinter().writeValue(
            File(
                TestSchedule::class.java.getResource("/").path + "${year}_${semester}_CS_summary.json"
            ), courses
        )
    }
    "!should fetch all courses and sections" {
        ScheduleYearSemester.fetch("2020", "fall").departments()
            .shuffled()
            .flatMap { it.courses() }
            .shuffled()
            .flatMap { it.sections() }
            .also { sections ->
                sections shouldHaveAtLeastSize 0
            }
    }
})
