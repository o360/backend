package silhouette

import com.mohiva.play.silhouette.api.crypto.Crypter

/**
  * Dummy crypter.
  */
class DummyCrypter extends Crypter {
  override def encrypt(value: String): String = value

  override def decrypt(value: String): String = value
}
