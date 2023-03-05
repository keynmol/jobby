package frontend

import com.raquo.laminar.api.L.*
import org.scalajs.dom
import jobby.spec.AuthHeader
import scala.scalajs.js.Date
import com.raquo.airstream.state.StrictSignal
import com.raquo.airstream.core.Signal
import com.raquo.airstream.core.Observer

enum AuthState:
  case Unauthenticated
  case Token(value: AuthHeader, renewed: Date, length: Int)

class AppState private (
    _authToken: Var[Option[AuthState]],
    val events: EventBus[AuthEvent]
):
  val $token: StrictSignal[Option[AuthState]] = _authToken.signal

  val $authHeader: Signal[Option[AuthHeader]] = _authToken.signal.map {
    case Some(tok: AuthState.Token) => Some(tok.value)
    case _                          => None

  }

  def authHeader: Option[AuthHeader] = _authToken.now() match
    case Some(tok: AuthState.Token) => Some(tok.value)
    case _                          => None

  val tokenWriter: Observer[Option[AuthState]] = _authToken.writer
end AppState

object AppState:
  def init: AppState =
    AppState(
      _authToken = Var(None),
      events = EventBus[AuthEvent]()
    )

end AppState
