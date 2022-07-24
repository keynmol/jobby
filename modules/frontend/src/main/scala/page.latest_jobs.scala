package frontend
package pages

import views.*

import com.raquo.laminar.api.L.*
import jobby.spec.*
import scala.scalajs.js.Date
import scala.concurrent.ExecutionContext.Implicits.global

import com.raquo.waypoint.Router
import jobby.spec.GetCompaniesOutput

def latest_jobs(using state: AppState, api: Api, router: Router[Page]) =
  div(
    h1(Styles.contentTitle, "Latest jobs"),
    div(
      Styles.latestJobs.container,
      children <-- api
        .stream(_.jobs.latestJobs().flatMap { latest =>
          val companyIds = latest.jobs.map(_.companyId)

          api.companies.getCompanies(companyIds).map {
            case GetCompaniesOutput(companies) =>
              val mapping = companies.map(c => c.id -> c).toMap

              latest.jobs.flatMap { job =>
                mapping
                  .get(job.companyId)
                  .map(company =>
                    JobListing(job, company.id, company.attributes.name).node
                  )
              }
          }
        })
    )
  )
end latest_jobs
