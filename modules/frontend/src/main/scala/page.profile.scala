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

import cats.syntax.all.*
import jobby.spec.Company
import jobby.spec.Job
import com.raquo.airstream.core.EventStream

def profile(using state: AppState, api: Api, router: Router[Page]) =
  val myCompanies = state.$authHeader.flatMap {
    case None => Signal.fromValue(List.empty)
    case Some(tok) =>
      val result: EventStream[List[(Company, List[Job])]] = api.stream { a =>
        a.companies.myCompanies(tok).map(_.companies).flatMap { companies =>
          companies.traverse(company =>
            a.jobs.listJobs(company.id).map(_.jobs).map(company -> _)
          )
        }
      }

      val deleted          = Var(Set.empty[jobby.spec.JobId])
      val deletedCompanies = Var(Set.empty[jobby.spec.CompanyId])

      inline def renderCompany(company: Company) =
        CompanyListing(
          company,
          true,
          onDelete = company =>
            api.future(_.companies.deleteCompany(tok, company.id)).map { _ =>
              deletedCompanies.update(_ + company.id)
            }
        ).node

      inline def renderJob(job: Job, company: Company) =
        JobListing(
          job,
          company.id,
          company.attributes.name,
          allowDelete = true,
          onDelete = job =>
            api.future(_.jobs.deleteJob(tok, job.id)).map { _ =>
              deleted.update(_ + job.id)
            }
        ).node

      result
        .map { details =>
          details.map { case (company, jobs) =>
            div(
              child.maybe <-- deletedCompanies.signal.map { isDeleted =>
                if isDeleted(company.id) then None
                else
                  Option(
                    div(
                      renderCompany(company),
                      children <-- deleted.signal.map { isDeleted =>
                        jobs.filterNot(j => isDeleted(j.id)).map { job =>
                          renderJob(job, company)
                        }
                      }
                    )
                  )
              }
            )
          }
        }
        .startWith(List(i("You haven't created any companies yet...")))
  }
  div(
    children <-- myCompanies
  )
end profile
