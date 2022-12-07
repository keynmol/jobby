$version: "2.0"

namespace jobby.spec

use alloy#simpleRestJson
use alloy#uuidFormat

@simpleRestJson
service UserService {
  version: "1.0.0",
  operations: [Login, Register, Refresh]
}

@http(method: "POST", uri: "/api/users/login", code: 200)
operation Login {
  input: LoginInput,
  output: Tokens,
  errors: [CredentialsError]
}

@idempotent
@http(method: "PUT", uri: "/api/users/register", code: 204)
operation Register {
  input: RegisterInput,
  errors: [ValidationError]
}

@http(method: "POST", uri: "/api/users/refresh", code: 200)
operation Refresh {
  input: RefreshInput,
  output: RefreshOutput,
  errors: [CredentialsError, UnauthorizedError]
}

structure RefreshInput {
  @httpHeader("Cookie")
  refreshToken: RefreshToken,

  @httpQuery("logout")
  logout: Boolean
}

structure RefreshOutput {
  access_token: AccessToken,

  @httpHeader("Set-Cookie")
  logout: Cookie,
  
  @required
  expires_in: TokenExpiration
}

structure RegisterInput {
  @required 
  login: UserLogin,

  @required 
  password: UserPassword
}

structure LoginInput {
  @required 
  login: UserLogin,

  @required 
  password: UserPassword
}

structure Tokens {
  @required
  access_token: AccessToken,

  @httpHeader("Set-Cookie")
  cookie: Cookie,
  expires_in: TokenExpiration
}

@uuidFormat
string UserId
string UserLogin
string UserPassword

string AccessToken
string RefreshToken
string Cookie
integer TokenExpiration

@error("client")
@httpError(400)
structure CredentialsError {
  @required
  message: String
}
