package pl.abankowski.httpsigner.signature.rsa

import org.bouncycastle.crypto.params.AsymmetricKeyParameter
import pl.abankowski.httpsigner.signature.RsaSHA512Verifier

class RsaVerifier(override val pubKey: AsymmetricKeyParameter) extends RsaSHA512Verifier
