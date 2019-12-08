package requestsigner

import java.security.SecureRandom

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{HttpHeader, HttpMethods, HttpRequest => AkkaHttpRequest, Uri => AkkaUri}
import akka.http.scaladsl.model.Uri.{Authority => AkkaAuthority, Host, Path}
import akka.testkit.TestKit
import cats.effect.IO
import org.bouncycastle.crypto.generators.RSAKeyPairGenerator
import org.http4s.{Headers, Method, Request, Uri}
import org.http4s.Uri.{Authority, RegName, Scheme}
import org.scalatest.{FunSpec, FunSpecLike, Matchers}
import pl.abankowski.requestsigner.signature.rsa.Rsa
import pl.abankowski.requestsigner.{AkkaRequestCrypto, Http4SRequestCrypto, RequestCrypto, SignatureInvalid, SignatureMissing, SignatureValid}

class InteropSpec extends TestKit(ActorSystem("MySpec")) with FunSpecLike with Matchers {
  private implicit val ctx = IO.contextShift(scala.concurrent.ExecutionContext.global)

  describe("Having all signers set up") {

    val keySizeBits = 2^1024
    val strength = 12

    import java.math.BigInteger

    import org.bouncycastle.crypto.params.RSAKeyGenerationParameters
    val publicExponent = BigInteger.valueOf(0x10001)

    val rnd = SecureRandom.getInstanceStrong
    val rsagp = new RSAKeyGenerationParameters(publicExponent, rnd, keySizeBits, strength)

    val rsag = new RSAKeyPairGenerator
    rsag.init(rsagp)

    val crypto1 = Rsa(rsag.generateKeyPair())

    var signer1: AkkaRequestCrypto = new AkkaRequestCrypto(crypto1)
    var signer2: Http4SRequestCrypto = new Http4SRequestCrypto(crypto1)

    it("they should generate equal signatures") {
      val req = AkkaHttpRequest(
        method = HttpMethods.GET,
        uri = AkkaUri(
          scheme = "http",
          authority = AkkaUri.Authority(host = Host("example.com")),
          path = Path("/foo")
        ),
        headers = List.empty[HttpHeader]
      )

      val signed1 = signer1.sign(req).unsafeRunSync()

      val signature1 = signed1.headers.find(_.name == RequestCrypto.signatureHeaderName)

      val baseUri = Uri.apply(
        Some(Scheme.http),
        Some(Authority(host = RegName("example.com"), port = Some(9000)))
      )

      val req2 = Request[IO](
        method = Method.GET,
        uri = baseUri.withPath("/foo"),
        headers = Headers.empty)

      val signed2 = signer2.sign(req2).unsafeRunSync()

      val signature2 = signed2.headers.find(_.name.value == RequestCrypto.signatureHeaderName)

      signature1 shouldEqual signature2
    }
  }
}
