package pl.abankowski.httpsigner.http4s

import cats.effect.Async
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import pl.abankowski.httpsigner.signature.{Generator, Verifier}
import pl.abankowski.httpsigner.HttpCryptoConfig

class Http4sResponseSigner[F[_]](
  override val crypto: Generator,
  override val config: HttpCryptoConfig = new HttpCryptoConfig {}
)(override implicit val F: Async[F])
    extends pl.abankowski.httpsigner.http4s.impl.Http4sResponseSigner[F] {
  override val logger: Logger[F] = Slf4jLogger.getLogger[F]
}

class Http4sResponseVerifier[F[_]](
  override val crypto: Verifier,
  override val config: HttpCryptoConfig = new HttpCryptoConfig {}
)(override implicit val F: Async[F])
    extends pl.abankowski.httpsigner.http4s.impl.Http4sResponseVerifier[F] {
  override val logger: Logger[F] = Slf4jLogger.getLogger[F]
}

class Http4SResponseCrypto[F[_]](
  override val crypto: Generator with Verifier,
  override val config: HttpCryptoConfig = new HttpCryptoConfig {}
)(override implicit val F: Async[F])
    extends pl.abankowski.httpsigner.http4s.impl.Http4sResponseSigner[F]
    with pl.abankowski.httpsigner.http4s.impl.Http4sResponseVerifier[F] {
  override val logger: Logger[F] = Slf4jLogger.getLogger[F]
}
