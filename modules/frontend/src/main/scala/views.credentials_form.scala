package frontend
package views

import com.raquo.laminar.api.L.*
import jobby.spec.*

case class Credentials(
    login: UserLogin,
    password: UserPassword
)

case class CredentialsForm private (
    node: HtmlElement
)

object CredentialsForm:
  def apply(
      submitButton: String,
      submit: Observer[Credentials],
      error: Signal[Option[String]]
  ) =
    val credentials = Var(Credentials(UserLogin(""), UserPassword("")))

    val loginWriter = credentials.updater[UserLogin] { case (st, ul) =>
      st.copy(login = ul)
    }

    val passwordWriter = credentials.updater[UserPassword] { case (st, up) =>
      st.copy(password = up)
    }

    val node = div(
      form(
        onSubmit.preventDefault.mapTo(credentials.now()) --> submit,
        child.maybe <-- error.map(_.map(str => div(Styles.error, str))),
        inputGroup(
          "login",
          input(
            Styles.textInput,
            idAttr := "credentials-login",
            tpe    := "text",
            onInput.mapToValue.map(UserLogin.apply) --> loginWriter
          )
        ),
        inputGroup(
          "password",
          input(
            Styles.textInput,
            idAttr := "credentials-password",
            tpe    := "password",
            onInput.mapToValue.map(UserPassword.apply) --> passwordWriter
          )
        ),
        button(
          Styles.form.submit,
          idAttr := "credentials-submit",
          submitButton,
          tpe := "submit"
        )
      )
    )

    new CredentialsForm(node)
  end apply
end CredentialsForm
