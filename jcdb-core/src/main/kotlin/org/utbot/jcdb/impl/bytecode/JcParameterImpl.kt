package org.utbot.jcdb.impl.bytecode

import org.utbot.jcdb.api.JcAnnotation
import org.utbot.jcdb.api.JcDeclaration
import org.utbot.jcdb.api.JcMethod
import org.utbot.jcdb.api.JcParameter
import org.utbot.jcdb.api.TypeName
import org.utbot.jcdb.impl.types.ParameterInfo
import org.utbot.jcdb.impl.types.TypeNameImpl

class JcParameterImpl(
    override val method: JcMethod,
    private val info: ParameterInfo
) : JcParameter {

    override val access: Int
        get() = info.access

    override val name: String?
        get() = info.name

    override val index: Int
        get() = info.index

    override val declaration: JcDeclaration
        get() = JcDeclarationImpl.of(method.enclosingClass.declaration.location, this)

    override val annotations: List<JcAnnotation>
        get() = info.annotations.map { JcAnnotationImpl(it, method.enclosingClass.classpath) }

    override val type: TypeName
        get() = TypeNameImpl(info.type)

}