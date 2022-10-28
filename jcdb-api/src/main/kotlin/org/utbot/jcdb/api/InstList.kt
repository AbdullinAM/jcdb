package org.utbot.jcdb.api

private const val offset = "  "

class JcRawInstList(
    instructions: List<JcRawInst> = emptyList(),
    tryCatchBlocks: List<JcRawTryCatchBlock> = emptyList()
) {
    private val instructions_ = instructions.toMutableList()
    private val tryCatchBlocks = tryCatchBlocks.toMutableList()

    override fun toString(): String = instructions_.joinToString(
        prefix = "\n--------------------\n",
        postfix = "\n--------------------\n",
        separator = "\n"
    ) + if (tryCatchBlocks.isNotEmpty()) tryCatchBlocks.joinToString(
        postfix = "\n--------------------\n",
        separator = "\n"
    ) else ""
}

data class JcRawTryCatchBlock(
    val throwable: TypeName,
    val handler: JcRawLabelInst,
    val startInclusive: JcRawLabelInst,
    val endExclusive: JcRawLabelInst
) {
    override fun toString(): String = "${handler.name} catch $throwable: ${startInclusive.name} - ${endExclusive.name}"
}

sealed interface JcRawInst {
    val operands: List<JcRawExpr>
}

enum class IdentityType {
    INSTANCE, ARGUMENT, LOCAL
}

data class JcRawIdentityInst(
    val operand: JcRawValue,
    val type: IdentityType
) : JcRawInst {
    override val operands: List<JcRawExpr>
        get() = listOf(operand)

    override fun toString(): String = "$offset$operand = ${type.name.lowercase()}"
}

data class JcRawAssignInst(
    val lhv: JcRawValue,
    val rhv: JcRawExpr
) : JcRawInst {
    override val operands: List<JcRawExpr>
        get() = listOf(lhv, rhv)

    override fun toString(): String = "$offset$lhv = $rhv"
}

data class JcRawEnterMonitorInst(
    val monitor: JcRawValue
) : JcRawInst {
    override val operands: List<JcRawExpr>
        get() = listOf(monitor)

    override fun toString(): String = "${offset}enter monitor $monitor"
}

data class JcRawExitMonitorInst(
    val monitor: JcRawValue
) : JcRawInst {
    override val operands: List<JcRawExpr>
        get() = listOf(monitor)

    override fun toString(): String = "${offset}exit monitor $monitor"
}

data class JcRawCallInst(
    val callExpr: JcRawCallExpr
) : JcRawInst {
    override val operands: List<JcRawExpr>
        get() = listOf(callExpr)

    override fun toString(): String = "$offset$callExpr"
}

data class JcRawLabelInst(
    val name: String
) : JcRawInst {
    override val operands: List<JcRawExpr>
        get() = emptyList()

    override fun toString(): String = "label $name:"
}

data class JcRawReturnInst(
    val returnValue: JcRawValue?
) : JcRawInst {
    override val operands: List<JcRawExpr>
        get() = listOfNotNull(returnValue)

    override fun toString(): String = "${offset}return" + (returnValue?.let { " $it" } ?: "")
}

data class JcRawThrowInst(
    val throwable: JcRawValue
) : JcRawInst {
    override val operands: List<JcRawExpr>
        get() = listOf(throwable)

    override fun toString(): String = "${offset}throw $throwable"
}

data class JcRawCatchInst(
    val throwable: JcRawValue
) : JcRawInst {
    override val operands: List<JcRawExpr>
        get() = listOf(throwable)

    override fun toString(): String = "${offset}catch $throwable"
}

sealed interface JcRawBranchingInst : JcRawInst {
    val successors: List<JcRawLabelInst>
}

data class JcRawGotoInst(
    val target: JcRawLabelInst
) : JcRawBranchingInst {
    override val operands: List<JcRawExpr>
        get() = emptyList()

    override val successors: List<JcRawLabelInst>
        get() = listOf(target)

    override fun toString(): String = "${offset}goto ${target.name}"
}

data class JcRawIfInst(
    val condition: JcRawValue,
    val trueBranch: JcRawLabelInst,
    val falseBranch: JcRawLabelInst
) : JcRawBranchingInst {
    override val operands: List<JcRawExpr>
        get() = listOf(condition)

    override val successors: List<JcRawLabelInst>
        get() = listOf(trueBranch, falseBranch)

    override fun toString(): String = "${offset}if ($condition) goto ${trueBranch.name} else ${falseBranch.name}"
}

data class JcRawSwitchInst(
    val key: JcRawValue,
    val branches: Map<JcRawValue, JcRawLabelInst>,
    val default: JcRawLabelInst
) : JcRawBranchingInst {
    override val operands: List<JcRawExpr>
        get() = listOf(key) + branches.keys

    override val successors: List<JcRawLabelInst>
        get() = branches.values + default

    override fun toString(): String = buildString {
        appendLine("${offset}switch ($key) {")
        branches.forEach { (option, label) -> appendLine("$offset  $option -> ${label.name}") }
        appendLine("$offset  else -> ${default.name}")
        append("${offset}}")
    }
}

sealed interface JcRawExpr {
    val operands: List<JcRawValue>
}

data class JcRawAddExpr(
    val lhv: JcRawValue,
    val rhv: JcRawValue
) : JcRawExpr {
    override val operands: List<JcRawValue>
        get() = listOf(lhv, rhv)

    override fun toString(): String = "$lhv + $rhv"
}

data class JcRawAndExpr(
    val lhv: JcRawValue,
    val rhv: JcRawValue
) : JcRawExpr {
    override val operands: List<JcRawValue>
        get() = listOf(lhv, rhv)

    override fun toString(): String = "$lhv & $rhv"
}

data class JcRawCmpExpr(
    val lhv: JcRawValue,
    val rhv: JcRawValue
) : JcRawExpr {
    override val operands: List<JcRawValue>
        get() = listOf(lhv, rhv)

    override fun toString(): String = "$lhv cmp $rhv"
}

data class JcRawCmpgExpr(
    val lhv: JcRawValue,
    val rhv: JcRawValue
) : JcRawExpr {
    override val operands: List<JcRawValue>
        get() = listOf(lhv, rhv)

    override fun toString(): String = "$lhv cmpg $rhv"
}

data class JcRawCmplExpr(
    val lhv: JcRawValue,
    val rhv: JcRawValue
) : JcRawExpr {
    override val operands: List<JcRawValue>
        get() = listOf(lhv, rhv)

    override fun toString(): String = "$lhv cmpl $rhv"
}

data class JcRawDivExpr(
    val lhv: JcRawValue,
    val rhv: JcRawValue
) : JcRawExpr {
    override val operands: List<JcRawValue>
        get() = listOf(lhv, rhv)

    override fun toString(): String = "$lhv / $rhv"
}

data class JcRawMulExpr(
    val lhv: JcRawValue,
    val rhv: JcRawValue
) : JcRawExpr {
    override val operands: List<JcRawValue>
        get() = listOf(lhv, rhv)

    override fun toString(): String = "$lhv * $rhv"
}

data class JcRawEqExpr(
    val lhv: JcRawValue,
    val rhv: JcRawValue
) : JcRawExpr {
    override val operands: List<JcRawValue>
        get() = listOf(lhv, rhv)

    override fun toString(): String = "$lhv == $rhv"
}

data class JcRawNeqExpr(
    val lhv: JcRawValue,
    val rhv: JcRawValue
) : JcRawExpr {
    override val operands: List<JcRawValue>
        get() = listOf(lhv, rhv)

    override fun toString(): String = "$lhv != $rhv"
}

data class JcRawGeExpr(
    val lhv: JcRawValue,
    val rhv: JcRawValue
) : JcRawExpr {
    override val operands: List<JcRawValue>
        get() = listOf(lhv, rhv)

    override fun toString(): String = "$lhv >= $rhv"
}

data class JcRawGtExpr(
    val lhv: JcRawValue,
    val rhv: JcRawValue
) : JcRawExpr {
    override val operands: List<JcRawValue>
        get() = listOf(lhv, rhv)

    override fun toString(): String = "$lhv > $rhv"
}

data class JcRawLeExpr(
    val lhv: JcRawValue,
    val rhv: JcRawValue
) : JcRawExpr {
    override val operands: List<JcRawValue>
        get() = listOf(lhv, rhv)

    override fun toString(): String = "$lhv <= $rhv"
}

data class JcRawLtExpr(
    val lhv: JcRawValue,
    val rhv: JcRawValue
) : JcRawExpr {
    override val operands: List<JcRawValue>
        get() = listOf(lhv, rhv)

    override fun toString(): String = "$lhv < $rhv"
}

data class JcRawOrExpr(
    val lhv: JcRawValue,
    val rhv: JcRawValue
) : JcRawExpr {
    override val operands: List<JcRawValue>
        get() = listOf(lhv, rhv)

    override fun toString(): String = "$lhv | $rhv"
}

data class JcRawRemExpr(
    val lhv: JcRawValue,
    val rhv: JcRawValue
) : JcRawExpr {
    override val operands: List<JcRawValue>
        get() = listOf(lhv, rhv)

    override fun toString(): String = "$lhv % $rhv"
}

data class JcRawShlExpr(
    val lhv: JcRawValue,
    val rhv: JcRawValue
) : JcRawExpr {
    override val operands: List<JcRawValue>
        get() = listOf(lhv, rhv)

    override fun toString(): String = "$lhv << $rhv"
}

data class JcRawShrExpr(
    val lhv: JcRawValue,
    val rhv: JcRawValue
) : JcRawExpr {
    override val operands: List<JcRawValue>
        get() = listOf(lhv, rhv)

    override fun toString(): String = "$lhv >> $rhv"
}

data class JcRawSubExpr(
    val lhv: JcRawValue,
    val rhv: JcRawValue
) : JcRawExpr {
    override val operands: List<JcRawValue>
        get() = listOf(lhv, rhv)

    override fun toString(): String = "$lhv - $rhv"
}

data class JcRawUshrExpr(
    val lhv: JcRawValue,
    val rhv: JcRawValue
) : JcRawExpr {
    override val operands: List<JcRawValue>
        get() = listOf(lhv, rhv)

    override fun toString(): String = "$lhv u<< $rhv"
}

data class JcRawXorExpr(
    val lhv: JcRawValue,
    val rhv: JcRawValue
) : JcRawExpr {
    override val operands: List<JcRawValue>
        get() = listOf(lhv, rhv)

    override fun toString(): String = "$lhv ^ $rhv"
}

data class JcRawLengthExpr(
    val array: JcRawValue
) : JcRawExpr {
    override val operands: List<JcRawValue>
        get() = listOf(array)

    override fun toString(): String = "$array.length"
}

data class JcRawNegExpr(
    val operand: JcRawValue
) : JcRawExpr {
    override val operands: List<JcRawValue>
        get() = listOf(operand)

    override fun toString(): String = "-$operand"
}

data class JcRawCastExpr(
    val operand: JcRawValue,
    val targetType: TypeName
) : JcRawExpr {
    override val operands: List<JcRawValue>
        get() = listOf(operand)

    override fun toString(): String = "($targetType) $operand"
}

data class JcRawNewExpr(
    val targetType: TypeName
) : JcRawExpr {
    override val operands: List<JcRawValue>
        get() = emptyList()

    override fun toString(): String = "new $targetType"
}

data class JcRawNewArrayExpr(
    val dimensions: List<JcRawValue>,
    val targetType: TypeName
) : JcRawExpr {
    constructor(length: JcRawValue, targetType: TypeName) : this(listOf(length), targetType)

    override val operands: List<JcRawValue>
        get() = dimensions

    override fun toString(): String = "new $targetType${dimensions.joinToString() { "[$it]" }}"
}

data class JcRawInstanceOfExpr(
    val operand: JcRawValue,
    val targetType: TypeName
) : JcRawExpr {
    override val operands: List<JcRawValue>
        get() = listOf(operand)

    override fun toString(): String = "$operand instanceof $targetType"
}

sealed interface JcRawCallExpr : JcRawExpr {
    val declaringClass: TypeName
    val methodName: String
    val methodDesc: String
    val args: List<JcRawValue>

    override val operands: List<JcRawValue>
        get() = args
}

data class MethodHandle(
    val tag: Int,
    val declaringClass: TypeName,
    val methodName: String,
    val methodDesc: String,
    val isInterface: Boolean
)

data class JcRawDynamicCallExpr(
    override val declaringClass: TypeName,
    override val methodName: String,
    override val methodDesc: String,
    override val args: List<JcRawValue>,
    val bsm: MethodHandle,
    val bsmArgs: List<Any>
) : JcRawCallExpr

data class JcRawVirtualCallExpr(
    override val declaringClass: TypeName,
    override val methodName: String,
    override val methodDesc: String,
    val instance: JcRawValue,
    override val args: List<JcRawValue>,
) : JcRawCallExpr {
    override fun toString(): String = "$instance.$methodName${args.joinToString(prefix = "(", postfix = ")", separator = ", ")}"
}

data class JcRawInterfaceCallExpr(
    override val declaringClass: TypeName,
    override val methodName: String,
    override val methodDesc: String,
    val instance: JcRawValue,
    override val args: List<JcRawValue>,
) : JcRawCallExpr {
    override fun toString(): String = "$instance.$methodName${args.joinToString(prefix = "(", postfix = ")", separator = ", ")}"
}

data class JcRawStaticCallExpr(
    override val declaringClass: TypeName,
    override val methodName: String,
    override val methodDesc: String,
    override val args: List<JcRawValue>,
) : JcRawCallExpr {
    override fun toString(): String = "$declaringClass.$methodName${args.joinToString(prefix = "(", postfix = ")", separator = ", ")}"
}

data class JcRawSpecialCallExpr(
    override val declaringClass: TypeName,
    override val methodName: String,
    override val methodDesc: String,
    val instance: JcRawValue,
    override val args: List<JcRawValue>,
) : JcRawCallExpr {
    override fun toString(): String = "$instance.$methodName${args.joinToString(prefix = "(", postfix = ")", separator = ", ")}"
}


sealed class JcRawValue(open val typeName: TypeName) : JcRawExpr {
    override val operands: List<JcRawValue>
        get() = emptyList()
}

data class JcRawThis(override val typeName: TypeName) : JcRawValue(typeName) {
    override fun toString(): String = "this"
}
data class JcRawArgument(val index: Int, val name: String?, override val typeName: TypeName) : JcRawValue(typeName) {
    override fun toString(): String = name ?: "arg$$index"
}
data class JcRawLocal(val name: String, override val typeName: TypeName) : JcRawValue(typeName) {
    override fun toString(): String = name
}

data class JcRawRegister(val index: Int, override val typeName: TypeName) : JcRawValue(typeName) {
    override fun toString(): String = "%$index"
}

data class JcRawFieldRef(
    val instance: JcRawValue?,
    val declaringClass: TypeName,
    val fieldName: String,
    override val typeName: TypeName
) : JcRawValue(typeName) {
    constructor(declaringClass: TypeName, fieldName: String, typeName: TypeName) : this(
        null,
        declaringClass,
        fieldName,
        typeName
    )

    override fun toString(): String = "${instance ?: declaringClass}.$fieldName"
}

data class JcRawArrayAccess(
    val array: JcRawValue,
    val index: JcRawValue,
    override val typeName: TypeName
) : JcRawValue(typeName) {
    override fun toString(): String = "$array[$index]"
}

sealed class JcRawConstant(typeName: TypeName) : JcRawValue(typeName)

data class JcRawBool(val value: Boolean, override val typeName: TypeName) : JcRawConstant(typeName) {
    override fun toString(): String = "$value"
}
data class JcRawByte(val value: Byte, override val typeName: TypeName) : JcRawConstant(typeName) {
    override fun toString(): String = "$value"
}
data class JcRawChar(val value: Char, override val typeName: TypeName) : JcRawConstant(typeName) {
    override fun toString(): String = "$value"
}
data class JcRawShort(val value: Short, override val typeName: TypeName) : JcRawConstant(typeName) {
    override fun toString(): String = "$value"
}
data class JcRawInt(val value: Int, override val typeName: TypeName) : JcRawConstant(typeName) {
    override fun toString(): String = "$value"
}
data class JcRawLong(val value: Long, override val typeName: TypeName) : JcRawConstant(typeName) {
    override fun toString(): String = "$value"
}
data class JcRawFloat(val value: Float, override val typeName: TypeName) : JcRawConstant(typeName) {
    override fun toString(): String = "$value"
}
data class JcRawDouble(val value: Double, override val typeName: TypeName) : JcRawConstant(typeName) {
    override fun toString(): String = "$value"
}

data class JcRawNullConstant(override val typeName: TypeName) : JcRawConstant(typeName) {
    override fun toString(): String = "null"
}
data class JcRawStringConstant(val value: String, override val typeName: TypeName) : JcRawConstant(typeName) {
    override fun toString(): String = "\"$value\""
}
data class JcRawClassConstant(val className: TypeName, override val typeName: TypeName) : JcRawConstant(typeName) {
    override fun toString(): String = "$className.class"
}
data class JcRawMethodConstant(
    val declaringClass: TypeName,
    val name: String,
    val argumentTypes: List<TypeName>,
    val returnType: TypeName,
    override val typeName: TypeName
) : JcRawConstant(typeName) {
    override fun toString(): String = "$declaringClass.$name${argumentTypes.joinToString(prefix = "(", postfix = ")")}:$returnType"
}
