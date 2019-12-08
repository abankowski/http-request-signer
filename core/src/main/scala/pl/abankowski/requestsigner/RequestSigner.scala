package pl.abankowski.requestsigner

import org.bouncycastle.util.encoders.Base64

import scala.language.higherKinds
import pl.abankowski.requestsigner.signature.{Generator, Verifier}


sealed trait RequestSigner[R, F[_]]

object RequestSigner {
  val signatureHeaderName = "Request-Signature"

  val headers: Set[String] = Set(
    "Content-Type",
    "Cookie",
    "Referer"
  )

  val signatureSeparator = '-'
}

sealed trait SignatureVerificationResult
case object SignatureValid extends SignatureVerificationResult
case object SignatureMissing extends SignatureVerificationResult
case object SignatureMalformed extends SignatureVerificationResult
case object SignatureInvalid extends SignatureVerificationResult

abstract class AbstractRequestSigner[R, F[_]](crypto: Generator with Verifier) extends RequestSigner[R, F] {

  def sign(request: R): F[R]
  def verify(request: R): F[SignatureVerificationResult]

  protected def calculateSignature(message: Array[Byte]): String = {
    Base64.toBase64String(crypto.signature(message))
  }

  protected def verifySignature(message: Array[Byte], signature: String): SignatureVerificationResult =
    if (crypto.verify(message, Base64.decode(signature))) {
      SignatureValid
    } else {
      SignatureInvalid
    }
}
