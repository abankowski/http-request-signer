package pl.abankowski.requestsigner

import java.io.ByteArrayOutputStream

import org.bouncycastle.crypto.params.DSAParameters

import scala.concurrent.duration._
import scala.language.{higherKinds, postfixOps}

class AkkaRequestSigner(cipher: DSAParameters)(ctx: ContextShift[IO])
  extends AbstractRequestSigner[HttpRequest, IO](cipher){

  private def message(request: HttpRequest) = IO {
    (List(request.method.value, request.uri.toString()) ++
     request.headers.collect({
       case header if headers.contains(header.name()) => s"${header.name()}:${header.value()}"
     })).foldLeft(new ByteArrayOutputStream())({ (buffer, value) =>
      buffer.write(value.getBytes)
      buffer
    })
  }.flatMap { content =>
    IO.fromFuture(IO {
      request.entity.toStrict(10 seconds)
    }).map {e =>
      content.write(e.getData().toArray)
      content.toByteArray
    }
  }

  override def sign(request: HttpRequest): IO[HttpRequest] =
    message(request)
      .map(calculateSignature)
      .map(signature =>
        request.withHeaders(
          request.headers :+ new HttpHeader {
              override def name(): String = signatureHeaderName
              override def value(): String = signature
              override def lowercaseName(): String = name().toLowerCase
              override def renderInRequests(): Boolean = true
              override def renderInResponses(): Boolean = false
            }
        )
      )

  override def verify(request: HttpRequest): IO[SignatureVerificationResult] = {
    request.headers.find(_.name() == signatureHeaderName).map(signature =>
      message(request).map( message =>
        verifySignature(message, signature.value())
      )).getOrElse(IO.pure(SignatureMissing))
  }
}

