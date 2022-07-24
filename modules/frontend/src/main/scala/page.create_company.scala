package frontend
package pages

import views.*

import com.raquo.laminar.api.L.*
import jobby.spec.*
import scala.concurrent.ExecutionContext.Implicits.global
import com.raquo.waypoint.Router

def create_company(using state: AppState, api: Api, router: Router[Page]) =
  val error = Var(Option.empty[String])

  val companyHandler = Observer[CompanyAttributes] { cc =>
    state.authHeader match
      case None => println("oh noes")
      case Some(h) =>
        api
          .future(
            _.companies
              .createCompany(
                auth = h,
                attributes = cc
              )
              .attempt
          )
          .collect {
            case Left(ValidationError(msg)) =>
              error.set(Some(msg))
            case Right(res) =>
              redirectTo(Page.CompanyPage(res.id))
          }
  }

  val createCompany = CreateCompanyForm(companyHandler, error.signal)

  div(
    createCompany.node
  )
end create_company
