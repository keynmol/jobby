package frontend

import scala.scalajs.js.Date

import cats.effect.IO
import cats.syntax.all.*

import jobby.spec.AuthHeader
import jobby.spec.*

import com.raquo.laminar.api.L.*
import scala.concurrent.duration.FiniteDuration
import com.raquo.airstream.core.EventStream

class AuthRefresh(bus: EventBus[AuthEvent], period: FiniteDuration)(using
    state: AppState,
    api: Api
):
  def loop =
    eventSources
      .withCurrentValueOf(state.$token)
      .flatMap {
        case (_, None) => refresh

        case (AuthEvent.Reset, _) => logout

        case (AuthEvent.Force(value), _) =>
          EventStream.fromValue(Some(value))

        case (_, Some(AuthState.Unauthenticated)) =>
          EventStream.empty

        case (_, Some(AuthState.Token(t, d, maxAge))) =>
          val secondsSinceIssue = (Date.now - d.valueOf()) / 1000

          val remaining = maxAge - secondsSinceIssue

          if remaining <= 60 then refresh
          else EventStream.empty
      } --> state.tokenWriter

  private def refresh =
    api
      .stream(
        _.users
          .refresh(None)
          .map { out =>
            out.access_token
              .map(tok => AuthHeader("Bearer " + tok.value))
              .map(AuthState.Token(_, new Date, out.expires_in.value.toInt))
          }
          .recoverWith { case _: UnauthorizedError =>
            api.users
              .refresh(None, logout = Some(true))
              .as(Some(AuthState.Unauthenticated))
          }
      )

  private def logout: EventStream[Some[AuthState]] =
    api
      .stream(
        _.users
          .refresh(None, logout = Some(true))
          .as(Some(AuthState.Unauthenticated))
      )

  private val eventSources: EventStream[AuthEvent] = EventStream
    .merge(
      bus.events,
      EventStream.periodic(period.toMillis.toInt).mapTo(AuthEvent.Check),
      state.$token.changes.collect { case None => AuthEvent.Reset }
    )

end AuthRefresh
