package frontend
package pages

import com.raquo.laminar.api.L.*
import com.raquo.waypoint.Router

def job(
    page: Signal[Page.JobPage],
)(using state: AppState, api: Api, router: Router[Page]) =
  div("yo")
end job
