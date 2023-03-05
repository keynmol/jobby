package frontend

import jobby.spec.*
import com.raquo.laminar.api.L
import com.raquo.laminar.api.L.*
import com.raquo.waypoint.*
import java.util.UUID

import io.circe.{*, given}
import io.circe.syntax.*
import smithy4s.Newtype
import scala.scalajs.js.JSON

def codec[A: Decoder: Encoder](nt: Newtype[A]): Codec[nt.Type] =
  val decT = summon[Decoder[A]].map(nt.apply)
  val encT = summon[Encoder[A]].contramap(nt.value)

  Codec.from(decT, encT)

given Codec[CompanyId] = codec(CompanyId)
given Codec[JobId]     = codec(JobId)

sealed trait Page derives Codec.AsObject
object Page:
  case object LatestJobs                extends Page
  case object Login                     extends Page
  case object Logout                    extends Page
  case object Register                  extends Page
  case object CreateCompany             extends Page
  case object CreateJob                 extends Page
  case object Profile                   extends Page
  case class CompanyPage(id: CompanyId) extends Page
  case class JobPage(id: JobId)         extends Page

  val mainRoute    = Route.static(Page.LatestJobs, root / endOfSegments)
  val profileRoute = Route.static(Page.Profile, root / "me")
  val loginRoute   = Route.static(Page.Login, root / "login")
  val logoutRoute  = Route.static(Page.Logout, root / "logout")

  val createCompanyRoute =
    Route.static(Page.CreateCompany, root / "companies" / "create")
  val createJobRoute = Route.static(Page.CreateJob, root / "jobs" / "create")

  val registerRoute =
    Route.static(Page.Register, root / "register")

  val companyPageRoute = Route(
    encode = (stp: CompanyPage) => stp.id.toString,
    decode = (arg: String) => CompanyPage(CompanyId(UUID.fromString(arg))),
    pattern = root / "company" / segment[String] / endOfSegments
  )

  val jobPageRoute = Route(
    encode = (stp: JobPage) => stp.id.toString,
    decode = (arg: String) => JobPage(JobId(UUID.fromString(arg))),
    pattern = root / "job" / segment[String] / endOfSegments
  )

  val router = new Router[Page](
    routes = List(
      mainRoute,
      profileRoute,
      loginRoute,
      registerRoute,
      companyPageRoute,
      jobPageRoute,
      createCompanyRoute,
      createJobRoute,
      logoutRoute
    ),
    getPageTitle = {
      case LatestJobs    => "Jobby: latest"
      case Login         => "Jobby: login"
      case Register      => "Jobby: register"
      case CreateCompany => "Jobby: create company"
      case CreateJob     => "Jobby: create vacancy"
      case _             => "Jobby"
    },
    serializePage = pg => pg.asJson.noSpaces,
    deserializePage = str =>
      io.circe.scalajs.decodeJs[Page](JSON.parse(str)).fold(throw _, identity)
  )(
    popStateEvents = windowEvents(_.onPopState),
    owner = L.unsafeWindowOwner
  )
end Page

def navigateTo(page: Page)(using router: Router[Page]): Binder[HtmlElement] =
  Binder { el =>
    import org.scalajs.dom

    val isLinkElement = el.ref.isInstanceOf[dom.html.Anchor]

    if isLinkElement then el.amend(href(router.absoluteUrlForPage(page)))

    (onClick
      .filter(ev =>
        !(isLinkElement && (ev.ctrlKey || ev.metaKey || ev.shiftKey || ev.altKey))
      )
      .preventDefault
      --> (_ => redirectTo(page))).bind(el)
  }
