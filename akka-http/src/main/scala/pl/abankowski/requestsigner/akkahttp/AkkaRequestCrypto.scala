package pl.abankowski.requestsigner.akkahttp

import akka.http.scaladsl.model.headers.{ModeledCustomHeader, ModeledCustomHeaderCompanion}
import akka.stream.Materializer
import cats.effect.{ContextShift, IO}
import pl.abankowski.requestsigner.signature.{Generator, Verifier}
import pl.abankowski.requestsigner.RequestCrypto

import scala.language.{higherKinds, postfixOps}
import scala.util.Try

final class SignatureHeader(signature: String) extends ModeledCustomHeader[SignatureHeader] {
  override def renderInRequests = true
  override def renderInResponses = true
  override val companion = SignatureHeader
  override def value: String = signature
}

object SignatureHeader extends ModeledCustomHeaderCompanion[SignatureHeader] {
  override val name = RequestCrypto.signatureHeaderName
  override def parse(value: String) = Try(new SignatureHeader(value))
}

final class AkkaRequestCrypto(override val crypto: Generator with Verifier)(
  override implicit val mat: Materializer, val ctx: ContextShift[IO])
    extends pl.abankowski.requestsigner.akkahttp.impl.AkkaRequestSigner
      with pl.abankowski.requestsigner.akkahttp.impl.AkkaRequestVerifier {
}

final class AkkaRequestSigner(override val crypto: Generator)(
  override implicit val mat: Materializer, val ctx: ContextShift[IO])
  extends pl.abankowski.requestsigner.akkahttp.impl.AkkaRequestSigner {
}

final class AkkaRequestVerifier(override val crypto: Verifier)(
  override implicit val mat: Materializer, val ctx: ContextShift[IO])
  extends pl.abankowski.requestsigner.akkahttp.impl.AkkaRequestVerifier {
}

