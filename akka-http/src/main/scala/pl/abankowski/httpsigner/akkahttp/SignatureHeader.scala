package pl.abankowski.httpsigner.akkahttp

import akka.http.scaladsl.model.headers.{ModeledCustomHeader, ModeledCustomHeaderCompanion}
import pl.abankowski.httpsigner.HttpCrypto

import scala.util.Try

final class SignatureHeader(signature: String) extends ModeledCustomHeader[SignatureHeader] {
  override def renderInRequests = true
  override def renderInResponses = true
  override val companion = SignatureHeader
  override def value: String = signature
}

object SignatureHeader extends ModeledCustomHeaderCompanion[SignatureHeader] {
  override val name = HttpCrypto.signatureHeaderName
  override def parse(value: String) = Try(new SignatureHeader(value))
}
