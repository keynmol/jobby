package frontend

import com.raquo.laminar.api.L.*
import com.raquo.waypoint.Router

import jobby.spec.*

case class UserToolbar private (node: Node)

object UserToolbar:
  def apply(using
      api: Api,
      state: AppState,
      router: Router[Page]
  ): UserToolbar =
    val logout =
      button(Styles.userToolbar.button, "Logout", navigateTo(Page.Logout))

    val register =
      button(Styles.userToolbar.button, "Register", navigateTo(Page.Register))

    val login =
      button(Styles.userToolbar.button, "Login", navigateTo(Page.Login))

    val addJob =
      button(Styles.userToolbar.button, "Add a job", navigateTo(Page.CreateJob))

    val profile =
      button(Styles.userToolbar.button, "Profile", navigateTo(Page.Profile))

    val addCompany =
      button(
        Styles.userToolbar.button,
        "Add a company",
        navigateTo(Page.CreateCompany)
      )

    val buttons = state.$token.map {
      _.toList.flatMap {
        case t: AuthState.Token => Seq(profile, addJob, addCompany, logout)
        case _                  => Seq(login, register)
      }
    }

    val node = div(
      Styles.userToolbar.container,
      children <-- buttons
    )

    UserToolbar(node)
  end apply
end UserToolbar
