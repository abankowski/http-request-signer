package pl.abankowski.httpsigner.http4s

import cats.effect.{ContextShift, IO}
import pl.abankowski.httpsigner.signature.{Generator, Verifier}

class Http4sResponseSigner(override val crypto: Generator)(override implicit val ctx: ContextShift[IO])
  extends pl.abankowski.httpsigner.http4s.impl.Http4sResponseSigner

class Http4sResponseVerifier(override val crypto: Verifier)(override implicit val ctx: ContextShift[IO])
  extends pl.abankowski.httpsigner.http4s.impl.Http4sResponseVerifier

class Http4SResponseCrypto(override val crypto: Generator with Verifier)(override implicit val ctx: ContextShift[IO])
  extends pl.abankowski.httpsigner.http4s.impl.Http4sResponseSigner
    with pl.abankowski.httpsigner.http4s.impl.Http4sResponseVerifier