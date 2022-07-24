package jobby

import jobby.spec.*
import cats.effect.*
import cats.implicits.*
import java.util.UUID

import database.operations as op
import smithy4s.Timestamp

import validation.*

class JobServiceImpl(db: Database, auth: HttpAuth, timeCop: TimeCop)
    extends JobService[IO]:
  override def getJob(id: JobId): IO[Job] =
    db.option(op.GetJob(id)).flatMap {
      case None    => IO.raiseError(JobNotFound())
      case Some(j) => IO.pure(j)
    }

  override def createJob(
      authHeader: AuthHeader,
      companyId: CompanyId,
      attributes: JobAttributes
  ): IO[CreateJobOutput] =
    for
      userId        <- auth.access(authHeader)
      companyLookup <- db.option(op.GetCompanyById(companyId))
      company <- IO.fromOption(companyLookup)(
        ValidationError("company not found")
      )

      _ <- IO.raiseUnless(company.owner_id == userId)(ForbiddenError())

      _ <- List(
        validateJobTitle(attributes.title),
        validateJobDescription(attributes.description),
        validateJobUrl(attributes.url),
        validateSalaryRange(attributes.range)
      ).traverse(IO.fromEither)

      added <- timeCop.timestampNT(JobAdded)

      createdJob <-
        db.option(
          op.CreateJob(
            companyId,
            attributes,
            added
          )
        )
      jobId <- IO.fromOption(createdJob)(
        ValidationError("well you *must have* done something wrong")
      )
    yield CreateJobOutput(jobId)
    end for
  end createJob

  override def latestJobs() =
    db.vector(op.LatestJobs)
      .map(_.toList)
      .map(LatestJobsOutput.apply)

  override def listJobs(companyId: CompanyId) =
    db.vector(op.ListJobs(companyId)).map(_.toList).map(ListJobsOutput.apply)

  override def deleteJob(authHeader: AuthHeader, id: JobId): IO[Unit] =
    for
      userId        <- auth.access(authHeader)
      job           <- getJob(id)
      companyLookup <- db.option(op.GetCompanyById(job.companyId))
      company <- IO.fromOption(companyLookup)(
        ValidationError("company not found")
      )
      _ <- IO.raiseUnless(company.owner_id == userId)(ForbiddenError())
      _ <- db.option(op.DeleteJobById(id))
    yield ()
end JobServiceImpl
