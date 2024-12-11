package jobby
package database

import jobby.spec.*
import skunk.Codec
import skunk.codec.all.*
import skunk.data.Type
import smithy4s.{Newtype, Timestamp}

object codecs:
  extension [T](c: Codec[T])
    private[database] def as(obj: Newtype[T]): Codec[obj.Type] =
      c.imap(obj.apply(_))(_.value)

  val userId: Codec[UserId]       = uuid.as(UserId)
  val userLogin: Codec[UserLogin] = varchar(50).as(UserLogin)

  val companyId          = uuid.as(CompanyId)
  val hashedPassword     = bpchar(81).imap(HashedPassword(_))(_.ciphertext)
  val companyName        = varchar(128).as(CompanyName)
  val companyDescription = text.as(CompanyDescription)
  val companyUrl         = varchar(512).as(CompanyUrl)
  val minSalary          = int4.as(MinSalary)
  val maxSalary          = int4.as(MaxSalary)
  val jobId              = uuid.as(JobId)
  val jobTitle           = varchar(256).as(JobTitle)
  val jobDescription     = text.as(JobDescription)
  val jobUrl             = varchar(512).as(JobUrl)
  val currency =
    `enum`[Currency](_.value, Currency.fromString, Type("currency_enum"))

  val salaryRange = (minSalary *: maxSalary *: currency).to[SalaryRange]

  val added = timestamptz
    .imap(Timestamp.fromOffsetDateTime)(_.toOffsetDateTime)
    .as(JobAdded)

  val jobAttributes =
    (jobTitle *:
      jobDescription *:
      jobUrl *:
      salaryRange).to[JobAttributes]

  val job =
    (jobId *:
      companyId *:
      jobAttributes *:
      added).to[Job]

  val companyAttributes =
    (companyName *:
      companyDescription *:
      companyUrl).to[CompanyAttributes]

  val company =
    (companyId *:
      userId *:
      companyAttributes).to[Company]
end codecs
