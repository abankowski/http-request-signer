package pl.abankowski.requestsigner.http4s

import cats.effect.{ContextShift, IO}
import pl.abankowski.requestsigner.signature.{Generator, Verifier}

class Http4SRequestSigner(override val crypto: Generator)(override implicit val ctx: ContextShift[IO])
  extends pl.abankowski.requestsigner.http4s.impl.Http4sRequestSigner

class Http4SRequestVerifier(override val crypto: Verifier)(override implicit val ctx: ContextShift[IO])
  extends pl.abankowski.requestsigner.http4s.impl.Http4sRequestVerifier

class Http4SRequestCrypto(override val crypto: Generator with Verifier)(override implicit val ctx: ContextShift[IO])
  extends pl.abankowski.requestsigner.http4s.impl.Http4sRequestSigner
    with pl.abankowski.requestsigner.http4s.impl.Http4sRequestVerifier