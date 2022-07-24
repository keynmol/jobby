package frontend

import com.raquo.laminar.api.L.*

def inputGroup(name: String, el: HtmlElement) =
  div(
    Styles.form.inputGroup,
    label(name, Styles.form.inputLabel),
    el.amend(Styles.form.inputField)
  )
