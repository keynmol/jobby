package frontend

import com.raquo.laminar.api.L.*

import jobby.spec.*
import com.raquo.waypoint.Router
import java.text.DecimalFormat

case class JobListing private (node: Node)

def format(price: Int) =
  DecimalFormat("###,###").format(price.toLong)

def currency(cur: Currency): String =
  import Currency.*
  cur match
    case GBP => "£"
    case USD => "$"
    case EUR => "€"

object JobListing:
  def apply(
      job: Job,
      companyId: CompanyId,
      companyName: CompanyName,
      allowDelete: Boolean = false,
      onDelete: Job => Unit = _ => ()
  )(using Router[Page]): JobListing =
    import job.attributes.*

    val cur = currency(range.currency)

    val deleteLink =
      Option.when(allowDelete) {
        span(
          " ",
          a(
            Styles.jobListing.deleteLink,
            "(delete)",
            href := "#",
            onClick.preventDefault --> { _ =>
              val sure =
                org.scalajs.dom.window.confirm(
                  s"Are you sure you want to delete ${job.attributes.title}?"
                )

              if sure then onDelete(job)
            }
          )
        )
      }

    val node = div(
      Styles.jobListing.container,
      h3(
        a(href := url.value, title.value, Styles.jobListing.title),
        deleteLink
      ),
      "at ",
      a(
        Styles.company.nameUrl,
        navigateTo(Page.CompanyPage(companyId)),
        companyName.value
      ),
      p(Styles.jobListing.description, code(description.value)),
      p(
        Styles.jobListing.salaryRange,
        s"$cur${format(range.min.value)} - $cur${format(range.max.value)}"
      )
    )

    JobListing(node)
  end apply
end JobListing
