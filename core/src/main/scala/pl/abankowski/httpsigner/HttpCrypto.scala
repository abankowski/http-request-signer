package pl.abankowski.httpsigner

import org.bouncycastle.util.encoders.Base64

import scala.language.higherKinds
import pl.abankowski.httpsigner.signature.{Generator, Verifier}

trait HttpCryptoConfig {
  final val signatureHeaderName = "Request-Signature"

  val protectedHeaders: Set[String] = Set(
    "Content-Type",
    "Cookie",
    "Referer"
  )
}

sealed trait SignatureVerificationResult
case object SignatureValid extends SignatureVerificationResult
case object SignatureMissing extends SignatureVerificationResult
case object SignatureMalformed extends SignatureVerificationResult
case object SignatureInvalid extends SignatureVerificationResult

trait HttpSigner[M, F[_]] {
  val crypto: Generator
  def sign(messasge: M): F[M]

  protected def calculateSignature(message: Array[Byte]): String = Base64.toBase64String(crypto.signature(message))
}

trait HttpVerifier[M, F[_]] {
  val crypto: Verifier
  def verify(message: M): F[SignatureVerificationResult]

  protected def verifySignature(message: Array[Byte], signature: String): SignatureVerificationResult =
    if (crypto.verify(message, Base64.decode(signature))) {
      SignatureValid
    } else {
      SignatureInvalid
    }
}
