package jobby
package tests

import jobby.spec.*
import cats.effect.IO
import cats.syntax.all.*

trait JobsSuite:
  self: JobbySuite =>

  probeTest("Creating jobs by authenticated company owner") { probe =>
    import probe.*
    for
      companyOwner <- fragments.authenticateUser
      randomUser   <- fragments.authenticateUser
      companyId    <- fragments.createCompany(companyOwner)
      companyId2   <- fragments.createCompany(randomUser)

      attributes <- fragments.jobAttributes

      create = (authHeader: AuthHeader, companyId: CompanyId) =>
        api.jobs.createJob(
          authHeader,
          companyId,
          attributes
        )

      byCompanyOwner <- create(companyOwner, companyId)
      byRando        <- create(randomUser, companyId).attempt
      wrongCompany   <- create(companyOwner, companyId2).attempt
      withoutAuth    <- create(AuthHeader("Bearer bla"), companyId).attempt
    yield expect.all(
      byRando == Left(ForbiddenError()),
      wrongCompany == Left(ForbiddenError()),
      withoutAuth == Left(UnauthorizedError())
    )
    end for
  }

  probeTest("Deleting jobs by authenticated company owner") { probe =>
    import probe.*
    for
      bumBook_Owner <- fragments.authenticateUser
      Poodle_Owner  <- fragments.authenticateUser
      BumBook_Inc   <- fragments.createCompany(bumBook_Owner)
      Poodle_Inc    <- fragments.createCompany(Poodle_Owner)

      attributes <- fragments.jobAttributes

      createJob = (authHeader: AuthHeader, companyId: CompanyId) =>
        api.jobs.createJob(
          authHeader,
          companyId,
          attributes
        )

      bumBook_Job1 <- createJob(bumBook_Owner, BumBook_Inc)
      bumBook_Job2 <- createJob(bumBook_Owner, BumBook_Inc)
      bumBook_Job3 <- createJob(bumBook_Owner, BumBook_Inc)

      Poodl_Job1 <- createJob(Poodle_Owner, Poodle_Inc)
      Poodl_Job2 <- createJob(Poodle_Owner, Poodle_Inc)

      _ <- api.jobs.deleteJob(bumBook_Owner, bumBook_Job1.id)
      _ <- api.jobs.deleteJob(Poodle_Owner, Poodl_Job1.id)

      nope <- api.jobs.deleteJob(bumBook_Owner, Poodl_Job2.id).attempt

      nope2 <- api.jobs.deleteJob(Poodle_Owner, bumBook_Job2.id).attempt

      nope3 <- api.jobs
        .deleteJob(AuthHeader("Bearer bla"), bumBook_Job3.id)
        .attempt
    yield expect.all(
      nope == Left(ForbiddenError()),
      nope2 == Left(ForbiddenError()),
      nope3 == Left(UnauthorizedError())
    )
    end for
  }

  probeTest("Listing latest jobs") { probe =>
    import probe.*
    for
      companyOwner <- fragments.authenticateUser
      companyId    <- fragments.createCompany(companyOwner)

      attributes <- fragments.jobAttributes

      createJob = api.jobs.createJob(
        companyOwner,
        companyId,
        attributes
      )
      _ <- createJob.replicateA_(probe.config.misc.latestJobs * 2)

      jobs <- api.jobs.latestJobs().map(_.jobs)
      sorted = jobs.sortBy(_.added.value.epochSecond).reverse
    yield expect.all(
      jobs.length == config.misc.latestJobs,
      sorted == jobs
    )
    end for
  }
end JobsSuite
