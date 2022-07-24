package jobby

import jobby.spec.*
import cats.effect.*
import java.util.UUID
import jobby.database.{operations as op}
import jobby.spec.CompaniesServiceGen.CreateCompany
import cats.syntax.all.*

import validation.*

class CompaniesServiceImpl(db: Database, httpAuth: HttpAuth)
    extends CompaniesService[IO]:
  override def createCompany(
      auth: AuthHeader,
      attributes: CompanyAttributes
  ): IO[CreateCompanyOutput] =
    httpAuth.access(auth).flatMap { userId =>
      val validation = List(
        validateCompanyName(attributes.name),
        validateCompanyDescription(attributes.description),
        validateCompanyUrl(attributes.url)
      ).traverse(IO.fromEither)

      validation *>
        db.option(
          op.CreateCompany(userId, attributes)
        ).flatMap {
          case None => IO.raiseError(ValidationError("Company already exists"))
          case Some(id) => IO.pure(CreateCompanyOutput(id))
        }
    }
  end createCompany

  override def deleteCompany(auth: AuthHeader, id: CompanyId): IO[Unit] =
    httpAuth.access(auth).flatMap { userId =>
      db.option(op.DeleteCompanyById(id, userId)).flatMap {
        case None     => IO.raiseError(ForbiddenError())
        case Some(id) => IO.unit
      }
    }

  override def getCompany(id: CompanyId): IO[Company] =
    db.option(op.GetCompanyById(id)).flatMap {
      case None    => IO.raiseError(CompanyNotFound())
      case Some(c) => IO.pure(c)
    }

  override def myCompanies(auth: AuthHeader): IO[MyCompaniesOutput] =
    httpAuth.access(auth).flatMap { userId =>
      db.vector(op.ListUserCompanies(userId))
        .map(v => MyCompaniesOutput(v.toList))
    }

  override def getCompanies(ids: List[CompanyId]) =
    ids
      .traverse(id => db.option(op.GetCompanyById(id)))
      .map(_.flatten)
      .map(GetCompaniesOutput.apply)
end CompaniesServiceImpl
