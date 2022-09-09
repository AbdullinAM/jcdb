package org.utbot.jcdb.impl.signature

import org.objectweb.asm.signature.SignatureVisitor
import org.utbot.jcdb.api.Classpath
import org.utbot.jcdb.api.Malformed
import org.utbot.jcdb.api.MethodResolution
import org.utbot.jcdb.api.Raw

open class MethodSignature(cp: Classpath) : Signature<MethodResolution>(cp) {

    private val parameterTypes = ArrayList<GenericType>()
    private val exceptionTypes = ArrayList<GenericType>()

    private lateinit var returnType: GenericType

    override fun visitParameterType(): SignatureVisitor {
        return GenericTypeExtractor(cp, ParameterTypeRegistrant())
    }

    override fun visitReturnType(): SignatureVisitor {
        collectTypeParameter()
        return GenericTypeExtractor(cp, ReturnTypeTypeRegistrant())
    }

    override fun visitExceptionType(): SignatureVisitor {
        return GenericTypeExtractor(cp, ExceptionTypeRegistrant())
    }

    override fun resolve(): MethodResolution {
        return MethodResolutionImpl(
            returnType,
            parameterTypes,
            exceptionTypes,
            typeVariables
        )
    }

    private inner class ParameterTypeRegistrant : GenericTypeRegistrant {
        override fun register(token: GenericType) {
            parameterTypes.add(token)
        }
    }

    private inner class ReturnTypeTypeRegistrant : GenericTypeRegistrant {
        override fun register(token: GenericType) {
            returnType = token
        }
    }

    private inner class ExceptionTypeRegistrant : GenericTypeRegistrant {
        override fun register(token: GenericType) {
            exceptionTypes.add(token)
        }
    }

    companion object {
        fun of(signature: String?, cp: Classpath): MethodResolution {
            signature ?: return Raw
            return try {
                of(signature, MethodSignature(cp))
            } catch (ignored: RuntimeException) {
                Malformed
            }
        }
    }
}