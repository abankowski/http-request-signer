# Http Requests Signer

Available implementations for http4s and akka-http are published as separate jars. Both are cross compiled for Scala 2.12 and 2.13.

Current implementation is a draft. Signature with DSA is hardcoded.

## Usage

Prepare `DSAParameters` that will be used as a constructor argument. Provide implicit ContextShift[IO] in the scope.
For **akka-http** get Materializer.
  
  
    val signer = Http4sRequestSigner(params: DSAParameters)
  
  
On the client side, once you have your request you can inject a signature by calling sign method:

    // http4s
    signer.sign(request: Request[IO]): IO[Request[IO]]
    
    // akka-http
    signer.sign(request: HttpRequest): IO[HttpRequest] 
    
On the server side, grab decoded request:

    // http4s
    signer.verify(request: Request[IO]): IO[SignatureVerificationResult] 
    
    // akka-http
    signer.verify(request: HttpRequest): IO[SignatureVerificationResult]

`SignatureVerificationResult` is defined as below:

    sealed trait SignatureVerificationResult
    case object SignatureValid extends SignatureVerificationResult
    case object SignatureMissing extends SignatureVerificationResult
    case object SignatureMalformed extends SignatureVerificationResult
    case object SignatureInvalid extends SignatureVerificationResult
