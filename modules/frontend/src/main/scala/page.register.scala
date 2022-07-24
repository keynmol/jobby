package frontend
package pages

import views.*

import com.raquo.laminar.api.L.*
import jobby.spec.*
import scala.scalajs.js.Date
import scala.concurrent.ExecutionContext.Implicits.global
import com.raquo.waypoint.Router

def register(using state: AppState, api: Api, router: Router[Page]) =
  val error = Var(Option.empty[String])
  val handler = Observer[Credentials] { case Credentials(login, password) =>
    api
      .future(
        _.users
          .register(
            login,
            password
          )
          .attempt
      )
      .collect {
        case Left(ValidationError(msg)) =>
          error.set(Some(msg))
        case Right(_) =>
          redirectTo(Page.Login)
      }
  }

  val form = CredentialsForm("Create account", handler, error.signal).node

  div(
    guestOnly,
    h1(Styles.header, "Register"),
    form
  )

end register
