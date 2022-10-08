package frontend

import com.raquo.laminar.api.L.*
import jobby.spec.*

case class CreateCompanyForm private (
    node: Node,
    stream: Signal[CompanyAttributes]
)

object CreateCompanyForm:
  def apply(obs: Observer[CompanyAttributes], error: Signal[Option[String]]) =
    val stateVar = Var(
      CompanyAttributes(
        name = CompanyName(""),
        description = CompanyDescription(""),
        url = CompanyUrl("")
      )
    )

    val nameWriter =
      stateVar.updater[String]((s, n) => s.copy(name = CompanyName(n)))

    val descriptionWriter = stateVar.updater[String]((s, n) =>
      s.copy(description = CompanyDescription(n))
    )

    val urlWriter =
      stateVar.updater[String]((s, n) => s.copy(url = CompanyUrl(n)))

    val node =
      div(
        child.maybe <-- error.map(_.map(msg => div(Styles.error, msg))),
        form(
          Styles.form.container,
          onSubmit.preventDefault.mapTo(stateVar.now()) --> obs,
          inputGroup(
            "name",
            input(
              idAttr := "input-company-name",
              controlled(
                value <-- stateVar.signal.map(_.name.value),
                onInput.mapToValue --> nameWriter
              )
            )
          ),
          inputGroup(
            "description",
            textArea(
              idAttr := "input-company-description",
              rows   := 15,
              controlled(
                value <-- stateVar.signal.map(_.description.value),
                onInput.mapToValue --> descriptionWriter
              )
            )
          ),
          inputGroup(
            "url",
            input(
              idAttr := "input-company-url",
              controlled(
                value <-- stateVar.signal.map(_.url.value),
                onInput.mapToValue --> urlWriter
              )
            )
          ),
          button(
            tpe    := "submit",
            idAttr := "input-company-submit",
            "Create",
            Styles.form.submit
          )
        )
      )

    new CreateCompanyForm(node, stateVar.signal)
  end apply
end CreateCompanyForm
