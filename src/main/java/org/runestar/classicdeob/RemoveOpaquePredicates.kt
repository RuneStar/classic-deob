package org.runestar.classicdeob

import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.IincInsnNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.JumpInsnNode
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.VarInsnNode
import org.objectweb.asm.tree.analysis.Analyzer
import org.objectweb.asm.tree.analysis.SourceInterpreter
import java.lang.AssertionError
import java.lang.reflect.Modifier

object RemoveOpaquePredicates : Transformer.Tree() {

    override fun transform(klasses: List<ClassNode>) {
        var changed = true
        var j = 0
        while (changed) {
            changed = false
            val ops = findOpParams(klasses)
            val constantArgs = findConstantArgs(klasses)
            val changedMethods = HashSet<MethodNode>()
            for ((p, pu) in ops) {
                val argVals = constantArgs[p] ?: continue
                if (pu.method in changedMethods) continue
                if (pu is ParamUsage.Unused || (pu is ParamUsage.Condition && argVals.all { passes(it.value, pu.value, pu.cmp) })) {
                    changed = true
                    j++
                    val newType = Type.getMethodType(p.methodDesc).removeArgumentAt(p.param)
                    val newDesc = newType.descriptor
                    var local = 0
                    val argTypes = newType.argumentTypes
                    for (i in 0 until p.param) local += argTypes[i].size

                    argVals.forEach {
                        it.invoke.desc = newDesc
                        //it.invoke.name += j
                        it.insns.remove(it.load)
                    }

                    pu.method.desc = newDesc
                    //pu.method.name += j
                    removeLocal(pu.method, local)
                    changedMethods.add(pu.method)

                    if (pu is ParamUsage.Condition) {
                        var jmp = pu.iload.next
                        if (jmp !is JumpInsnNode) jmp = jmp.next
                        jmp as JumpInsnNode
                        pu.method.instructions.insert(pu.iload, JumpInsnNode(GOTO, jmp.label))
                        pu.method.instructions.remove(pu.iload)
                    }
                }
            }
            RemoveDeadCode.transform(klasses)
        }
        RemoveGotos.transform(klasses)
    }

    private fun findOpParams(klasses: Collection<ClassNode>): Map<ParamId, ParamUsage> {
        val ops = HashMap<ParamId, ParamUsage>()
        for (c in klasses) {
            for (m in c.methods) {
                if (!Modifier.isStatic(m.access)) continue
                val args = Type.getArgumentTypes(m.desc)
                if (args.none { isValidOpType(it) }) continue

                var local = 0
                val localToArg = HashMap<Int, Int>()
                args.forEachIndexed { i, t ->
                    localToArg[local] = i
                    local += t.size
                }
                val maxArgLocal = local

                val stores = HashSet<Int>()
                val loads = HashMap<Int, MutableList<VarInsnNode>>()

                for (insn in m.instructions) {
                    if (insn !is VarInsnNode || insn.`var` > maxArgLocal) continue
                    when (insn.opcode) {
                        ILOAD -> loads.computeIfAbsent(insn.`var`) { ArrayList() }.add(insn)
                        ISTORE -> stores.add(insn.`var`)
                    }
                }

                for (loc in 0..maxArgLocal) {
                    val p = localToArg[loc] ?: continue
                    if (loc !in stores && loc !in loads) ops[ParamId(c.name, m.name, m.desc, p)] = ParamUsage.Unused(m)
                }

                val candidateLoads = loads.filter { it.key !in stores && it.value.size == 1 }.map { it.value.single() }

                loop@
                for (load in candidateLoads) {
                    val next = load.next
                    var cst = 0
                    var cmp = next.opcode + 6
                    var jmp: JumpInsnNode
                    if (next.opcode in IFEQ..IFLE) {
                        jmp = next as JumpInsnNode
                    } else if (intValue(next) != null) {
                        cst = intValue(next)!!
                        val next2 = next.next
                        if (next2.opcode in IF_ICMPEQ..IF_ICMPLE) {
                            jmp = next2 as JumpInsnNode
                            cmp = jmp.opcode
                        } else {
                            continue@loop
                        }
                    } else {
                        continue@loop
                    }
                    var label = jmp.next
                    while (label !is LabelNode) {
                        label = label.next ?: continue@loop
                    }
                    if (label != jmp.label) continue@loop

                    ops[ParamId(c.name, m.name, m.desc, localToArg.getValue(load.`var`))] = ParamUsage.Condition(load, cst, cmp, m)
                }
            }
        }
        return ops
    }

    private data class ParamId(
            val className: String,
            val methodName: String,
            val methodDesc: String,
            val param: Int
    )

    private data class ArgVal(
            val value: Int,
            val load: AbstractInsnNode,
            val invoke: MethodInsnNode,
            val insns: InsnList
    )

    private sealed class ParamUsage {

        abstract val method: MethodNode

        data class Unused(
                override val method: MethodNode
        ) : ParamUsage()

        data class Condition(
                val iload: VarInsnNode,
                val value: Int,
                val cmp: Int,
                override val method: MethodNode
        ) : ParamUsage()

    }

    private fun findConstantArgs(klasses: Collection<ClassNode>): Map<ParamId, Set<ArgVal>> {
        val args = HashMap<ParamId, MutableSet<ArgVal?>>()
        for (c in klasses) {
            for (m in c.methods) {
                val frames = Analyzer(SourceInterpreter()).analyze(c.name, m)
                val insns = m.instructions.toArray()
                insns.forEachIndexed { i, insn ->
                    if (insn.opcode != INVOKESTATIC) return@forEachIndexed
                    insn as MethodInsnNode
                    val frame = frames[i] ?: return@forEachIndexed
                    val stack = frame.getStackLast(Type.getArgumentTypes(insn.desc).size)
                    stack.forEachIndexed { j, sv ->
                        val src = sv.insns.singleOrNull() ?: return@forEachIndexed
                        val cst = intValue(src)
                        val av = if (cst == null) null else ArgVal(cst, src, insn, m.instructions)
                        args.computeIfAbsent(ParamId(insn.owner, insn.name, insn.desc, j)) { HashSet() }.add(av)
                    }
                }
            }
        }
        args.values.removeIf { it.any { it == null } }
        return args as Map<ParamId, Set<ArgVal>>
    }

    private fun passes(left: Int, right: Int, cmp: Int) = when (cmp) {
        IF_ICMPEQ -> left == right
        IF_ICMPGE -> left >= right
        IF_ICMPLE -> left <= right
        IF_ICMPGT -> left > right
        IF_ICMPLT -> left < right
        IF_ICMPNE -> left != right
        else -> throw AssertionError()
    }


    private fun isValidOpType(t: Type) = when (t) {
        Type.INT_TYPE, Type.BYTE_TYPE, Type.BOOLEAN_TYPE -> true
        else -> false
    }

    private fun removeLocal(m: MethodNode, local: Int) {
        m.maxLocals--
        for (insn in m.instructions) {
            when (insn) {
                is VarInsnNode -> if (insn.`var` > local) insn.`var`--
                is IincInsnNode -> if (insn.`var` > local) insn.`var`--
            }
        }
    }
}