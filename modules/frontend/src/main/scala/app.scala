package frontend

import com.raquo.laminar.api.L.*
import org.scalajs.dom
import com.raquo.waypoint.Router
import scala.scalajs.js.Date
import cats.effect.IO

import scala.concurrent.ExecutionContext.Implicits.global
import jobby.spec.Tokens
import jobby.spec.AccessToken
import jobby.spec.CompanyAttributes
import com.raquo.waypoint.SplitRender
import com.raquo.waypoint.Router

enum AuthEvent:
  case Check, Reset
  case Force(value: AuthState)

object Main:

  def renderPage(using
      router: Router[Page]
  )(using state: AppState, api: Api): Signal[HtmlElement] =
    SplitRender[Page, HtmlElement](router.currentPageSignal)
      .collectStatic(Page.Login)(pages.login)
      .collectStatic(Page.Logout)(pages.logout)
      .collectStatic(Page.LatestJobs)(pages.latest_jobs)
      .collectStatic(Page.Register)(pages.register)
      .collectStatic(Page.CreateCompany)(pages.create_company)
      .collectStatic(Page.CreateJob)(pages.create_job)
      .collectStatic(Page.Profile)(pages.profile)
      .collectSignal[Page.CompanyPage](pages.company)
      .collectSignal[Page.JobPage](pages.job)
      .signal

  def main(args: Array[String]): Unit =
    given state: AppState = AppState.init
    given Router[Page]    = Page.router
    given Api             = Api.create()

    val tokenRefresh = AuthRefresh(state.events, Config.tokenRefreshPeriod)

    val userToolbar = UserToolbar.apply

    val app = div(
      Styles.container,
      headerTag(
        Styles.headerContainer,
        div(
          a(Styles.logo, "Jobby", navigateTo(Page.LatestJobs)),
          span(
            Styles.logoTagline,
            "because you're worth it (better job that is)"
          ),
          p(
            small(
              "This is not a real job site, it's a project from a ",
              a(
                href := "https://blog.indoorvivants.com/2022-06-10-smithy4s-fullstack-part-1",
                "blog post series"
              )
            )
          )
        ),
        userToolbar.node
      ),
      div(
        Styles.contentContainer,
        child <-- renderPage,
        tokenRefresh.loop
      )
    )

    renderOnDomContentLoaded(
      dom.document.getElementById("appContainer"), {
        import scalacss.ProdDefaults.*

        val sty = styleTag(Styles.render[String], `type` := "text/css")
        dom.document.querySelector("head").appendChild(sty.ref)

        app
      }
    )
  end main
end Main

def authenticatedOnly(using router: Router[Page], state: AppState) =
  state.$token --> { tok =>
    tok match
      case None | Some(AuthState.Unauthenticated) => redirectTo(Page.Login)
      case _                                      =>
  }

def guestOnly(using router: Router[Page], state: AppState) =
  state.$token --> { tok =>
    tok match
      case None | Some(AuthState.Unauthenticated) =>
      case _                                      => redirectTo(Page.LatestJobs)
  }

def redirectTo(pg: Page)(using router: Router[Page]) =
  router.pushState(pg)

def forceRedirectTo(pg: Page)(using router: Router[Page]) =
  router.replaceState(pg)
