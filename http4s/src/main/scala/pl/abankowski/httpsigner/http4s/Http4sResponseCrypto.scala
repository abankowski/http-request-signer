package pl.abankowski.httpsigner.http4s

import cats.effect.{ContextShift, IO}
import pl.abankowski.httpsigner.signature.{Generator, Verifier}
import pl.abankowski.httpsigner.HttpCryptoConfig

class Http4sResponseSigner(
  override val crypto: Generator,
  override val config: HttpCryptoConfig = new HttpCryptoConfig {})(
  override implicit val ctx: ContextShift[IO]) extends pl.abankowski.httpsigner.http4s.impl.Http4sResponseSigner

class Http4sResponseVerifier(
  override val crypto: Verifier,
  override val config: HttpCryptoConfig = new HttpCryptoConfig {})(
  override implicit val ctx: ContextShift[IO]) extends pl.abankowski.httpsigner.http4s.impl.Http4sResponseVerifier

class Http4SResponseCrypto(
  override val crypto: Generator with Verifier,
  override val config: HttpCryptoConfig = new HttpCryptoConfig {})(override implicit val ctx: ContextShift[IO])
  extends pl.abankowski.httpsigner.http4s.impl.Http4sResponseSigner
    with pl.abankowski.httpsigner.http4s.impl.Http4sResponseVerifier