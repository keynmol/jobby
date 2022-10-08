$version: "2.0"

namespace jobby.spec

@error("client")
@httpError(400)
structure ValidationError {
  @required
  message: String
}

@error("client")
@httpError(401)
structure UnauthorizedError {
  message: String
}

@error("client")
@httpError(403)
structure ForbiddenError {}

string AuthHeader
