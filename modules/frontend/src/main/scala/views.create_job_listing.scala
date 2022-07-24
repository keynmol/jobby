package frontend

import com.raquo.laminar.api.L.*

import jobby.spec.*
import java.util.UUID
import smithy4s.Newtype
import monocle.Lens

case class CreateJob(
    companyId: Option[CompanyId] = None,
    attributes: JobAttributes
)

case class CreateJobListingForm private (
    node: Node,
    stream: Signal[CreateJob]
)

object CreateJobListingForm:
  def apply(submit: Observer[CreateJob], error: Signal[Option[String]])(using
      api: Api,
      appState: AppState
  ) =
    val stateVar = Var(
      CreateJob(
        None,
        JobAttributes(
          title = JobTitle(""),
          description = JobDescription(""),
          url = JobUrl(""),
          range = SalaryRange(
            min = MinSalary(30000),
            max = MaxSalary(100000),
            currency = Currency.GBP
          )
        )
      )
    )
    import monocle.syntax.all.*

    def writer[T](f: CreateJob => Lens[CreateJob, T]) =
      stateVar.updater[T] { case (state, cur) =>
        val lens = f(state)
        lens.replace(cur)(state)
      }

    def writerNT[T](nt: Newtype[T], f: CreateJob => Lens[CreateJob, nt.Type]) =
      stateVar.updater[T] { case (state, cur) =>
        val lens = f(state)
        val v    = nt.apply(cur)
        lens.replace(v)(state)
      }

    def controlledNT(
        nt: Newtype[String],
        f: CreateJob => Lens[CreateJob, nt.Type]
    ) =
      controlled(
        value <-- stateVar.signal.map(cj => f(cj).get(cj).value),
        onInput.mapToValue --> writerNT(nt, f)
      )

    val currencyToggles = Currency.values.map { c =>
      val str = currency(c)

      button(
        str,
        tpe := "button",
        cls <-- stateVar.signal
          .map(_.attributes.range.currency == c)
          .map(Styles.jobListing.currencyButton)
          .map(_.className.value),
        onClick.mapTo(c) --> writer(_.focus(_.attributes.range.currency).optic)
      )
    }

    val myCompanies = appState.$authHeader.flatMap {
      case None => Signal.fromValue(List.empty)
      case Some(tok) =>
        api
          .stream(_.companies.myCompanies(tok))
          .map(_.companies)
          .map(_.map(company => company.attributes.name.value -> company.id))
          .startWith(List.empty)
    }

    val companySelector =
      select(
        children <-- myCompanies.map { c =>
          c.map { case (name, id) => option(name, value := id.toString) }
        },
        onChange.mapToValue
          .map(UUID.fromString)
          .map(CompanyId.apply)
          .map(Option.apply) --> writer(_.focus(_.companyId).optic)
      )

    val node = div(
      child.maybe <-- error.map(_.map(msg => div(Styles.error, msg))),
      form(
        Styles.form.container,
        onSubmit.preventDefault.mapTo(stateVar.now()) --> submit,
        myCompanies.map(
          _.headOption.map(_._2)
        ) --> writer(_.focus(_.companyId).optic),
        inputGroup("company", companySelector),
        inputGroup(
          "job title",
          input(
            controlledNT(
              JobTitle,
              _.focus(_.attributes.title).optic
            )
          )
        ),
        inputGroup(
          "url",
          input(
            controlledNT(
              JobUrl,
              _.focus(_.attributes.url).optic
            )
          )
        ),
        inputGroup(
          "description",
          textArea(
            rows := 5,
            controlledNT(
              JobDescription,
              _.focus(_.attributes.description).optic
            )
          )
        ),
        inputGroup(
          "minimum salary",
          div(
            input(
              Styles.form.inputField,
              tpe     := "range",
              minAttr := "30000",
              maxAttr <-- stateVar.signal.map(
                _.attributes.range.max.value.toString
              ),
              value    := "60000",
              stepAttr := "1000",
              onInput.mapToValue.map(_.toInt) --> writerNT(
                MinSalary,
                _.focus(_.attributes.range.min).optic
              )
            ),
            p(
              textAlign := "center",
              child.text <-- stateVar.signal
                .map(
                  _.attributes.range.min.value
                )
                .map(format)
            )
          )
        ),
        inputGroup(
          "maximum salary",
          div(
            input(
              Styles.form.inputField,
              tpe := "range",
              minAttr <-- stateVar.signal.map(
                _.attributes.range.min.value.toString
              ),
              maxAttr  := "250000",
              value    := "100000",
              stepAttr := "1000",
              onInput.mapToValue.map(_.toInt) --> writerNT(
                MaxSalary,
                _.focus(_.attributes.range.max).optic
              )
            ),
            p(
              textAlign := "center",
              child.text <-- stateVar.signal
                .map(
                  _.attributes.range.max.value
                )
                .map(format)
            )
          )
        ),
        inputGroup("", p(textAlign := "center", currencyToggles)),
        button(
          Styles.form.submit,
          tpe := "submit",
          "Add"
        )
      )
    )

    new CreateJobListingForm(node, stateVar.signal)
  end apply
end CreateJobListingForm

case class CompanySelector()
