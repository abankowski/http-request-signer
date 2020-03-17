package pl.abankowski.httpsigner.http4s

import java.io.ByteArrayOutputStream

import cats.effect.{ContextShift, IO}
import org.http4s.{Header, Headers, Request, Response}
import pl.abankowski.httpsigner.{HttpCrypto, HttpSigner, HttpVerifier, SignatureMissing, SignatureVerificationResult}

package object impl {

  trait RequestHelpers {
    protected def message(request: Request[IO]): IO[Array[Byte]] =
      IO {
        (List(request.method.name, request.uri.renderString) ++
         request.headers.toList.collect({
           case header if HttpCrypto.headers.contains(header.name.value) => s"${header.name.value}:${header.value}"
         })).foldLeft(new ByteArrayOutputStream())({ (buffer, value) =>
          buffer.write(value.getBytes)
          buffer
        })
      }.flatMap( content =>
        request.body.compile.toVector.map(_.toArray).map{ body =>
          content.write(body)
          content.toByteArray
        }
      )
  }

  trait ResponseHelpers {
    protected def message(response: Response[IO]): IO[Array[Byte]] =
      IO {
        response.headers.toList.collect({
           case header if HttpCrypto.headers.contains(header.name.value) => s"${header.name.value}:${header.value}"
        }).foldLeft(new ByteArrayOutputStream())({ (buffer, value) =>
          buffer.write(value.getBytes)
          buffer
        })
      }.flatMap( content =>
        response.body.compile.toVector.map(_.toArray).map{ body =>
          content.write(body)
          content.toByteArray
        }
      )
  }

  trait Http4sRequestSigner extends HttpSigner[Request[IO], IO] with RequestHelpers {
    implicit val ctx: ContextShift[IO]

    override def sign(request: Request[IO]): IO[Request[IO]] =
      message(request)
        .map(calculateSignature)
        .map(signature =>
          request.withHeaders(Headers(Header(HttpCrypto.signatureHeaderName, signature) :: request.headers.toList))
        )
  }

  trait Http4sRequestVerifier extends HttpVerifier[Request[IO], IO] with RequestHelpers{
    implicit val ctx: ContextShift[IO]

    override def verify(request: Request[IO]): IO[SignatureVerificationResult] =
      request.headers.find(_.name.value == HttpCrypto.signatureHeaderName)
        .map(signature =>
          message(request).map(verifySignature(_, signature.value))
        )
        .getOrElse(IO.pure(SignatureMissing))
  }

  trait Http4sResponseSigner extends HttpSigner[Response[IO], IO] with ResponseHelpers{
    implicit val ctx: ContextShift[IO]

    override def sign(response: Response[IO]): IO[Response[IO]] =
      message(response)
        .map(calculateSignature)
        .map(signature =>
          response.withHeaders(Headers(Header(HttpCrypto.signatureHeaderName, signature) :: response.headers.toList))
        )
  }

  trait Http4sResponseVerifier extends HttpVerifier[Response[IO], IO] with ResponseHelpers{
    implicit val ctx: ContextShift[IO]

    override def verify(response: Response[IO]): IO[SignatureVerificationResult] =
      response.headers.find(_.name.value == HttpCrypto.signatureHeaderName)
        .map(signature =>
          message(response).map(verifySignature(_, signature.value))
        )
        .getOrElse(IO.pure(SignatureMissing))
  }
}
