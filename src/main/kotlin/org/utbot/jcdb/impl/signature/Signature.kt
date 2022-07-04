package org.utbot.jcdb.impl.signature

import org.objectweb.asm.signature.SignatureReader
import org.objectweb.asm.signature.SignatureVisitor

abstract class Signature<T : Resolution>: GenericTypeRegistrant.RejectingSignatureVisitor(), GenericTypeRegistrant {

    protected val typeVariables = ArrayList<FormalTypeVariable>()
    protected var currentTypeParameter: String? = null
    protected var currentBounds: MutableList<GenericType>? = null

    override fun visitFormalTypeParameter(name: String) {
        collectTypeParameter()
        currentTypeParameter = name
        currentBounds = ArrayList()
    }

    override fun visitClassBound(): SignatureVisitor {
        return GenericTypeExtractor(this)
    }

    override fun visitInterfaceBound(): SignatureVisitor {
        return GenericTypeExtractor(this)
    }

    override fun register(token: GenericType) {
        checkNotNull(currentBounds) { "Did not expect $token before finding formal parameter" }
        currentBounds!!.add(token)
    }

    protected fun collectTypeParameter() {
        if (currentTypeParameter != null) {
            typeVariables.add(Formal(currentTypeParameter, currentBounds))
        }
    }

    abstract fun resolve(): T

    companion object {
        fun <S : Resolution> extract(genericSignature: String?, visitor: Signature<S>): S {
            val signatureReader = SignatureReader(genericSignature)
            signatureReader.accept(visitor)
            return visitor.resolve()
        }
    }
}