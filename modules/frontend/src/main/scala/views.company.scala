package frontend

import com.raquo.laminar.api.L.*
import com.raquo.waypoint.Router
import jobby.spec.Company

case class CompanyListing private (node: Node)

object CompanyListing:
  def apply(
      company: jobby.spec.Company,
      allowDelete: Boolean = false,
      onDelete: Company => Unit = _ => ()
  )(using Router[Page]): CompanyListing =
    import company.attributes.*

    val deleteLink =
      Option.when(allowDelete) {
        span(
          " ",
          a(
            Styles.company.deleteLink,
            "(delete)",
            href := "#",
            onClick.preventDefault --> { _ =>
              val sure =
                org.scalajs.dom.window.confirm(
                  s"Are you sure you want to delete ${company.attributes.name}?\n" +
                    "Note: the company will still exist in the physical world"
                )

              if sure then onDelete(company)
            }
          )
        )
      }

    val node = div(
      Styles.company.container,
      h2(
        Styles.company.name,
        a(
          navigateTo(Page.CompanyPage(company.id)),
          idAttr := "company-profile-name",
          name.value,
          Styles.company.internalUrl
        ),
        span(" ", deleteLink)
      ),
      a(
        url.value,
        idAttr := "company-profile-url",
        href   := url.value,
        Styles.company.url
      ),
      pre(
        description.value,
        idAttr := "company-profile-description",
        Styles.company.description
      )
    )

    CompanyListing(node)
  end apply
end CompanyListing
