package pl.abankowski.httpsigner.akkahttp

import java.io.ByteArrayOutputStream

import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.Materializer
import cats.effect.{ContextShift, IO}
import pl.abankowski.httpsigner.{HttpCryptoConfig, HttpSigner, HttpVerifier, SignatureMissing, SignatureVerificationResult}

import scala.language.{higherKinds, postfixOps}
import scala.concurrent.duration._

package object impl {

   trait RequestHelpers {

    val config: HttpCryptoConfig

    protected def message(request: HttpRequest)(implicit mat: Materializer, ctx: ContextShift[IO]) = IO {
      (List(request.method.value, request.uri.toString()) ++
       request.headers.collect({
         case header if config.protectedHeaders.contains(header.name()) => s"${header.name()}:${header.value()}"
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

  trait ResponseHelpers {

    val config: HttpCryptoConfig

    protected def message(request: HttpResponse)(implicit mat: Materializer, ctx: ContextShift[IO]) = IO {
       request.headers.collect({
         case header if config.protectedHeaders.contains(header.name()) => s"${header.name()}:${header.value()}"
       }).foldLeft(new ByteArrayOutputStream())({ (buffer, value) =>
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

  trait AkkaHttpRequestSigner extends HttpSigner[HttpRequest, IO] with RequestHelpers {
    implicit val mat: Materializer
    implicit val ctx: ContextShift[IO]

    override def sign(request: HttpRequest): IO[HttpRequest] =
      message(request)
        .map(calculateSignature)
        .map(signature =>
          request.withHeaders(request.headers :+ SignatureHeader(signature)))
  }

  trait AkkaHttpRequestVerifier extends HttpVerifier[HttpRequest, IO] with RequestHelpers{
    implicit val mat: Materializer
    implicit val ctx: ContextShift[IO]

    val config: HttpCryptoConfig

    override def verify(request: HttpRequest): IO[SignatureVerificationResult] =
      request.headers.find(_.name() == config.signatureHeaderName).map(signature =>
        message(request).map( message =>
          verifySignature(message, signature.value())
        )).getOrElse(IO.pure(SignatureMissing))
  }

  trait AkkaHttpResponseSigner extends HttpSigner[HttpResponse, IO] with ResponseHelpers {
    implicit val mat: Materializer
    implicit val ctx: ContextShift[IO]

    override def sign(response: HttpResponse): IO[HttpResponse] =
      message(response)
        .map(calculateSignature)
        .map(signature =>
          response.withHeaders(response.headers :+ SignatureHeader(signature)))
  }

  trait AkkaHttpResponseVerifier extends HttpVerifier[HttpResponse, IO] with ResponseHelpers {
    implicit val mat: Materializer
    implicit val ctx: ContextShift[IO]
    val config: HttpCryptoConfig

    override def verify(response: HttpResponse): IO[SignatureVerificationResult] =
      response.headers.find(_.name() == config.signatureHeaderName).map(signature =>
        message(response).map( message =>
          verifySignature(message, signature.value())
        )).getOrElse(IO.pure(SignatureMissing))
  }
}
