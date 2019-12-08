package pl.abankowski.requestsigner

import java.io.ByteArrayOutputStream

import cats.effect.{ContextShift, IO}
import org.http4s.{Header, Headers, Request}

package object impl {

  trait RequestHelpers {
    protected def message(request: Request[IO]): IO[Array[Byte]]=
      IO {
        (List(request.method.name, request.uri.renderString) ++
         request.headers.toList.collect({
           case header if RequestCrypto.headers.contains(header.name.value) => s"${header.name.value}:${header.value}"
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

  trait Http4sRequestSigner extends RequestSigner[Request[IO], IO] with RequestHelpers {
    implicit val ctx: ContextShift[IO]

    override def sign(request: Request[IO]): IO[Request[IO]] =
      message(request)
        .map(calculateSignature)
        .map(signature =>
          request.withHeaders(Headers(Header(RequestCrypto.signatureHeaderName, signature) :: request.headers.toList))
        )
  }

  trait Http4sRequestVerifier extends RequestVerifier[Request[IO], IO] with RequestHelpers{
    implicit val ctx: ContextShift[IO]

    override def verify(request: Request[IO]): IO[SignatureVerificationResult] =
      request.headers.find(_.name.value == RequestCrypto.signatureHeaderName)
        .map(signature =>
          message(request).map(verifySignature(_, signature.value))
        )
        .getOrElse(IO.pure(SignatureMissing))
  }
}
