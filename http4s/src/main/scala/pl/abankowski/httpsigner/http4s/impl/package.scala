package pl.abankowski.httpsigner.http4s

import java.io.ByteArrayOutputStream

import cats.effect.{ContextShift, Sync}
import cats.implicits._
import org.http4s.{Header, Headers, Request, Response}
import pl.abankowski.httpsigner.{
  HttpCryptoConfig,
  HttpSigner,
  HttpVerifier,
  SignatureMissing,
  SignatureVerificationResult
}

package object impl {

  trait RequestHelpers[F[_]] {
    implicit val F: Sync[F]
    val config: HttpCryptoConfig

    protected def message(request: Request[F]): F[Array[Byte]] =
      F.delay {
          (List(request.method.name, request.uri.renderString) ++
            request.headers.toList.collect({
              case header
                  if config.protectedHeaders.contains(header.name.value) =>
                s"${header.name.value}:${header.value}"
            })).foldLeft(new ByteArrayOutputStream())({ (buffer, value) =>
            buffer.write(value.getBytes)
            buffer
          })
        }
        .flatMap(
          content =>
            request.body.compile.toVector.map(_.toArray).map { body =>
              content.write(body)
              content.toByteArray
          }
        )
  }

  trait ResponseHelpers[F[_]] {
    implicit val F: Sync[F]
    val config: HttpCryptoConfig

    protected def message(response: Response[F]): F[Array[Byte]] =
      F.delay {
          response.headers.toList
            .collect({
              case header
                  if config.protectedHeaders.contains(header.name.value) =>
                s"${header.name.value}:${header.value}"
            })
            .foldLeft(new ByteArrayOutputStream())({ (buffer, value) =>
              buffer.write(value.getBytes)
              buffer
            })
        }
        .flatMap(
          content =>
            response.body.compile.toVector.map(_.toArray).map { body =>
              content.write(body)
              content.toByteArray
          }
        )
  }

  trait Http4sRequestSigner[F[_]]
      extends HttpSigner[Request[F], F]
      with RequestHelpers[F] {
    implicit val ctx: ContextShift[F]

    override def sign(request: Request[F]): F[Request[F]] =
      message(request)
        .map(calculateSignature)
        .map(
          signature =>
            request.withHeaders(
              Headers(
                Header(config.signatureHeaderName, signature) :: request.headers.toList
              )
          )
        )
  }

  trait Http4sRequestVerifier[F[_]]
      extends HttpVerifier[Request[F], F]
      with RequestHelpers[F] {
    implicit val ctx: ContextShift[F]
    implicit val F: Sync[F]
    val config: HttpCryptoConfig

    override def verify(request: Request[F]): F[SignatureVerificationResult] =
      request.headers
        .find(_.name.value == config.signatureHeaderName)
        .map(
          signature => message(request).map(verifySignature(_, signature.value))
        )
        .getOrElse(F.pure(SignatureMissing))
  }

  trait Http4sResponseSigner[F[_]]
      extends HttpSigner[Response[F], F]
      with ResponseHelpers[F] {
    implicit val ctx: ContextShift[F]
    implicit val F: Sync[F]
    val config: HttpCryptoConfig

    override def sign(response: Response[F]): F[Response[F]] =
      message(response)
        .map(calculateSignature)
        .map(
          signature =>
            response.withHeaders(
              Headers(
                Header(config.signatureHeaderName, signature) :: response.headers.toList
              )
          )
        )
  }

  trait Http4sResponseVerifier[F[_]]
      extends HttpVerifier[Response[F], F]
      with ResponseHelpers[F] {
    implicit val ctx: ContextShift[F]
    implicit val F: Sync[F]
    val config: HttpCryptoConfig

    override def verify(response: Response[F]): F[SignatureVerificationResult] =
      response.headers
        .find(_.name.value == config.signatureHeaderName)
        .map(
          signature =>
            message(response).map(verifySignature(_, signature.value))
        )
        .getOrElse(F.pure(SignatureMissing))
  }
}
