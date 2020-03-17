package pl.abankowski.httpsigner.akkahttp

import akka.stream.Materializer
import cats.effect.{ContextShift, IO}
import pl.abankowski.httpsigner.signature.{Generator, Verifier}

import scala.language.{higherKinds, postfixOps}

final class AkkaHttpResponseSigner(override val crypto: Generator)(
  override implicit val mat: Materializer, val ctx: ContextShift[IO])
  extends pl.abankowski.httpsigner.akkahttp.impl.AkkaHttpResponseSigner {
}

final class AkkaHttpResponseVerifier(override val crypto: Verifier)(
  override implicit val mat: Materializer, val ctx: ContextShift[IO])
  extends pl.abankowski.httpsigner.akkahttp.impl.AkkaHttpResponseVerifier {
}

final class AkkaHttpResponseCrypto(override val crypto: Generator with Verifier)(
  override implicit val mat: Materializer, val ctx: ContextShift[IO])
  extends pl.abankowski.httpsigner.akkahttp.impl.AkkaHttpResponseSigner
    with pl.abankowski.httpsigner.akkahttp.impl.AkkaHttpResponseVerifier {
}
