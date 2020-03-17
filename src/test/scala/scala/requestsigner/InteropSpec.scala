package requestsigner

import java.security.SecureRandom

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{HttpHeader, HttpMethods, HttpRequest => AkkaHttpRequest, HttpResponse => AkkaHttpResponse, Uri => AkkaUri}
import akka.http.scaladsl.model.Uri.{Host, Path}
import akka.testkit.TestKit
import cats.effect.IO
import org.bouncycastle.crypto.generators.RSAKeyPairGenerator
import org.http4s.{Header, Headers, Method, Request, Response, Uri}
import org.http4s.Uri.{Authority, RegName, Scheme}
import org.scalatest.{FunSpec, FunSpecLike, Matchers}
import pl.abankowski.httpsigner.signature.rsa.Rsa
import pl.abankowski.httpsigner.{HttpCrypto, SignatureValid}
import pl.abankowski.httpsigner.akkahttp.{AkkaHttpRequestCrypto, AkkaHttpResponseCrypto}
import pl.abankowski.httpsigner.http4s.{Http4sRequestCrypto, Http4SResponseCrypto}

class InteropSpec extends TestKit(ActorSystem("MySpec")) with FunSpecLike with Matchers {
  private implicit val ctx = IO.contextShift(scala.concurrent.ExecutionContext.global)

  describe("Having all request signers set up") {

    val keySizeBits = 2^1024
    val strength = 12

    import java.math.BigInteger

    import org.bouncycastle.crypto.params.RSAKeyGenerationParameters
    val publicExponent = BigInteger.valueOf(0x10001)

    val rnd = SecureRandom.getInstanceStrong
    val rsagp = new RSAKeyGenerationParameters(publicExponent, rnd, keySizeBits, strength)

    val rsag = new RSAKeyPairGenerator
    rsag.init(rsagp)

    val crypto = Rsa(rsag.generateKeyPair())

    var signer1: AkkaHttpRequestCrypto = new AkkaHttpRequestCrypto(crypto)
    var signer2: Http4sRequestCrypto = new Http4sRequestCrypto(crypto)

    it("they should be compatible") {
      val req = AkkaHttpRequest(
        method = HttpMethods.GET,
        uri = AkkaUri(
          scheme = "http",
          authority = AkkaUri.Authority(host = Host("example.com"),  port = 9000),
          path = Path("/foo"),
        ),
        headers = List.empty[HttpHeader]
      )

      val signed1 = signer1.sign(req).unsafeRunSync()

      val signature1 = signed1.headers.find(_.name == HttpCrypto.signatureHeaderName)

      val baseUri = Uri.apply(
        Some(Scheme.http),
        Some(Authority(host = RegName("example.com"), port = Some(9000)))
      )

      val req2 = Request[IO](
        method = Method.GET,
        uri = baseUri.withPath("/foo"),
        headers = Headers.of(Header(HttpCrypto.signatureHeaderName, signature1.map(_.value()).getOrElse(""))))

      val verified = signer2.verify(req2).unsafeRunSync()

      verified shouldEqual SignatureValid
    }
  }

  describe("Having all response signers set up") {

    val keySizeBits = 2^1024
    val strength = 12

    import java.math.BigInteger

    import org.bouncycastle.crypto.params.RSAKeyGenerationParameters
    val publicExponent = BigInteger.valueOf(0x10001)

    val rnd = SecureRandom.getInstanceStrong
    val rsagp = new RSAKeyGenerationParameters(publicExponent, rnd, keySizeBits, strength)

    val rsag = new RSAKeyPairGenerator
    rsag.init(rsagp)

    val crypto = Rsa(rsag.generateKeyPair())

    var signer1: AkkaHttpResponseCrypto = new AkkaHttpResponseCrypto(crypto)
    var signer2: Http4SResponseCrypto = new Http4SResponseCrypto(crypto)

    it("they should be compatible") {
      val res = AkkaHttpResponse(
        headers = List.empty[HttpHeader]
      )

      val signed1 = signer1.sign(res).unsafeRunSync()

      val signature1 = signed1.headers.find(_.name == HttpCrypto.signatureHeaderName)

      val res2 = Response[IO](
        headers = Headers.of(Header(HttpCrypto.signatureHeaderName, signature1.map(_.value()).getOrElse(""))))

      val verified = signer2.verify(res2).unsafeRunSync()

      verified shouldEqual SignatureValid
    }
  }
}
