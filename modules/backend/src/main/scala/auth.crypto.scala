package jobby
package users

import jobby.spec.*
import cats.effect.*
import java.security.SecureRandom
import cats.effect.std.Random
import java.security.MessageDigest

object Crypto:
  def hashPassword(raw: UserPassword): IO[HashedPassword] =
    Random.javaSecuritySecureRandom[IO].flatMap { r =>
      for
        seed <- r.nextString(16)
        seeded = seed + ":" + raw.value
        digest = sha256(seeded)
      yield HashedPassword(seed + ":" + digest)
    }

  def sha256(s: String): String =
    val hash = MessageDigest.getInstance("SHA-256")
    hash.update(s.getBytes)
    bytesToHex(hash.digest)

  private def bytesToHex(bytes: Array[Byte]): String =
    val sb = StringBuilder()
    bytes.foreach { b =>
      sb.append(String.format("%02x", b))
    }
    sb.result
end Crypto
