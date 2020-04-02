package pl.abankowski.httpsigner.signature.generic

import java.security.{PrivateKey, Provider}

import pl.abankowski.httpsigner.signature.{GenericGenerator => GenericGeneratorImpl}

class GenericGenerator(
  override val algorithm: String,
  override val provider: Provider,
  override val privKey: PrivateKey
) extends GenericGeneratorImpl

object GenericGenerator {
  def apply(algorithm: String, provider: Provider, key: PrivateKey) = new GenericGenerator(algorithm, provider, key)
}
