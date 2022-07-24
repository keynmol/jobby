package jobby
package tests
package stub

import java.util.UUID

import cats.effect.*
import cats.effect.std.*
import cats.syntax.all.*

import jobby.database.operations.*
import jobby.spec.*

import com.comcast.ip4s.*

case class InMemoryDB(
    state: Ref[IO, InMemoryDB.State],
    gen: Generator,
    timecop: TimeCop
) extends Database:

  private def opt[T](s: InMemoryDB.State => Option[T]): fs2.Stream[IO, T] =
    fs2.Stream
      .eval(state.get)
      .map(s)
      .flatMap(fs2.Stream.fromOption(_))

  def stream[I, O](query: SqlQuery[I, O]) =
    query match
      case GetCompanyById(companyId) =>
        opt(_.companies.find(c => c.id == companyId))

      case GetJob(jobId) =>
        opt(_.jobs.find(c => c.id == jobId))

      case ListJobs(companyId) =>
        opt(_.jobs.find(c => c.companyId == companyId))

      case DeleteJobById(jobId) =>
        val delete = state.modify { st =>
          val matches = (j: Job) => j.id == jobId

          if st.jobs.exists(matches) then
            st.copy(jobs = st.jobs.filterNot(matches)) -> Seq("ok")
          else st                                      -> Seq.empty
        }

        fs2.Stream.evalSeq(delete)

      case CreateUser(login, hashedPassword) =>
        val insert =
          gen.id(UserId).flatMap { userId =>
            val user = (userId, login, hashedPassword)
            state
              .update(st => st.copy(users = st.users.appended(user)))
              .as(userId)
          }

        fs2.Stream.eval(insert)

      case DeleteCompanyById(companyId, userId) =>
        val delete = state.modify { st =>
          val matches = (company: Company) =>
            company.id == companyId && company.owner_id == userId

          if st.companies.exists(matches) then
            st.copy(
              companies = st.companies.filterNot(matches),
              jobs = st.jobs.filterNot(
                _.companyId == companyId
              ) // simulate cascade delete
            )     -> Seq("ok")
          else st -> Seq.empty
        }

        fs2.Stream.evalSeq(delete)

      case GetCredentials(login) =>
        opt(st => st.users.find(_._2 == login))
          .map { case (id, _, password) =>
            id -> password
          }

      case CreateCompany(userId, attributes) =>
        val insert = gen.id(CompanyId).flatMap { companyId =>
          state
            .update { st =>
              st.copy(companies =
                st.companies
                  .appended(
                    Company(
                      companyId,
                      userId,
                      attributes
                    )
                  )
              )
            }
            .as(companyId)
        }

        fs2.Stream.eval(insert)

      case CreateJob(companyId, attributes, _) =>
        val insert = gen.id(JobId).flatMap { jobId =>
          timecop.timestampNT(JobAdded).flatMap { ja =>
            val job = Job(
              id = jobId,
              companyId = companyId,
              attributes = attributes,
              added = ja
            )

            state.update(st => st.copy(jobs = st.jobs.appended(job))).as(jobId)
          }
        }

        fs2.Stream.eval(insert)

      case LatestJobs =>
        fs2.Stream
          .eval {
            state.get.map(
              _.jobs.sortBy(_.added.value.epochSecond).reverse.take(20)
            )
          }
          .flatMap(fs2.Stream.emits)

      case other =>
        fs2.Stream.raiseError(
          new Exception(s"Operation $other is not implemented in InMemoryDB")
        )
end InMemoryDB

object InMemoryDB:
  import jobby.spec.*
  case class State(
      jobs: Vector[Job] = Vector.empty,
      companies: Vector[Company] = Vector.empty,
      users: Vector[(UserId, UserLogin, HashedPassword)] = Vector.empty
  )

  def create =
    (IO.ref(InMemoryDB.State()), Generator.create, SlowTimeCop.apply)
      .mapN(InMemoryDB.apply)
end InMemoryDB
