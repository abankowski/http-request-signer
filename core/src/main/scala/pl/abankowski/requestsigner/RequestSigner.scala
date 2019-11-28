package pl.abankowski.requestsigner

import java.io.ByteArrayOutputStream
import java.math.BigInteger

import scala.language.higherKinds
import org.bouncycastle.crypto.signers.DSASigner
import org.bouncycastle.crypto.params.DSAParameters
import org.bouncycastle.util.encoders.Base64


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

abstract class AbstractRequestSigner[R, F[_]](cipher: DSAParameters) extends RequestSigner[R, F] {

  def sign(request: R): F[R]
  def verify(request: R): F[SignatureVerificationResult]

  private val signer = new DSASigner()
  signer.init(true, cipher)


  protected def calculateSignature(message: Array[Byte]) : String = {

    val s = signer.generateSignature(message)

    assert(s.size==2, "Signature must be two BigInteger size by underlying design")

    s"${Base64.toBase64String(s(0).toByteArray)}${RequestSigner.signatureSeparator}${Base64.toBase64String(s(1).toByteArray)}"
  }

  protected def verifySignature(message: Array[Byte], signature: String): SignatureVerificationResult = {

    val signatureParts = signature.split(RequestSigner.signatureSeparator)

    if (signatureParts.length == 2) {

      if (signer.verifySignature(
        message,
        new BigInteger(Base64.decode(signatureParts(0))),
        new BigInteger(Base64.decode(signatureParts(1))))) {
        SignatureInvalid
      } else {
        SignatureValid
      }

    } else {
      SignatureMalformed
    }
  }
}
