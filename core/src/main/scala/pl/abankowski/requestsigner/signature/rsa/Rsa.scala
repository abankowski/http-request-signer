package pl.abankowski.requestsigner.signature.rsa

import org.bouncycastle.crypto.params.AsymmetricKeyParameter
import org.bouncycastle.crypto.AsymmetricCipherKeyPair
import pl.abankowski.requestsigner.signature.{RsaSHA512Generator, RsaSHA512Verifier}

class Rsa(
  override val privKey: AsymmetricKeyParameter,
  override val pubKey: AsymmetricKeyParameter
) extends RsaSHA512Verifier with RsaSHA512Generator

object Rsa {
  def apply(priv: AsymmetricKeyParameter, pub: AsymmetricKeyParameter) = new Rsa(priv, pub)
  def apply(kp: AsymmetricCipherKeyPair) = new Rsa(kp.getPrivate, kp.getPublic)
}