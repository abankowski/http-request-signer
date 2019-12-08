package pl.abankowski.requestsigner

import java.io.ByteArrayOutputStream

import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.headers.{ModeledCustomHeader, ModeledCustomHeaderCompanion}
import akka.stream.Materializer
import cats.effect.{ContextShift, IO}
import pl.abankowski.requestsigner.signature.{Generator, Verifier}

import scala.language.{higherKinds, postfixOps}
import scala.concurrent.duration._
import scala.util.Try

final class SignatureHeader(signature: String) extends ModeledCustomHeader[SignatureHeader] {
  override def renderInRequests = true
  override def renderInResponses = true
  override val companion = SignatureHeader
  override def value: String = signature
}

object SignatureHeader extends ModeledCustomHeaderCompanion[SignatureHeader] {
  override val name = RequestSigner.signatureHeaderName
  override def parse(value: String) = Try(new SignatureHeader(value))
}

class AkkaRequestSigner(crypto: Generator with Verifier)(implicit mat: Materializer, ctx: ContextShift[IO])
  extends AbstractRequestSigner[HttpRequest, IO](crypto: Generator with Verifier){

  private def message(request: HttpRequest) = IO {
    (List(request.method.value, request.uri.toString()) ++
     request.headers.collect({
       case header if RequestSigner.headers.contains(header.name()) => s"${header.name()}:${header.value()}"
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

  override def sign(request: HttpRequest): IO[HttpRequest] =
    message(request)
      .map(calculateSignature)
      .map(signature =>
        request.withHeaders(request.headers :+ SignatureHeader(signature))
      )

  override def verify(request: HttpRequest): IO[SignatureVerificationResult] =
    request.headers.find(_.name() == RequestSigner.signatureHeaderName).map(signature =>
      message(request).map( message =>
        verifySignature(message, signature.value())
      )).getOrElse(IO.pure(SignatureMissing))
}

