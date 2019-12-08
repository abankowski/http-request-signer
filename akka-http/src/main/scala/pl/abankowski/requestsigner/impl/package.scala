package pl.abankowski.requestsigner

import java.io.ByteArrayOutputStream

import akka.http.scaladsl.model.HttpRequest
import akka.stream.Materializer
import cats.effect.{ContextShift, IO}
import scala.language.{higherKinds, postfixOps}
import scala.concurrent.duration._

package object impl {

   trait RequestHelpers {
    protected def message(request: HttpRequest)(implicit mat: Materializer, ctx: ContextShift[IO]) = IO {
      (List(request.method.value, request.uri.toString()) ++
       request.headers.collect({
         case header if RequestCrypto.headers.contains(header.name()) => s"${header.name()}:${header.value()}"
       })).foldLeft(new ByteArrayOutputStream())({ (buffer, value) =>
        buffer.write(value.getBytes)
        buffer
      })
    }.flatMap ( content =>
      IO.fromFuture(IO {
        request.entity.toStrict(10 seconds)
      }).map {e =>
        content.write(e.getData().toArray)
        content.toByteArray
      })
  }

  trait AkkaRequestSigner extends RequestSigner[HttpRequest, IO] with RequestHelpers {
    implicit val mat: Materializer
    implicit val ctx: ContextShift[IO]

    override def sign(request: HttpRequest): IO[HttpRequest] =
      message(request)
        .map(calculateSignature)
        .map(signature =>
          request.withHeaders(request.headers :+ SignatureHeader(signature)))
  }

  trait AkkaRequestVerifier extends RequestVerifier[HttpRequest, IO] with RequestHelpers{
    implicit val mat: Materializer
    implicit val ctx: ContextShift[IO]

    override def verify(request: HttpRequest): IO[SignatureVerificationResult] =
      request.headers.find(_.name() == RequestCrypto.signatureHeaderName).map(signature =>
        message(request).map( message =>
          verifySignature(message, signature.value())
        )).getOrElse(IO.pure(SignatureMissing))
  }
}
