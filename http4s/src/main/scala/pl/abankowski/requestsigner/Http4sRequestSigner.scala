package pl.abankowski.requestsigner

import java.io.ByteArrayOutputStream

import cats.effect.{ContextShift, IO}
import org.bouncycastle.crypto.params.DSAParameters
import org.http4s.{Header, Headers, Request}

class Http4sRequestSigner(cipher: DSAParameters)(implicit ctx: ContextShift[IO])
  extends AbstractRequestSigner[Request[IO], IO](cipher){

  override def sign(request: Request[IO]): IO[Request[IO]] =
    message(request)
      .map(calculateSignature)
      .map(signature =>
        request.withHeaders(Headers(Header(RequestSigner.signatureHeaderName, signature) :: request.headers.toList))
      )

  def message(request: Request[IO]): IO[Array[Byte]]=
    IO {
      (List(request.method.name, request.uri.renderString) ++
      request.headers.toList.collect({
        case header if RequestSigner.headers.contains(header.name.value) => s"${header.name.value}:${header.value}"
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

  override def verify(request: Request[IO]): IO[SignatureVerificationResult] =
    request.headers.find(_.name.value == RequestSigner.signatureHeaderName)
      .map(signature =>
        message(request).map(verifySignature(_, signature.value))
      )
      .getOrElse(IO.pure(SignatureMissing))
}
