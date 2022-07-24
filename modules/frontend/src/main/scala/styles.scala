package frontend

import scalacss.ProdDefaults.*
import scalacss.internal.StyleA

object Styles extends StyleSheet.Inline:
  import dsl.*

  val weirdBlue = rgba(255, 255, 0, 1)
  val yellow    = rgba(0, 164, 198, 1)
  val darkBlue  = rgb(0, 20, 24)
  val lightGray = grey(240)

  object Standalone extends StyleSheet.Standalone:
    import dsl.*
    "body" - (
      background := "linear-gradient(0deg, rgba(255,250,0,1) 5%, rgba(0,164,198,1) 100%);",
      backgroundAttachment := "fixed",
      backgroundRepeat.noRepeat,
      backgroundSize := "cover",
      fontFamily :=! "'Wotfard',Futura,-apple-system,sans-serif"
    )
    "html" - (
      height := 100.%%
    )
  end Standalone

  object latestJobs:
    val container = style(
      display.flex,
      flexDirection.column,
      gap := 15.px
    )

  object jobListing:
    val sampleText = style(color.white, padding(5.px))
    val container = style(
      backgroundColor := lightGray,
      padding         := 10.px,
      borderRadius    := 8.px
    )
    val deleteLink = style(
      color.darkred,
      fontWeight.bold
    )
    val title = style(
      fontSize := 1.3.rem,
      borderBottom(1.px, dashed, darkBlue),
      fontWeight.bold,
      fontStyle.italic,
      textDecorationLine.none,
      &.visited(color.black),
      color.black
    )
    val salaryRange = style(
      fontSize := 2.rem
    )
    val description = style(
      fontSize   := 1.5.rem,
      whiteSpace := "break-spaces"
    )

    private val curButtonMixin = mixin(
      padding  := 8.px,
      fontSize := 1.8.rem,
      fontWeight.bold,
      borderWidth := 1.px,
      borderColor := grey,
      borderStyle.solid,
      borderRadius := 3.px
    )
    val currencyButton = styleF.bool {
      case true =>
        mixin(
          curButtonMixin,
          color.white,
          backgroundColor.black
        )
      case false => mixin(curButtonMixin, backgroundColor.white)

    }
  end jobListing

  object company:
    val deleteLink = style(
      color.red,
      fontWeight.bold
    )
    val container = style(
      display.flex,
      flexDirection.column,
      gap := 15.px
    )
    val name = style(color.white, fontSize := 2.rem)

    val description = style(
      color.white,
      fontSize := 1.3.rem,
      margin   := 5.px,
      whiteSpace.preWrap
    )
    val url = style(
      color.white,
      fontSize := 1.5.rem
    )
    val internalUrl = style(
      textDecorationLine.none,
      &.visited(color.white),
      &.hover(textDecorationLine.underline)
    )

    val nameUrl = style(
      fontWeight.bold,
      fontStyle.italic,
      textDecorationLine.none,
      &.visited(color.black),
      &.hover(textDecorationLine.underline)
    )
  end company

  val container = style(
    padding  := 10.px,
    margin   := auto,
    maxWidth := 1024.px
  )

  val contentContainer = style(
    padding         := 15.px,
    borderRadius    := 10.px,
    borderColor     := grey,
    borderWidth     := 2.px,
    margin          := auto,
    maxWidth        := 1024.px,
    backgroundColor := darkBlue
  )

  val headerContainer = style(
    display.flex,
    flexDirection.row,
    justifyContent.spaceBetween,
    alignItems.center
  )

  val textInput = style(
    fontSize := 1.5.rem,
    padding  := 5.px
  )

  val error = style(
    border(1.px, red, solid),
    padding(10.px),
    fontWeight.bold,
    color := maroon,
    backgroundColor.white,
    margin := 5.px
  )

  val contentTitle = style(
    color.white
  )

  val logo =
    style(
      marginBottom := 1.px,
      fontSize     := 3.rem,
      textDecorationLine.none,
      &.visited(color.black),
      &.hover(textDecorationLine.underline)
    )
  val logoTagline = style(marginBottom := 10.px, display.block)

  object userToolbar:
    val container = style(
      display.flex,
      alignItems.end,
      justifyContent.right,
      gap(10.px)
    )
    val button = style(
      padding := 4.px,
      fontWeight.bold,
      fontSize          := 1.5.rem,
      borderWidth       := 0.px,
      borderBottomWidth := 4.px,
      borderBottomColor := black,
      backgroundColor   := white,
      cursor.pointer
    )
  end userToolbar

  val header = style(color.white)

  object form:
    val container = style(
      display.flex,
      width := 100.%%,
      flexDirection.column,
      gap := 15.px
    )
    val inputGroup = style(
      display.flex,
      width := 100.%%,
      flexDirection.row,
      alignContent.stretch,
      gap := 15.px
    )

    val inputLabel = style(
      flexGrow   := "1",
      flexShrink := "2",
      width      := 100.%%,
      fontSize   := 2.rem,
      color.white,
      textAlign.right
    )
    val inputField = style(
      flexGrow := "3",
      padding  := 4.px,
      fontSize := 1.2.rem,
      width    := 100.%%,
      backgroundColor.white
    )

    val submit = style(
      padding  := 5.px,
      fontSize := 2.rem
    )
  end form

  jobListing
  company
  latestJobs
  form

  Standalone.styles

end Styles

import com.raquo.laminar.api.L.*

// given Conversion[StyleA, Setter[HtmlElement]] with
//   def apply(st: StyleA): Setter[HtmlElement] = (cls := st.htmlClass)

given styleBinder: Conversion[StyleA, Modifier[HtmlElement]] with
  def apply(st: StyleA): Modifier[HtmlElement] = (cls := st.htmlClass)
