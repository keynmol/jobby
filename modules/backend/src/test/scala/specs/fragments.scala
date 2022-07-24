package jobby
package tests

import jobby.spec.*

class Fragments(probe: Probe):
  import probe.*
  def authenticateUser =
    for
      login    <- gen.str(UserLogin, 5 to 50)
      password <- gen.str(UserPassword, 12 to 128)
      _        <- api.users.register(login, password)
      resp     <- api.users.login(login, password)

      refreshToken = resp.cookie
      accessToken  = resp.access_token.value
      authHeader   = AuthHeader(s"Bearer $accessToken")
    yield authHeader

  def createCompany(
      authHeader: AuthHeader,
      attributes: Option[CompanyAttributes] = None
  ) =
    for
      generatedAttributes <- companyAttributes

      companyId <- api.companies
        .createCompany(
          authHeader,
          attributes.getOrElse(generatedAttributes)
        )
        .map(_.id)
    yield companyId

  def companyAttributes =
    for
      companyName        <- gen.str(CompanyName, 3 to 100)
      companyUrl         <- gen.url(CompanyUrl)
      companyDescription <- gen.str(CompanyDescription, 100 to 500)
      attributes = CompanyAttributes(
        companyName,
        companyDescription,
        companyUrl
      )
    yield attributes

  def jobAttributes =
    for
      jobTitle       <- gen.str(JobTitle, 10 to 50)
      jobDescription <- gen.str(JobDescription, 100 to 500)
      jobUrl         <- gen.url(JobUrl)

      minSalary <- gen.int(MinSalary, 1, 100)
      maxSalary <- gen.int(MaxSalary, minSalary.value, 100)
      range = SalaryRange(minSalary, maxSalary, Currency.GBP)

      attributes = JobAttributes(jobTitle, jobDescription, jobUrl, range)
    yield attributes

end Fragments
