$version: "2.0"

namespace jobby.spec

use alloy#simpleRestJson
use alloy#uuidFormat

@simpleRestJson
service JobService {
  version: "1.0.0",
  operations: [GetJob, CreateJob, ListJobs, LatestJobs, DeleteJob]
}

@readonly
@http(method: "GET", uri: "/api/jobs/{id}", code: 200)
operation GetJob {
  input: GetJobInput,
  output: Job,
  errors: [JobNotFound]
}

@idempotent
@http(method: "DELETE", uri: "/api/jobs/{id}", code: 200)
operation DeleteJob {
  input: DeleteJobInput,
  errors: [JobNotFound, ForbiddenError, UnauthorizedError]
}

@idempotent
@http(method: "PUT", uri: "/api/jobs", code: 200)
operation CreateJob {
  input: CreateJobInput,
  output: CreateJobOutput,
  errors: [ValidationError, UnauthorizedError, ForbiddenError]
}

@readonly
@http(method: "GET", uri: "/api/jobs", code: 200)
operation ListJobs {
  input: ListJobsInput,
  output: ListJobsOutput,
  errors: [ValidationError, UnauthorizedError]
}

@readonly
@http(method: "GET", uri: "/api/latest_jobs", code: 200)
operation LatestJobs {
  output: LatestJobsOutput
}

structure LatestJobsOutput {
  @required
  jobs: Jobs
}

@error("client")
@httpError(404)
structure JobNotFound {}

structure ListJobsOutput {
  @required
  jobs: Jobs
}

list Jobs {
  member: Job
}

structure CreateJobOutput {
  @required 
  id: JobId
}

structure JobAttributes { 
  @required
  title: JobTitle,

  @required 
  description: JobDescription,

  @required 
  url: JobUrl,

  @required
  range: SalaryRange
}

structure CreateJobInput { 
  @httpHeader("Authorization")
  @required 
  auth: AuthHeader,

  @required
  companyId: CompanyId,

  @required 
  attributes: JobAttributes
}

structure ListJobsInput {
  @httpQuery("company_id")
  @required
  company_id: CompanyId 
}

structure GetJobInput {
  @httpLabel 
  @required 
  id: JobId
}

structure DeleteJobInput {
  @httpHeader("Authorization")
  @required 
  auth: AuthHeader,

  @httpLabel 
  @required 
  id: JobId
}

@uuidFormat
string JobId
string JobTitle
string JobDescription
string JobUrl

structure Job {
  @required 
  id: JobId,

  @required 
  companyId: CompanyId,

  @required 
  attributes: JobAttributes,

  @required
  added: JobAdded
}

timestamp JobAdded

integer MinSalary
integer MaxSalary

structure SalaryRange {
  @required 
  min: MinSalary,

  @required
  max: MaxSalary,

  @required 
  currency: Currency
}

@enum([{value: "USD", name: "USD"}, {value: "GBP", name: "GBP"}, {value: "EUR", name: "EUR"}])
string Currency
