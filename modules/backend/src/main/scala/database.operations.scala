package jobby
package database
package operations

import skunk.Session
import skunk.PreparedCommand
import cats.effect.IO
import skunk.PreparedQuery

import skunk.*
import skunk.implicits.*

import codecs.*
import skunk.codec.all.*

import jobby.spec.*
import cats.effect.kernel.Resource

sealed abstract class SqlQuery[I, O](val input: I, query: skunk.Query[I, O]):
  def use[A](session: Session[IO])(f: PreparedQuery[IO, I, O] => IO[A]) =
    session.prepareR(query).use(f)

  def prepare(session: Session[IO]): Resource[IO, PreparedQuery[IO, I, O]] =
    session.prepareR(query)
end SqlQuery

// user ops

case class GetCredentials(login: UserLogin)
    extends SqlQuery(
      login,
      sql"""
        select user_id, salted_hash 
        from users where lower(login) = lower($userLogin)
      """.query(userId ~ hashedPassword)
    )

case class CreateUser(login: UserLogin, password: HashedPassword)
    extends SqlQuery(
      login -> password,
      sql"""
        insert into 
          users (user_id, login, salted_hash) 
          values (gen_random_uuid(), lower($userLogin), $hashedPassword)
        returning user_id
      """.query(userId)
    )

// company ops

case class GetCompanyById(login: CompanyId)
    extends SqlQuery(
      login,
      sql"""
        select company_id, owner_id, name, description, url from companies 
        where company_id = $companyId
      """.query(company)
    )

case class DeleteCompanyById(company: CompanyId, user: UserId)
    extends SqlQuery(
      company -> user,
      sql"""
        delete from companies where company_id = $companyId and owner_id = $userId
        returning 'ok'::varchar
      """.query(varchar)
    )

case class DeleteJobById(id: JobId)
    extends SqlQuery(
      id,
      sql"""
        delete from jobs where job_id = $jobId
        returning 'ok'::varchar
      """.query(varchar)
    )

case class CreateCompany(userId: UserId, attributes: CompanyAttributes)
    extends SqlQuery(
      userId -> attributes,
      sql"""
        insert into 
          companies(company_id, owner_id, name, description, url) 
        values(gen_random_uuid(), ${codecs.userId}, $companyAttributes)
        on conflict do nothing
        returning company_id
      """.query(companyId)
    )
end CreateCompany

case class ListUserCompanies(user: UserId)
    extends SqlQuery(
      user,
      sql"""
        select company_id, owner_id, name, description, url from companies
        where owner_id = $userId
      """.query(company)
    )

// // job ops

case class GetJob(id: JobId)
    extends SqlQuery(
      id,
      sql"""
        select 
          job_id, company_id, job_title, job_description, job_url, min_salary, max_salary, currency, added 
        from jobs 
        where 
          job_id = $jobId
      """.query(job)
    )

case class CreateJob(
    company: CompanyId,
    attributes: JobAttributes,
    jobAdded: JobAdded
) extends SqlQuery(
      ((company, attributes), jobAdded),
      sql"""
        insert into jobs(job_id, company_id, job_title, job_description, job_url, min_salary, max_salary, currency, added)
        values          (gen_random_uuid(), $companyId, $jobAttributes, $added)
        returning job_id
      """.query(jobId)
    )

case object LatestJobs
    extends SqlQuery(
      skunk.Void,
      sql"""
      select 
        job_id, 
        company_id,
        job_title, 
        job_description,
        job_url,
        min_salary,
        max_salary,
        currency,
        added
      from jobs order by added desc limit 20
  """.query(job)
    )

case class ListJobs(company: CompanyId)
    extends SqlQuery(
      company,
      sql"""
      select 
        job_id, 
        company_id,
        job_title, 
        job_description,
        job_url,
        min_salary,
        max_salary,
        currency,
        added
      from jobs where company_id = $companyId
  """.query(job)
    )
