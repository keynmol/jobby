import sbt.VirtualAxis

sealed abstract class BuildStyle(
    val idSuffix: String,
    val directorySuffix: String
) extends VirtualAxis.WeakAxis
    with Product
    with Serializable

object BuildStyle {
  case object SingleFile extends BuildStyle("-bundle", "bundle")
  case object Modules    extends BuildStyle("-modules", "modules")
}
