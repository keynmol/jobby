package jobby
package validation

import jobby.spec.*

def validateJobTitle(login: JobTitle) =
  val str = login.value.trim
  if str.length == 0 then err("Title cannot be empty")
  else if str.length < 10 || str.length > 50 then
    err("Title cannot be shorter than 10 or longer than 50 characters")
  else ok

def validateJobDescription(login: JobDescription) =
  val minLength = 100
  val maxLength = 5000

  val str = login.value.trim
  if str.length == 0 then err("Description cannot be empty")
  else if str.length < minLength || str.length > maxLength then
    err(
      s"Description cannot be shorter than $minLength or longer than $maxLength characters"
    )
  else ok
end validateJobDescription

def validateJobUrl(url: JobUrl) =
  import _root_.io.lemonlabs.uri.*

  Url.parse(url.value) match
    case u: AbsoluteUrl => ok
    case _              => err("Job URL must be a valid absolute URL")

def validateSalaryRange(range: SalaryRange) =
  if range.min.value <= range.max.value && range.min.value > 0 && range.max.value > 0
  then ok
  else err("Salary range invalid")
