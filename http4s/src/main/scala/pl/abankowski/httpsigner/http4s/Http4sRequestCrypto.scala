package pl.abankowski.httpsigner.http4s

import cats.effect.{ContextShift, IO}
import pl.abankowski.httpsigner.signature.{Generator, Verifier}

class Http4sRequestSigner(override val crypto: Generator)(override implicit val ctx: ContextShift[IO])
  extends pl.abankowski.httpsigner.http4s.impl.Http4sRequestSigner

class Http4sRequestVerifier(override val crypto: Verifier)(override implicit val ctx: ContextShift[IO])
  extends pl.abankowski.httpsigner.http4s.impl.Http4sRequestVerifier

class Http4sRequestCrypto(override val crypto: Generator with Verifier)(override implicit val ctx: ContextShift[IO])
  extends pl.abankowski.httpsigner.http4s.impl.Http4sRequestSigner
    with pl.abankowski.httpsigner.http4s.impl.Http4sRequestVerifier
