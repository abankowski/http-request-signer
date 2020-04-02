package pl.abankowski.httpsigner.akkahttp

import akka.stream.Materializer
import cats.effect.{Async, ContextShift}
import pl.abankowski.httpsigner.signature.{Generator, Verifier}
import pl.abankowski.httpsigner.HttpCryptoConfig

import scala.language.{higherKinds, postfixOps}

final class AkkaHttpResponseSigner[F[_]](
  override val crypto: Generator,
  override val config: HttpCryptoConfig = new HttpCryptoConfig {}
)(override implicit val mat: Materializer, val ctx: ContextShift[F], val F: Async[F])
    extends pl.abankowski.httpsigner.akkahttp.impl.AkkaHttpResponseSigner[F] {}

final class AkkaHttpResponseVerifier[F[_]](
  override val crypto: Verifier,
  override val config: HttpCryptoConfig = new HttpCryptoConfig {}
)(override implicit val mat: Materializer, val ctx: ContextShift[F], val F: Async[F])
    extends pl.abankowski.httpsigner.akkahttp.impl.AkkaHttpResponseVerifier[F] {}

final class AkkaHttpResponseCrypto[F[_]](
  override val crypto: Generator with Verifier,
  override val config: HttpCryptoConfig = new HttpCryptoConfig {}
)(override implicit val mat: Materializer, val ctx: ContextShift[F], val F: Async[F])
    extends pl.abankowski.httpsigner.akkahttp.impl.AkkaHttpResponseSigner[F]
    with pl.abankowski.httpsigner.akkahttp.impl.AkkaHttpResponseVerifier[F] {}
