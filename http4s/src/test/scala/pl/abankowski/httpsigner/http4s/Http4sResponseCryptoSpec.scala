package pl.abankowski.httpsigner.http4s

import java.security.SecureRandom

import cats.effect.IO
import org.bouncycastle.crypto.generators.RSAKeyPairGenerator
import org.http4s.{Headers, Response}
import org.scalatest.{FunSpec, Matchers}
import pl.abankowski.httpsigner.{
  SignatureInvalid,
  SignatureMissing,
  SignatureValid
}
import pl.abankowski.httpsigner.signature.rsa.Rsa

class Http4sResponseCryptoSpec extends FunSpec with Matchers {
  private implicit val ctx =
    IO.contextShift(scala.concurrent.ExecutionContext.global)

  describe("Having Http4sResponseSigner set up") {

    val keySizeBits = 2 ^ 1024
    val strength = 12

    import java.math.BigInteger

    import org.bouncycastle.crypto.params.RSAKeyGenerationParameters
    val publicExponent = BigInteger.valueOf(0x10001)

    val rnd = SecureRandom.getInstanceStrong
    val rsagp =
      new RSAKeyGenerationParameters(publicExponent, rnd, keySizeBits, strength)

    val rsag = new RSAKeyPairGenerator
    rsag.init(rsagp)

    val crypto1 = Rsa(rsag.generateKeyPair())
    val crypto2 = Rsa(rsag.generateKeyPair())

    var signer1: Http4SResponseCrypto[IO] =
      new Http4SResponseCrypto[IO](crypto1)
    var signer2: Http4SResponseCrypto[IO] =
      new Http4SResponseCrypto[IO](crypto2)

    it("should generate a signature") {
      val res = Response[IO](headers = Headers.empty)

      val signed = signer1.sign(res).unsafeRunSync()

      val signature =
        signed.headers.find(_.name.value == signer1.config.signatureHeaderName)

      signature shouldBe defined

      signature.map(_.value.nonEmpty) shouldEqual Some(true)
    }

    it("different keys give different signature") {
      val res = Response[IO](headers = Headers.empty)

      val signed1 = signer1.sign(res).unsafeRunSync()
      val signed2 = signer2.sign(res).unsafeRunSync()

      val signature1 =
        signed1.headers.find(_.name.value == signer1.config.signatureHeaderName)
      val signature2 =
        signed2.headers.find(_.name.value == signer2.config.signatureHeaderName)

      signature1 shouldBe defined
      signature2 shouldBe defined

      signature1.map(_.value) shouldNot equal(signature2.map(_.value))
    }

    it("should accept valid signature") {

      val res = Response[IO](headers = Headers.empty)

      val signed = signer1.sign(res).unsafeRunSync()

      signer1.verify(signed).unsafeRunSync() shouldEqual (SignatureValid)
    }

    it("should reject invalid signature") {

      val res = Response[IO](headers = Headers.empty)

      val signed = signer1.sign(res).unsafeRunSync()

      signer2.verify(signed).unsafeRunSync() shouldEqual (SignatureInvalid)
    }

    it("should not find a signature") {
      val res = Response[IO](headers = Headers.empty)

      signer2.verify(res).unsafeRunSync() shouldEqual (SignatureMissing)
    }
  }
}
