package pl.abankowski.requestsigner.signature.rsa

import org.bouncycastle.crypto.params.AsymmetricKeyParameter
import pl.abankowski.requestsigner.signature.RsaSHA512Generator

class RsaGenerator(override val privKey: AsymmetricKeyParameter) extends RsaSHA512Generator
