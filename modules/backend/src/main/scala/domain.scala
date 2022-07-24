package jobby

class Secret(val plaintext: String):
  override def toString() = "<secret>"

class HashedPassword(val ciphertext: String):
  override def toString()           = "<hashed-password>"
  def process[A](f: String => A): A = f(ciphertext)
