package jobby
package tests
package unit

import weaver.*
import jobby.spec.*

import weaver.scalacheck.*
import org.scalacheck.Gen

object ValidationPropertyTests extends SimpleIOSuite with Checkers:
  override def checkConfig: CheckConfig =
    CheckConfig.default.copy(
      minimumSuccessful = 500,
      initialSeed = Some(13378008L)
    )

  test("users: username") {
    forall(org.scalacheck.Gen.asciiPrintableStr) { str =>
      val trimmed = str.trim
      val isValid = jobby.validation.validateUserLogin(UserLogin(str)).isRight

      expect(isValid) or
        expect(
          trimmed.length < 12
            || trimmed.length > 50
            || trimmed.isEmpty
        )
    }
  }

  test("users: password") {
    forall(org.scalacheck.Gen.asciiPrintableStr) { str =>
      val trimmed = str.trim
      val isValid =
        jobby.validation.validateUserPassword(UserPassword(str)).isRight

      expect(isValid) or
        expect(
          trimmed.length < 12
            || trimmed.length > 128
            || str.exists(_.isWhitespace)
        )
    }
  }

  test("companies: name") {
    forall(org.scalacheck.Gen.asciiPrintableStr) { str =>
      val trimmed = str.trim
      val isValid =
        jobby.validation.validateCompanyName(CompanyName(str)).isRight

      expect(isValid) or
        expect(
          trimmed.isEmpty
            || trimmed.length > 100
            || trimmed.length < 3
        )
    }
  }

  test("companies: description") {
    forall(org.scalacheck.Gen.asciiPrintableStr) { str =>
      val trimmed = str.trim
      val isValid =
        jobby.validation
          .validateCompanyDescription(CompanyDescription(str))
          .isRight

      expect(isValid) or
        expect(
          trimmed.isEmpty
            || trimmed.length < 100
            || trimmed.length > 5000
        )
    }
  }

  test("jobs: title") {
    forall(org.scalacheck.Gen.asciiPrintableStr) { str =>
      val trimmed = str.trim
      val isValid =
        jobby.validation.validateJobTitle(JobTitle(str)).isRight

      expect(isValid) or
        expect(
          trimmed.isEmpty
            || trimmed.length < 10
            || trimmed.length > 50
        )
    }
  }

  test("jobs: description") {
    forall(org.scalacheck.Gen.asciiPrintableStr) { str =>
      val trimmed = str.trim
      val isValid =
        jobby.validation.validateJobDescription(JobDescription(str)).isRight

      expect(isValid) or
        expect(
          trimmed.trim.isEmpty
            || trimmed.length < 100
            || trimmed.length > 5000
        )
    }
  }

  test("jobs: salary range") {
    val rangeGen =
      for
        min      <- Gen.chooseNum(-10_000, 10_0000)
        max      <- Gen.chooseNum(-10_000, 10_0000)
        currency <- Gen.oneOf(Currency.values)
      yield SalaryRange(
        min = MinSalary(min),
        max = MaxSalary(max),
        currency = currency
      )

    given cats.Show[SalaryRange] = cats.Show.fromToString

    forall(rangeGen) { range =>
      val isValid =
        jobby.validation.validateSalaryRange(range).isRight

      expect(isValid) or
        expect(
          range.min.value > range.max.value
            || range.min.value <= 0
            || range.max.value <= 0
        )
    }
  }
end ValidationPropertyTests
