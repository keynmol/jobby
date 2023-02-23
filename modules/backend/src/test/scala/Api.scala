package jobby
package tests

import cats.effect.*
import cats.syntax.all.*

import jobby.spec.*

import org.http4s.*
import org.http4s.client.*
import smithy4s.http4s.SimpleRestJsonBuilder

case class Api(
    companies: CompaniesService[IO],
    jobs: JobService[IO],
    users: UserService[IO],
    health: HealthService[IO]
)

object Api:
  def build(client: Client[IO], uri: Uri): IO[Api] =
    val companies = IO.fromEither(
      SimpleRestJsonBuilder(CompaniesService)
        .client(client)
        .uri(uri)
        .use
    )
    val jobs = IO.fromEither(
      SimpleRestJsonBuilder(JobService)
        .client(client)
        .uri(uri)
        .use
    )

    val users = IO.fromEither(
      SimpleRestJsonBuilder(UserService)
        .client(client)
        .uri(uri)
        .use
    )

    val health = IO.fromEither(
      SimpleRestJsonBuilder(HealthService)
        .client(client)
        .uri(uri)
        .use
    )

    (companies, jobs, users, health).mapN(Api.apply)
  end build
end Api
