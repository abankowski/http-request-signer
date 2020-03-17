package pl.abankowski.httpsigner

import org.bouncycastle.crypto.digests.SHA512Digest
import org.bouncycastle.crypto.params.AsymmetricKeyParameter
import org.bouncycastle.crypto.signers.RSADigestSigner

package object signature {

  sealed trait Verifier {
    def verify(message: Array[Byte], signature: Array[Byte]): Boolean
  }

  sealed trait Generator {
    def signature(message: Array[Byte]): Array[Byte]
  }

  trait RsaSHA512Verifier extends Verifier {

    val pubKey: AsymmetricKeyParameter
    private val signer  = new RSADigestSigner(new SHA512Digest())

    override def verify(message: Array[Byte], signature: Array[Byte]): Boolean = {
      signer.init(false, pubKey)
      signer.update(message, 0, message.length)
      signer.verifySignature(signature)
    }
  }

  trait RsaSHA512Generator extends Generator {

    val privKey: AsymmetricKeyParameter
    private val signer  = new RSADigestSigner(new SHA512Digest())

    override def signature(message: Array[Byte]): Array[Byte] = {
      signer.init(true, privKey)
      signer.update(message, 0, message.length)
      signer.generateSignature()
    }
  }
}
