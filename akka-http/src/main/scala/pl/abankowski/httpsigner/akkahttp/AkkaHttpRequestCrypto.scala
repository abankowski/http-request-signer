package pl.abankowski.httpsigner.akkahttp

import akka.stream.Materializer
import cats.effect.{ContextShift, IO}
import pl.abankowski.httpsigner.signature.{Generator, Verifier}

import scala.language.{higherKinds, postfixOps}

final class AkkaHttpRequestSigner(override val crypto: Generator)(
  override implicit val mat: Materializer, val ctx: ContextShift[IO])
  extends pl.abankowski.httpsigner.akkahttp.impl.AkkaHttpRequestSigner {
}

final class AkkaHttpRequestVerifier(override val crypto: Verifier)(
  override implicit val mat: Materializer, val ctx: ContextShift[IO])
  extends pl.abankowski.httpsigner.akkahttp.impl.AkkaHttpRequestVerifier {
}

final class AkkaHttpRequestCrypto(override val crypto: Generator with Verifier)(
  override implicit val mat: Materializer, val ctx: ContextShift[IO])
  extends pl.abankowski.httpsigner.akkahttp.impl.AkkaHttpRequestSigner
    with pl.abankowski.httpsigner.akkahttp.impl.AkkaHttpRequestVerifier {
}
