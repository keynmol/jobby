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

def logout(using state: AppState, api: Api, router: Router[Page]) =
  div(
    child.maybe <-- api
      .stream(_.users.refresh(logout = Some(true)))
      .map { _ =>
        state.events.emit(AuthEvent.Reset)
        redirectTo(Page.LatestJobs)
        None
      }
  )
end logout
