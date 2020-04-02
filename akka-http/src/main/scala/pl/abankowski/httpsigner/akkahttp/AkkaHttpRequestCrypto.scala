package pl.abankowski.httpsigner.akkahttp

import akka.stream.Materializer
import cats.effect.{Async, ContextShift}
import pl.abankowski.httpsigner.signature.{Generator, Verifier}
import pl.abankowski.httpsigner.HttpCryptoConfig

import scala.language.{higherKinds, postfixOps}

final class AkkaHttpRequestSigner[F[_]](
  override val crypto: Generator,
  override val config: HttpCryptoConfig = new HttpCryptoConfig {}
)(override implicit val mat: Materializer,
  val ctx: ContextShift[F],
  val F: Async[F])
    extends pl.abankowski.httpsigner.akkahttp.impl.AkkaHttpRequestSigner[F] {}

final class AkkaHttpRequestVerifier[F[_]](
  override val crypto: Verifier,
  override val config: HttpCryptoConfig = new HttpCryptoConfig {}
)(override implicit val mat: Materializer,
  val ctx: ContextShift[F],
  val F: Async[F])
    extends pl.abankowski.httpsigner.akkahttp.impl.AkkaHttpRequestVerifier[F] {}

final class AkkaHttpRequestCrypto[F[_]](
  override val crypto: Generator with Verifier,
  override val config: HttpCryptoConfig = new HttpCryptoConfig {}
)(override implicit val mat: Materializer,
  val ctx: ContextShift[F],
  val F: Async[F])
    extends pl.abankowski.httpsigner.akkahttp.impl.AkkaHttpRequestSigner[F]
    with pl.abankowski.httpsigner.akkahttp.impl.AkkaHttpRequestVerifier[F] {}
