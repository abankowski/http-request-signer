package pl.abankowski.httpsigner.akkahttp

import java.io.ByteArrayOutputStream

import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.Materializer
import cats.effect.{Async, ContextShift}
import cats.implicits._
import pl.abankowski.httpsigner.{
  HttpCryptoConfig,
  HttpSigner,
  HttpVerifier,
  SignatureMissing,
  SignatureVerificationResult
}

import scala.language.{higherKinds, postfixOps}
import scala.concurrent.duration._

package object impl {

  trait RequestHelpers[F[_]] {
    implicit val ctx: ContextShift[F]

    val config: HttpCryptoConfig

    protected def message(
      request: HttpRequest
    )(implicit mat: Materializer, ctx: ContextShift[F], F: Async[F]) =
      F.delay {
        (List(request.method.value, request.uri.toString()) ++
          request.headers.collect({
            case header if config.protectedHeaders.contains(header.name()) =>
              s"${header.name()}:${header.value()}"
          })).foldLeft(new ByteArrayOutputStream()) { (buffer, value) =>
          buffer.write(value.getBytes)
          buffer
        }
      }.flatMap(content =>
        Async
          .fromFuture(F.delay {
            request.entity.toStrict(10 seconds)
          })
          .map { e =>
            content.write(e.getData().toArray)
            content.toByteArray
          }
      )
  }

  trait ResponseHelpers[F[_]] {
    val config: HttpCryptoConfig

    protected def message(
      request: HttpResponse
    )(implicit mat: Materializer, ctx: ContextShift[F], F: Async[F]) =
      F.delay {
        request.headers
          .collect({
            case header if config.protectedHeaders.contains(header.name()) =>
              s"${header.name()}:${header.value()}"
          })
          .foldLeft(new ByteArrayOutputStream()) { (buffer, value) =>
            buffer.write(value.getBytes)
            buffer
          }
      }.flatMap(content =>
        Async
          .fromFuture(F.delay {
            request.entity.toStrict(10 seconds)
          })
          .map { e =>
            content.write(e.getData().toArray)
            content.toByteArray
          }
      )
  }

  trait AkkaHttpRequestSigner[F[_]] extends HttpSigner[HttpRequest, F] with RequestHelpers[F] {
    implicit val mat: Materializer
    implicit val ctx: ContextShift[F]
    implicit val F: Async[F]

    override def sign(request: HttpRequest): F[HttpRequest] =
      message(request)
        .map(calculateSignature)
        .map(signature => request.withHeaders(request.headers :+ SignatureHeader(signature)))
  }

  trait AkkaHttpRequestVerifier[F[_]] extends HttpVerifier[HttpRequest, F] with RequestHelpers[F] {
    implicit val mat: Materializer
    implicit val ctx: ContextShift[F]
    implicit val F: Async[F]

    val config: HttpCryptoConfig

    override def verify(request: HttpRequest): F[SignatureVerificationResult] =
      request.headers
        .find(_.name() == config.signatureHeaderName)
        .map(signature =>
          message(request)
            .map(message => verifySignature(message, signature.value()))
        )
        .getOrElse(F.pure(SignatureMissing))
  }

  trait AkkaHttpResponseSigner[F[_]] extends HttpSigner[HttpResponse, F] with ResponseHelpers[F] {
    implicit val mat: Materializer
    implicit val ctx: ContextShift[F]
    implicit val F: Async[F]

    override def sign(response: HttpResponse): F[HttpResponse] =
      message(response)
        .map(calculateSignature)
        .map(signature => response.withHeaders(response.headers :+ SignatureHeader(signature)))
  }

  trait AkkaHttpResponseVerifier[F[_]] extends HttpVerifier[HttpResponse, F] with ResponseHelpers[F] {
    implicit val mat: Materializer
    implicit val ctx: ContextShift[F]
    implicit val F: Async[F]
    val config: HttpCryptoConfig

    override def verify(
      response: HttpResponse
    ): F[SignatureVerificationResult] =
      response.headers
        .find(_.name() == config.signatureHeaderName)
        .map(signature =>
          message(response)
            .map(message => verifySignature(message, signature.value()))
        )
        .getOrElse(F.pure(SignatureMissing))
  }
}
