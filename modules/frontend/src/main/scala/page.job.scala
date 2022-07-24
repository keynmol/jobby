package frontend
package pages

import views.*

import com.raquo.laminar.api.L.*
import jobby.spec.*
import scala.scalajs.js.Date
import scala.concurrent.ExecutionContext.Implicits.global
import com.raquo.waypoint.Router

def job(
    page: Signal[Page.JobPage]
)(using state: AppState, api: Api, router: Router[Page]) =
  div("yo")
end job
