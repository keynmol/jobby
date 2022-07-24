package jobby
package validation

import jobby.spec.*

private[validation] def err[T](msg: String) =
  Left[ValidationError, T](ValidationError(msg))

private[validation] def ok =
  Right[ValidationError, Unit](())

def validateUserLogin(login: UserLogin) =
  val str = login.value.trim
  if str.length == 0 then err("Login cannot be empty")
  else if str.length < 5 || str.length > 50 then
    err("Login cannot be shorter than 5, or longer than 50 characters")
  else ok

def validateUserPassword(password: UserPassword) =
  val str = password.value
  if str.exists(_.isWhitespace) then
    err("Password cannot contain whitespace characters")
  else if str.length < 12 || str.length > 128 then
    err("Password cannot be shorter than 12 or longer than 128 characters")
  else ok
