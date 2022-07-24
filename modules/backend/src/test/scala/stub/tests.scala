package jobby
package tests
package stub

import weaver.*

class UsersTests(global: GlobalRead)
    extends StubSuite(global)
    with jobby.tests.UsersSuite

class CompaniesTests(global: GlobalRead)
    extends StubSuite(global)
    with jobby.tests.CompaniesSuite

class JobsTests(global: GlobalRead)
    extends StubSuite(global)
    with jobby.tests.JobsSuite
