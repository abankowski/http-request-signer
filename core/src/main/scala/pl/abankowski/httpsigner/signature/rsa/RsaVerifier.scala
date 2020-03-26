package pl.abankowski.httpsigner.signature.rsa

import org.bouncycastle.crypto.params.AsymmetricKeyParameter
import pl.abankowski.httpsigner.signature.RsaSHA512Verifier

final class RsaVerifier(override val pubKey: AsymmetricKeyParameter) extends RsaSHA512Verifier

object RsaVerifier {
  def apply(pub: AsymmetricKeyParameter) = new RsaVerifier(pub)
}
