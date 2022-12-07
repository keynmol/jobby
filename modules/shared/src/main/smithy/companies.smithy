$version: "2.0"

metadata suppressions = [
    {
        id: "HttpHeaderTrait",
        namespace: "jobby.spec",
        reason: "I totally know what I'm doing"
    }
]

namespace jobby.spec

use alloy#simpleRestJson
use alloy#uuidFormat

// SERVICES

@simpleRestJson
@httpBearerAuth
service CompaniesService {
  version: "1.0.0",
  operations: [
    CreateCompany, 
    DeleteCompany, 
    GetCompany, 
    MyCompanies, 
    GetCompanies
  ]
}

// OPERATIONS
@idempotent
@http(method: "POST", uri: "/api/companies/", code: 200)
operation GetCompanies {
  input: GetCompaniesInput,
  output: GetCompaniesOutput,
  errors: [CompanyNotFound]
}


@readonly
@http(method: "GET", uri: "/api/companies/{id}", code: 200)
operation GetCompany {
  input: GetCompanyInput,
  output: Company,
  errors: [CompanyNotFound]
}

@idempotent
@http(method: "PUT", uri: "/api/companies", code: 200)
operation CreateCompany {
  input: CreateCompanyInput,
  output: CreateCompanyOutput,
  errors: [ValidationError]
}

@readonly
@http(method: "GET", uri: "/api/my_companies", code: 200)
operation MyCompanies {
  input: MyCompaniesInput,
  output: MyCompaniesOutput,
  errors: [ValidationError, UnauthorizedError]
}

@idempotent
@http(method: "DELETE", uri: "/api/companies/{id}", code: 204)
operation DeleteCompany {
  input: DeleteCompanyInput,
  errors: [UnauthorizedError, ForbiddenError]
}

// STRUCTURES

structure GetCompaniesInput {
  @required
  ids: CompanyIdList
}

list CompanyIdList {
  member: CompanyId
}

structure GetCompaniesOutput {
  @required
  companies: CompaniesList
}

structure MyCompaniesInput {
  @httpHeader("Authorization")
  @required
  auth: AuthHeader, 
}

structure MyCompaniesOutput {
  @required
  companies: CompaniesList
}

list CompaniesList {
  member: Company
}

structure DeleteCompanyInput {
  @httpHeader("Authorization")
  @required
  auth: AuthHeader, 
  
  @httpLabel
  @required 
  id: CompanyId
}

@error("client")
@httpError(404)
structure CompanyNotFound {}

structure GetCompanyInput {
  @httpLabel
  @required 
  id: CompanyId
}

structure CreateCompanyOutput {
  @httpLabel
  @required 
  id: CompanyId
}

structure CreateCompanyInput {
  @httpHeader("Authorization")
  @required
  auth: AuthHeader, 

  @required 
  attributes: CompanyAttributes
}

@uuidFormat
string CompanyId
string CompanyUrl
string CompanyName
string CompanyDescription

structure Company {
  @required
  id: CompanyId,

  @required
  owner_id: UserId,

  @required 
  attributes: CompanyAttributes
}

structure CompanyAttributes {
  @required
  name: CompanyName,

  @required 
  description: CompanyDescription,

  @required 
  url: CompanyUrl
}
