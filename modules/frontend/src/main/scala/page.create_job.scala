package frontend
package pages

import views.*

import com.raquo.laminar.api.L.*
import jobby.spec.*
import scala.concurrent.ExecutionContext.Implicits.global
import com.raquo.waypoint.Router
import java.util.UUID

def create_job(using state: AppState, api: Api, router: Router[Page]) =
  val error = Var(Option.empty[String])
  val jobHandler = Observer[CreateJob] { cj =>
    state.authHeader match
      case None => println("oh noes")
      case Some(h) =>
        cj.companyId.foreach { companyId =>
          api
            .future(
              _.jobs
                .createJob(
                  auth = h,
                  companyId = companyId,
                  attributes = cj.attributes
                )
                .attempt
            )
            .collect {
              case Left(ValidationError(msg)) =>
                error.set(Some(msg))
              case Right(res) =>
                redirectTo(Page.CompanyPage(companyId))
            }
        }
  }

  val createJob = CreateJobListingForm(jobHandler, error.signal)

  div(
    createJob.node,
    child.maybe <-- createJob.stream.map { cj =>
      cj.companyId.map { cid =>
        div(
          p(
            Styles.jobListing.sampleText,
            "Here's what the listing will look like:"
          ),
          JobListing(
            Job(
              id = JobId(cid.value),
              companyId = cid,
              attributes = cj.attributes,
              added = JobAdded(smithy4s.Timestamp.nowUTC())
            ),
            cid,
            CompanyName("some company")
          ).node
        )
      }
    }
  )
end create_job
