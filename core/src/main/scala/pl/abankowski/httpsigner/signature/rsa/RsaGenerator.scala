package pl.abankowski.httpsigner.signature.rsa

import org.bouncycastle.crypto.params.AsymmetricKeyParameter
import pl.abankowski.httpsigner.signature.RsaSHA512Generator

class RsaGenerator(override val privKey: AsymmetricKeyParameter) extends RsaSHA512Generator
