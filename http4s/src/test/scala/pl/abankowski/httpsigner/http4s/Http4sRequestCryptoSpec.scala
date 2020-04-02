package pl.abankowski.httpsigner.http4s

import java.security.SecureRandom

import cats.effect.IO
import org.bouncycastle.crypto.generators.RSAKeyPairGenerator
import org.http4s.{Headers, Method, Request, Uri}
import org.http4s.Uri.{Authority, RegName, Scheme}
import org.scalatest.{FunSpec, Matchers}
import pl.abankowski.httpsigner.{
  SignatureInvalid,
  SignatureMissing,
  SignatureValid
}
import pl.abankowski.httpsigner.signature.rsa.Rsa

class Http4sRequestCryptoSpec extends FunSpec with Matchers {
  private implicit val ctx =
    IO.contextShift(scala.concurrent.ExecutionContext.global)

  describe("Having Http4sRequestSigner set up") {

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

    var signer1: Http4sRequestCrypto[IO] = new Http4sRequestCrypto[IO](crypto1)
    var signer2: Http4sRequestCrypto[IO] = new Http4sRequestCrypto[IO](crypto2)

    it("should generate a signature") {
      val baseUri = Uri.apply(
        Some(Scheme.http),
        Some(Authority(host = RegName("example.com"), port = Some(9000)))
      )

      val req = Request[IO](
        method = Method.GET,
        uri = baseUri.withPath("/foo"),
        headers = Headers.empty
      )

      val signed = signer1.sign(req).unsafeRunSync()

      val signature =
        signed.headers.find(_.name.value == signer1.config.signatureHeaderName)

      signature shouldBe defined

      signature.map(_.value.nonEmpty) shouldEqual Some(true)
    }

    it("different keys give different signature") {
      val baseUri = Uri.apply(
        Some(Scheme.http),
        Some(Authority(host = RegName("example.com"), port = Some(9000)))
      )

      val req = Request[IO](
        method = Method.GET,
        uri = baseUri.withPath("/foo"),
        headers = Headers.empty
      )

      val signed1 = signer1.sign(req).unsafeRunSync()
      val signed2 = signer2.sign(req).unsafeRunSync()

      val signature1 =
        signed1.headers.find(_.name.value == signer1.config.signatureHeaderName)
      val signature2 =
        signed2.headers.find(_.name.value == signer2.config.signatureHeaderName)

      signature1 shouldBe defined
      signature2 shouldBe defined

      signature1.map(_.value) shouldNot equal(signature2.map(_.value))
    }

    it("different uri give different signature") {
      val baseUri1 = Uri.apply(
        Some(Scheme.http),
        Some(Authority(host = RegName("example.com"), port = Some(9000)))
      )

      val req1 = Request[IO](
        method = Method.GET,
        uri = baseUri1.withPath("/foo"),
        headers = Headers.empty
      )

      val baseUri2 = Uri.apply(
        Some(Scheme.http),
        Some(Authority(host = RegName("example.pl"), port = Some(9000)))
      )

      val req2 = Request[IO](
        method = Method.GET,
        uri = baseUri2.withPath("/foo"),
        headers = Headers.empty
      )

      val signed1 = signer1.sign(req1).unsafeRunSync()
      val signed2 = signer1.sign(req2).unsafeRunSync()

      val signature1 =
        signed1.headers.find(_.name.value == signer1.config.signatureHeaderName)
      val signature2 =
        signed2.headers.find(_.name.value == signer1.config.signatureHeaderName)

      signature1 shouldBe defined
      signature2 shouldBe defined

      signature1.map(_.value) shouldNot equal(signature2.map(_.value))
    }

    it("should accept valid signature") {
      val baseUri = Uri.apply(
        Some(Scheme.http),
        Some(Authority(host = RegName("example.com"), port = Some(9000)))
      )

      val req = Request[IO](
        method = Method.GET,
        uri = baseUri.withPath("/foo"),
        headers = Headers.empty
      )

      val signed = signer1.sign(req).unsafeRunSync()

      signer1.verify(signed).unsafeRunSync() shouldEqual (SignatureValid)
    }

    it("should reject invalid signature") {
      val baseUri = Uri.apply(
        Some(Scheme.http),
        Some(Authority(host = RegName("example.com"), port = Some(9000)))
      )

      val req = Request[IO](
        method = Method.GET,
        uri = baseUri.withPath("/foo"),
        headers = Headers.empty
      )

      val signed = signer1.sign(req).unsafeRunSync()

      signer2.verify(signed).unsafeRunSync() shouldEqual (SignatureInvalid)
    }

    it("should not find a signature") {
      val baseUri = Uri.apply(
        Some(Scheme.http),
        Some(Authority(host = RegName("example.com"), port = Some(9000)))
      )

      val req = Request[IO](
        method = Method.GET,
        uri = baseUri.withPath("/foo"),
        headers = Headers.empty
      )

      signer2.verify(req).unsafeRunSync() shouldEqual (SignatureMissing)
    }
  }
}
