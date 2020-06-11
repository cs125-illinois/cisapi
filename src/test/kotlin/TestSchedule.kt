import edu.illinois.cs.cs125.cisapi.CalendarYears
import edu.illinois.cs.cs125.cisapi.ScheduleYear
import edu.illinois.cs.cs125.cisapi.ScheduleYearSemester
import edu.illinois.cs.cs125.cisapi.ScheduleYearSemesterDepartment
import edu.illinois.cs.cs125.cisapi.fromXml
import io.kotlintest.matchers.collections.shouldContainInOrder
import io.kotlintest.matchers.collections.shouldHaveSize
import io.kotlintest.matchers.numerics.shouldBeGreaterThan
import io.kotlintest.matchers.string.shouldEndWith
import io.kotlintest.matchers.string.shouldStartWith
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec

private fun String.load() = TestSchedule::class.java.getResource("/$this").readText()

class TestSchedule : StringSpec({
    "should load schedule.xml properly" {
        "schedule.xml".load().fromXml<CalendarYears>().also { schedule ->
            schedule.calendarYears shouldHaveSize 17
            schedule.calendarYears.map { year -> year.id }.sorted() shouldContainInOrder (2004..2020).toList()
            schedule.calendarYears.forEach {
                it.href.toString() shouldEndWith "${it.year}.xml"
            }
        }
    }
    "should load schedule/2020.xml properly" {
        "schedule_2020.xml".load().fromXml<ScheduleYear>().also { year ->
            year.terms shouldHaveSize 4
            year.terms.forEach {
                it.id shouldStartWith "12020"
            }
        }
    }
    "should load schedule/2020/fall.xml properly" {
        "schedule_2020_fall.xml".load().fromXml<ScheduleYearSemester>().also { semester ->
            semester.subjects.size shouldBeGreaterThan 0
            semester.subjects.find { it.id == "CS" }?.department shouldBe "Computer Science"
            semester.parents.calendarYear.year shouldBe 2020
        }
    }
    "should load schedule/2020/fall/CS.xml properly" {
        "schedule_2020_fall_CS.xml".load().fromXml<ScheduleYearSemesterDepartment>().also { department ->
            department.contactName shouldBe "Nancy Amato"
            department.courses.find { it.id == "125" }?.name shouldBe "Intro to Computer Science"
            department.parents.calendarYear.year shouldBe 2020
            department.parents.term.semester shouldBe "Fall 2020"
        }
    }
})
