package pl.abankowski.httpsigner.http4s

import java.io.ByteArrayOutputStream

import cats.effect.{ContextShift, Sync}
import cats.implicits._
import org.http4s.{Header, Headers, Request, Response}
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import pl.abankowski.httpsigner.{HttpCryptoConfig, HttpSigner, HttpVerifier, SignatureMissing, SignatureVerificationResult}

package object impl {

  trait Helpers {
    protected def md5(msg: Array[Byte]): String = this.synchronized {
      import java.security.MessageDigest

      val md5 = MessageDigest.getInstance("MD5")
      md5.update(msg)
      BigInt(1, md5.digest).toLong.toHexString
    }
  }

  trait RequestHelpers[F[_]] extends Helpers{
    implicit val F: Sync[F]
    val config: HttpCryptoConfig

    protected def message(request: Request[F]): F[Array[Byte]] =
      F.delay {
        (List(request.method.name, request.uri.path, request.uri.query.renderString) ++
          request.headers.toList.collect({
            case header if config.protectedHeaders.contains(header.name.value) =>
              s"${header.name.value}:${header.value}"
          })).foldLeft(new ByteArrayOutputStream()) { (buffer, value) =>
          buffer.write(value.getBytes)
          buffer
        }
      }.flatMap(content =>
        request.body.compile.toVector.map(_.toArray).map { body =>
          content.write(body)
          content.toByteArray
        }
      )
  }

  trait ResponseHelpers[F[_]] extends Helpers{
    implicit val F: Sync[F]
    val config: HttpCryptoConfig

    protected def message(response: Response[F]): F[Array[Byte]] =
      F.delay {
        response.headers.toList
          .collect({
            case header if config.protectedHeaders.contains(header.name.value) =>
              s"${header.name.value}:${header.value}"
          })
          .foldLeft(new ByteArrayOutputStream()) { (buffer, value) =>
            buffer.write(value.getBytes)
            buffer
          }
      }.flatMap { content =>
        response.body.compile.toVector.map(_.toArray).map {body =>
          content.write(body)
          content.toByteArray
        }
      }
  }

  trait Http4sRequestSigner[F[_]] extends HttpSigner[Request[F], F] with RequestHelpers[F] {
    implicit val ctx: ContextShift[F]
    protected val logger: Logger[F]

    override def sign(request: Request[F]): F[Request[F]] =
      message(request)
        .flatMap { msg =>
          val signature = calculateSignature(msg)
          logger.debug(s"Signing request ${request.method} ${request.pathInfo} req-hash=${md5(msg)} req-sig=$signature")
            .map(_ =>
              request.withHeaders(
                Headers(
                  Header(config.signatureHeaderName, signature) :: request.headers.toList
                )
            ))
        }
  }

  trait Http4sRequestVerifier[F[_]] extends HttpVerifier[Request[F], F] with RequestHelpers[F] {
    implicit val ctx: ContextShift[F]
    implicit val F: Sync[F]
    val config: HttpCryptoConfig
    protected val logger: Logger[F]

    override def verify(request: Request[F]): F[SignatureVerificationResult] =
      request.headers
        .find(_.name.value == config.signatureHeaderName)
        .map(signature =>
          message(request).map{ msg =>
            val result = verifySignature(msg, signature.value)
            logger.debug(s"Verifying request ${request.method} ${request.pathInfo} req-hash=${md5(msg)} req-sig=${signature.value} result=${result}")
            result
        })
        .getOrElse(
          logger.debug(s"Verifying request ${request.method} ${request.pathInfo} failed, signature missing")
            .map(_ => SignatureMissing))
   }

  trait Http4sResponseSigner[F[_]] extends HttpSigner[Response[F], F] with ResponseHelpers[F] {
    implicit val ctx: ContextShift[F]
    implicit val F: Sync[F]
    val config: HttpCryptoConfig
    protected val logger: Logger[F]

    override def sign(response: Response[F]): F[Response[F]] =
      message(response)
        .flatMap { msg =>
          val signature = calculateSignature(msg)
          logger.debug(s"Signing response resp-sig=$signature")
            .map(_ =>
            response.withHeaders(
              response.headers.put(Header(config.signatureHeaderName, signature))))
        }
  }

  trait Http4sResponseVerifier[F[_]] extends HttpVerifier[Response[F], F] with ResponseHelpers[F] {
    implicit val ctx: ContextShift[F]
    implicit val F: Sync[F]
    val config: HttpCryptoConfig
    protected val logger: Logger[F]

    override def verify(response: Response[F]): F[SignatureVerificationResult] =
      response.headers
        .find(_.name.value == config.signatureHeaderName)
        .map(signature =>
          message(response).map{ msg =>
            val result = verifySignature(msg, signature.value)
            logger.debug(s"Verifying response resp-hash=${md5(msg)} req-sig=${signature.value} result=${result}")
            result
          })
        .getOrElse(
          logger.debug(s"Verifying response failed, signature missing")
            .map(_ => SignatureMissing))
  }
}
