package jobby
package tests

import jobby.spec.*
import cats.effect.IO

trait CompaniesSuite:
  self: JobbySuite =>

  test("Creation by authenticated user") { probe =>
    import probe.*
    for
      authHeader <- fragments.authenticateUser
      userId     <- auth.access(authHeader)
      attributes <- fragments.companyAttributes

      companyId <- api.companies
        .createCompany(
          authHeader,
          attributes
        )
        .map(_.id)

      retrieved <- api.companies.getCompany(companyId)
    yield expect.all(
      attributes.name == retrieved.attributes.name,
      attributes.url == retrieved.attributes.url,
      attributes.description == retrieved.attributes.description,
      userId == retrieved.owner_id
    )
    end for
  }

  test("Deletion by the owner") { probe =>
    import probe.*
    for
      ownerAuth  <- fragments.authenticateUser
      rando      <- fragments.authenticateUser
      attributes <- fragments.companyAttributes

      company <- api.companies.createCompany(
        ownerAuth,
        attributes
      )

      byRando <- api.companies.deleteCompany(rando, company.id).attempt

      _ <- expect(byRando.isLeft).failFast

      jobAttributes <- fragments.jobAttributes

      _ <- api.jobs.createJob(ownerAuth, company.id, jobAttributes)

      jobsBeforeDeletion <- api.jobs.listJobs(company.id)

      _                 <- api.companies.deleteCompany(ownerAuth, company.id)
      afterDeletion     <- api.companies.getCompany(company.id).attempt
      jobsAfterDeletion <- api.jobs.listJobs(company.id)
    yield expect.all(
      afterDeletion == Left(CompanyNotFound()),
      jobsBeforeDeletion.jobs.nonEmpty,
      jobsAfterDeletion.jobs.isEmpty
    )
    end for
  }
end CompaniesSuite
