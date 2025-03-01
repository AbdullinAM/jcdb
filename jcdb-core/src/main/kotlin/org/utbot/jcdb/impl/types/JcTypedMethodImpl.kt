package org.utbot.jcdb.impl.types

import org.objectweb.asm.Type
import org.objectweb.asm.tree.LocalVariableNode
import org.utbot.jcdb.api.JcClassOrInterface
import org.utbot.jcdb.api.JcMethod
import org.utbot.jcdb.api.JcRefType
import org.utbot.jcdb.api.JcType
import org.utbot.jcdb.api.JcTypeVariableDeclaration
import org.utbot.jcdb.api.JcTypedMethod
import org.utbot.jcdb.api.JcTypedMethodParameter
import org.utbot.jcdb.api.MethodResolution
import org.utbot.jcdb.api.ext.findClass
import org.utbot.jcdb.api.isStatic
import org.utbot.jcdb.api.throwClassNotFound
import org.utbot.jcdb.impl.types.signature.FieldResolutionImpl
import org.utbot.jcdb.impl.types.signature.FieldSignature
import org.utbot.jcdb.impl.types.signature.MethodResolutionImpl
import org.utbot.jcdb.impl.types.signature.MethodSignature
import org.utbot.jcdb.impl.types.substition.JcSubstitutor

class JcTypedMethodImpl(
    override val enclosingType: JcRefType,
    override val method: JcMethod,
    private val parentSubstitutor: JcSubstitutor
) : JcTypedMethod {

    private class TypedMethodInfo(
        val substitutor: JcSubstitutor,
        private val resolution: MethodResolution
    ) {
        val impl: MethodResolutionImpl? get() = resolution as? MethodResolutionImpl
    }

    private val classpath = method.enclosingClass.classpath

    private val info by lazy(LazyThreadSafetyMode.NONE) {
        val signature = MethodSignature.withDeclarations(method)
        val impl = signature as? MethodResolutionImpl
        val substitutor = if (!method.isStatic) {
            parentSubstitutor.newScope(impl?.typeVariables.orEmpty())
        } else {
            JcSubstitutor.empty.newScope(impl?.typeVariables.orEmpty())
        }

        TypedMethodInfo(
            substitutor = substitutor,
            resolution = MethodSignature.withDeclarations(method)
        )
    }

    override val name: String
        get() = method.name

    override val typeParameters: List<JcTypeVariableDeclaration>
        get() {
            val impl = info.impl ?: return emptyList()
            return impl.typeVariables.map { it.asJcDeclaration(method) }
        }

    override val exceptions: List<JcClassOrInterface>
        get() {
            val impl = info.impl ?: return emptyList()
            return impl.exceptionTypes.map {
                classpath.findClass(it.name)
            }
        }

    override val typeArguments: List<JcRefType>
        get() {
            return emptyList()
        }

    override val parameters: List<JcTypedMethodParameter>
        get() {
            val methodInfo = info
            return method.parameters.mapIndexed { index, jcParameter ->
                JcTypedMethodParameterImpl(
                    enclosingMethod = this,
                    substitutor = methodInfo.substitutor,
                    parameter = jcParameter,
                    jvmType = methodInfo.impl?.parameterTypes?.get(index)
                )
            }
        }

    override val returnType: JcType by lazy(LazyThreadSafetyMode.NONE) {
        val typeName = method.returnType.typeName
        val info = info
        val impl = info.impl
        if (impl == null) {
            classpath.findTypeOrNull(typeName)
                ?: throw IllegalStateException("Can't resolve type by name $typeName")
        } else {
            classpath.typeOf(info.substitutor.substitute(impl.returnType))
        }
    }

    override fun typeOf(inst: LocalVariableNode): JcType {
        val variableSignature =
            FieldSignature.of(inst.signature, method.allVisibleTypeParameters()) as? FieldResolutionImpl
        if (variableSignature == null) {
            val type = Type.getType(inst.desc)
            return classpath.findTypeOrNull(type.className) ?: type.className.throwClassNotFound()
        }
        val info = info
        return classpath.typeOf(info.substitutor.substitute(variableSignature.fieldType))
    }

}