package frontend
package pages

import views.*

import com.raquo.laminar.api.L.*
import jobby.spec.AccessToken
import jobby.spec.Tokens
import jobby.spec.AuthHeader
import scala.scalajs.js.Date
import scala.concurrent.ExecutionContext.Implicits.global

import com.raquo.waypoint.Router

def login(using state: AppState, api: Api, router: Router[Page]) =
  val error = Var(Option.empty[String])
  val handler = Observer[Credentials] { case Credentials(login, password) =>
    api
      .future(
        _.users
          .login(
            login,
            password
          )
          .attempt
      )
      .collect {
        case l @ Left(_) =>
          error.set(Some(l.toString))
        case Right(Tokens(AccessToken(tok), _, Some(expiresIn))) =>
          error.set(None)
          state.events.emit(
            AuthEvent.Force(
              AuthState.Token(
                AuthHeader("Bearer " + tok),
                new Date,
                expiresIn.value
              )
            )
          )

          redirectTo(Page.LatestJobs)
      }
  }

  val form = CredentialsForm("Login", handler, error.signal)

  div(guestOnly, h1(Styles.header, "Login"), form.node)
end login
