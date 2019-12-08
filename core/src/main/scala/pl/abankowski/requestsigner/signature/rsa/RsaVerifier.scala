package pl.abankowski.requestsigner.signature.rsa

import org.bouncycastle.crypto.params.AsymmetricKeyParameter
import pl.abankowski.requestsigner.signature.RsaSHA512Verifier

class RsaVerifier(override val pubKey: AsymmetricKeyParameter) extends RsaSHA512Verifier
