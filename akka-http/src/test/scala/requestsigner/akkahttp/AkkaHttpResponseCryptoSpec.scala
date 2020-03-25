package requestsigner.akkahttp

import java.security.SecureRandom

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{HttpHeader, HttpResponse}
import akka.testkit.TestKit
import cats.effect.IO
import org.bouncycastle.crypto.generators.RSAKeyPairGenerator
import org.scalatest.{FunSpecLike, Matchers}
import pl.abankowski.httpsigner.{SignatureInvalid, SignatureMissing, SignatureValid}
import pl.abankowski.httpsigner.akkahttp.AkkaHttpResponseCrypto
import pl.abankowski.httpsigner.signature.rsa.Rsa

class AkkaHttpResponseCryptoSpec extends TestKit(ActorSystem("MySpec")) with FunSpecLike with Matchers {
  private implicit val ctx = IO.contextShift(scala.concurrent.ExecutionContext.global)

  describe("Having Http4sResponseSigner set up") {

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

    var signer1: AkkaHttpResponseCrypto = new AkkaHttpResponseCrypto(crypto1)
    var signer2: AkkaHttpResponseCrypto = new AkkaHttpResponseCrypto(crypto2)

    it("should generate a signature") {

      val res = HttpResponse(
        headers = List.empty[HttpHeader]
      )

      val signed = signer1.sign(res).unsafeRunSync()

      val signature = signed.headers.find(_.name == signer1.config.signatureHeaderName)

      signature shouldBe defined

      signature.map(_.value.nonEmpty) shouldEqual Some(true)
    }

    it("different keys give different signature") {
      val req = HttpResponse(
        headers = List.empty[HttpHeader]
      )

      val signed1 = signer1.sign(req).unsafeRunSync()
      val signed2 = signer2.sign(req).unsafeRunSync()

      val signature1 = signed1.headers.find(_.name == signer1.config.signatureHeaderName)
      val signature2 = signed2.headers.find(_.name == signer2.config.signatureHeaderName)

      signature1 shouldBe defined
      signature2 shouldBe defined

      signature1.map(_.value) shouldNot equal(signature2.map(_.value))
    }


    it("should accept valid signature") {

      val req = HttpResponse(
        headers = List.empty[HttpHeader]
      )

      val signed = signer1.sign(req).unsafeRunSync()

      signer1.verify(signed).unsafeRunSync() shouldEqual(SignatureValid)
    }

    it("should reject invalid signature") {
      val req = HttpResponse(
        headers = List.empty[HttpHeader]
      )

      val signed = signer1.sign(req).unsafeRunSync()

      signer2.verify(signed).unsafeRunSync() shouldEqual(SignatureInvalid)
    }


    it("should not find a signature") {
      val req = HttpResponse(
        headers = List.empty[HttpHeader]
      )

      signer2.verify(req).unsafeRunSync() shouldEqual(SignatureMissing)
    }
  }
}
