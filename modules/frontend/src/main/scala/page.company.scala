package frontend
package pages

import com.raquo.laminar.api.L.*
import com.raquo.waypoint.Router
import jobby.spec.*

def company(
    page: Signal[Page.CompanyPage],
)(using state: AppState, api: Api, router: Router[Page]) =
  val companyInfo = Var(Option.empty[Company])

  val fetchCompanyInfo =
    page.flatMapSwitch { case Page.CompanyPage(id) =>
      api
        .stream(
          _.companies
            .getCompany(id),
        )
    } --> companyInfo.someWriter

  div(
    fetchCompanyInfo,
    child.maybe <-- companyInfo.signal.map(
      _.map(CompanyListing.apply(_)).map(_.node),
    ),
    div(
      Styles.latestJobs.container,
      children <-- companyInfo.signal.flatMapSwitch {
        case None => Signal.fromValue(Nil)
        case Some(company) =>
          api
            .stream(
              _.jobs
                .listJobs(company.id)
                .map(_.jobs)
                .map(
                  _.map(
                    JobListing
                      .apply(_, company.id, company.attributes.name)
                      .node,
                  ),
                ),
            )
            .startWith(Nil)
      },
    ),
  )
end company
