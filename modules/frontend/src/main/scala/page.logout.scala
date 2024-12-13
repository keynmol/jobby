package frontend
package pages

import com.raquo.laminar.api.L.*
import com.raquo.waypoint.Router

def logout(using state: AppState, api: Api, router: Router[Page]) =
  div(
    child.maybe <-- api
      .stream(_.users.refresh(logout = Some(true)))
      .map { _ =>
        state.events.emit(AuthEvent.Reset)
        redirectTo(Page.LatestJobs)
        None
      },
  )
end logout
