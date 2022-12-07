$version: "2.0"

namespace jobby.spec

use alloy#simpleRestJson

@simpleRestJson
service HealthService {
  version: "1.0.0",
  operations: [HealthCheck]
}

@http(method: "GET", uri: "/api/health", code: 200)
operation HealthCheck {
  output := {
    service: String
  }
}



