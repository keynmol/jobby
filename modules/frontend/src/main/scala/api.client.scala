package frontend

import scala.concurrent.Future

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.raquo.airstream.core.EventStream as LaminarStream
import jobby.spec.*
import org.http4s.*
import org.http4s.dom.*
import org.scalajs.dom
import smithy4s.http4s.*

class Api private (
    val companies: CompaniesService[IO],
    val jobs: JobService[IO],
    val users: UserService[IO],
):
  import org.scalajs.macrotaskexecutor.MacrotaskExecutor.Implicits.*
  def future[A](a: Api => IO[A]): Future[A] =
    a(this).unsafeToFuture()

  def stream[A](a: Api => IO[A]): LaminarStream[A] =
    LaminarStream.fromFuture(future(a))
end Api

object Api:
  def create(location: String = dom.window.location.origin) =
    val uri = Uri.unsafeFromString(location)

    val client = FetchClientBuilder[IO].create

    val companies =
      SimpleRestJsonBuilder(CompaniesService)
        .client(client)
        .uri(uri)
        .make
        .fold(throw _, identity)

    val jobs =
      SimpleRestJsonBuilder(JobService)
        .client(client)
        .uri(uri)
        .make
        .fold(throw _, identity)

    val users =
      SimpleRestJsonBuilder(UserService)
        .client(client)
        .uri(uri)
        .make
        .fold(throw _, identity)

    Api(companies, jobs, users)
  end create
end Api
