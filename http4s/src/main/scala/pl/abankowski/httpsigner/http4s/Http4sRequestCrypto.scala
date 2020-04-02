package pl.abankowski.httpsigner.http4s

import cats.effect.{ContextShift, Sync}
import pl.abankowski.httpsigner.signature.{Generator, Verifier}
import pl.abankowski.httpsigner.HttpCryptoConfig

class Http4sRequestSigner[F[_]](
  override val crypto: Generator,
  override val config: HttpCryptoConfig = new HttpCryptoConfig {}
)(override implicit val ctx: ContextShift[F], override implicit val F: Sync[F])
    extends pl.abankowski.httpsigner.http4s.impl.Http4sRequestSigner[F]

class Http4sRequestVerifier[F[_]](
  override val crypto: Verifier,
  override val config: HttpCryptoConfig = new HttpCryptoConfig {}
)(override implicit val ctx: ContextShift[F], override implicit val F: Sync[F])
    extends pl.abankowski.httpsigner.http4s.impl.Http4sRequestVerifier[F]

class Http4sRequestCrypto[F[_]](
  override val crypto: Generator with Verifier,
  override val config: HttpCryptoConfig = new HttpCryptoConfig {}
)(override implicit val ctx: ContextShift[F], override implicit val F: Sync[F])
    extends pl.abankowski.httpsigner.http4s.impl.Http4sRequestSigner[F]
    with pl.abankowski.httpsigner.http4s.impl.Http4sRequestVerifier[F]
