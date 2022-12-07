package jobby
package tests
package integration

import weaver.*

class UsersTests(global: GlobalRead)
    extends IntegrationSuite(global)
    with jobby.tests.UsersSuite

class CompaniesTests(global: GlobalRead)
    extends IntegrationSuite(global)
    with jobby.tests.CompaniesSuite

class JobsTests(global: GlobalRead)
    extends IntegrationSuite(global)
    with jobby.tests.JobsSuite

class HealthTests(global: GlobalRead)
    extends IntegrationSuite(global)
    with jobby.tests.HealthSuite
