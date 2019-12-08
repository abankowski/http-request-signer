package requestsigner

import java.security.SecureRandom

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{HttpHeader, HttpMethods, HttpRequest, Uri}
import akka.http.scaladsl.model.Uri.Path
import akka.stream.ActorMaterializer
import akka.testkit.TestKit
import cats.effect.IO
import org.bouncycastle.crypto.generators.RSAKeyPairGenerator
import org.scalatest.{FunSpec, FunSpecLike, Matchers}
import pl.abankowski.requestsigner.signature.rsa.Rsa
import pl.abankowski.requestsigner.{AkkaRequestCrypto, RequestCrypto, SignatureInvalid, SignatureMissing, SignatureValid}

class AkkaRequestCryptoSpec extends TestKit(ActorSystem("MySpec")) with FunSpecLike with Matchers {
  private implicit val ctx = IO.contextShift(scala.concurrent.ExecutionContext.global)

  describe("Having Http4sRequestSigner set up") {

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
    val crypto2 = Rsa(rsag.generateKeyPair())

    var signer1: AkkaRequestCrypto = new AkkaRequestCrypto(crypto1)
    var signer2: AkkaRequestCrypto = new AkkaRequestCrypto(crypto2)


    it("should generate a signature") {

      val uri = Uri(
        scheme = "https",
        path = Path("/foo")
      )

      val req = HttpRequest(
        method = HttpMethods.GET,
        uri = uri,
        headers = List.empty[HttpHeader]
      )

      val signed = signer1.sign(req).unsafeRunSync()

      val signature = signed.headers.find(_.name == RequestCrypto.signatureHeaderName)

      signature shouldBe defined

      signature.map(_.value.nonEmpty) shouldEqual Some(true)
    }

    it("different keys give different signature") {
      val uri = Uri(
        scheme = "https",
        path = Path("/foo")
      )

      val req = HttpRequest(
        method = HttpMethods.GET,
        uri = uri,
        headers = List.empty[HttpHeader]
      )

      val signed1 = signer1.sign(req).unsafeRunSync()
      val signed2 = signer2.sign(req).unsafeRunSync()

      val signature1 = signed1.headers.find(_.name == RequestCrypto.signatureHeaderName)
      val signature2 = signed2.headers.find(_.name == RequestCrypto.signatureHeaderName)

      signature1 shouldBe defined
      signature2 shouldBe defined

      signature1.map(_.value) shouldNot equal(signature2.map(_.value))
    }

    it("different uri give different signature") {
      val uri1 = Uri(
        scheme = "https",
        path = Path("/foo")
      )

      val req1 = HttpRequest(
        method = HttpMethods.GET,
        uri = uri1,
        headers = List.empty[HttpHeader]
      )

      val uri2 = Uri(
        scheme = "https",
        path = Path("/bar")
      )

      val req2 = HttpRequest(
        method = HttpMethods.GET,
        uri = uri2,
        headers = List.empty[HttpHeader]
      )


      val signed1 = signer1.sign(req1).unsafeRunSync()
      val signed2 = signer1.sign(req2).unsafeRunSync()

      val signature1 = signed1.headers.find(_.name == RequestCrypto.signatureHeaderName)
      val signature2 = signed2.headers.find(_.name == RequestCrypto.signatureHeaderName)

      signature1 shouldBe defined
      signature2 shouldBe defined

      signature1.map(_.value) shouldNot equal(signature2.map(_.value))
    }

    it("should accept valid signature") {
      val uri = Uri(
        scheme = "https",
        path = Path("/foo")
      )

      val req = HttpRequest(
        method = HttpMethods.GET,
        uri = uri,
        headers = List.empty[HttpHeader]
      )

      val signed = signer1.sign(req).unsafeRunSync()

      signer1.verify(signed).unsafeRunSync() shouldEqual(SignatureValid)
    }

    it("should reject invalid signature") {
      val uri = Uri(
        scheme = "https",
        path = Path("/foo")
      )

      val req = HttpRequest(
        method = HttpMethods.GET,
        uri = uri,
        headers = List.empty[HttpHeader]
      )

      val signed = signer1.sign(req).unsafeRunSync()

      signer2.verify(signed).unsafeRunSync() shouldEqual(SignatureInvalid)
    }


    it("should not find a signature") {
      val uri = Uri(
        scheme = "https",
        path = Path("/foo")
      )

      val req = HttpRequest(
        method = HttpMethods.GET,
        uri = uri,
        headers = List.empty[HttpHeader]
      )

      signer2.verify(req).unsafeRunSync() shouldEqual(SignatureMissing)
    }
  }
}
