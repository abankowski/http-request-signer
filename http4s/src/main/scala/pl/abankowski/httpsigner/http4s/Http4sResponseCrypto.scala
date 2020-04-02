package pl.abankowski.httpsigner.http4s

import cats.effect.{ContextShift, Sync}
import pl.abankowski.httpsigner.signature.{Generator, Verifier}
import pl.abankowski.httpsigner.HttpCryptoConfig

class Http4sResponseSigner[F[_]](
  override val crypto: Generator,
  override val config: HttpCryptoConfig = new HttpCryptoConfig {}
)(override implicit val ctx: ContextShift[F], override implicit val F: Sync[F])
    extends pl.abankowski.httpsigner.http4s.impl.Http4sResponseSigner[F]

class Http4sResponseVerifier[F[_]](
  override val crypto: Verifier,
  override val config: HttpCryptoConfig = new HttpCryptoConfig {}
)(override implicit val ctx: ContextShift[F], override implicit val F: Sync[F])
    extends pl.abankowski.httpsigner.http4s.impl.Http4sResponseVerifier[F]

class Http4SResponseCrypto[F[_]](
  override val crypto: Generator with Verifier,
  override val config: HttpCryptoConfig = new HttpCryptoConfig {}
)(override implicit val ctx: ContextShift[F], override implicit val F: Sync[F])
    extends pl.abankowski.httpsigner.http4s.impl.Http4sResponseSigner[F]
    with pl.abankowski.httpsigner.http4s.impl.Http4sResponseVerifier[F]
