package jobby
package validation

import jobby.spec.*

def validateCompanyName(login: CompanyName) =
  val str = login.value.trim
  if str.length == 0 then err("Name cannot be empty")
  else if str.length < 3 || str.length > 100 then
    err("Name cannot be shorter than 3 or longer than 100 characters")
  else ok

def validateCompanyDescription(login: CompanyDescription) =
  val str = login.value.trim
  if str.length == 0 then err("Description cannot be empty")
  else if str.length < 100 || str.length > 5000 then
    err("Description cannot be shorter than 100 or longer than 5000 characters")
  else ok

def validateCompanyUrl(url: CompanyUrl) =
  import _root_.io.lemonlabs.uri.*

  Url.parse(url.value) match
    case u: AbsoluteUrl => ok
    case _              => err("Company URL must be a valid absolute URL")
