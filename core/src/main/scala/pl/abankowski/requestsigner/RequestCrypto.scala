package pl.abankowski.requestsigner

import org.bouncycastle.util.encoders.Base64

import scala.language.higherKinds
import pl.abankowski.requestsigner.signature.{Generator, Verifier}


object RequestCrypto {
  val signatureHeaderName = "Request-Signature"

  val headers: Set[String] = Set(
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

trait RequestSigner[R, F[_]] {
  val crypto: Generator
  def sign(request: R): F[R]

  protected def calculateSignature(message: Array[Byte]): String = Base64.toBase64String(crypto.signature(message))
}

trait RequestVerifier[R, F[_]] {
  val crypto: Verifier
  def verify(request: R): F[SignatureVerificationResult]

  protected def verifySignature(message: Array[Byte], signature: String): SignatureVerificationResult =
    if (crypto.verify(message, Base64.decode(signature))) {
      SignatureValid
    } else {
      SignatureInvalid
    }
}