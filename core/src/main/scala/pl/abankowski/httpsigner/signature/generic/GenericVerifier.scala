package pl.abankowski.httpsigner.signature.generic

import java.security.{PrivateKey, Provider, PublicKey}

import pl.abankowski.httpsigner.signature.{GenericVerifier => GenericVerifierImpl}

class GenericVerifier(
  override val algorithm: String,
  override val provider: Provider,
  override val pubKey: PublicKey
) extends GenericVerifierImpl

object GenericVerifier {
  def apply(algorithm: String, provider: Provider, key: PublicKey) = new GenericVerifier(algorithm, provider, key)
}
