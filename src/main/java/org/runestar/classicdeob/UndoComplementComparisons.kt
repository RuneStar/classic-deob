package org.runestar.classicdeob

import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.InsnNode
import org.objectweb.asm.tree.JumpInsnNode
import org.objectweb.asm.tree.analysis.Analyzer
import org.objectweb.asm.tree.analysis.SourceInterpreter
import java.lang.AssertionError

object UndoComplementComparisons : Transformer.Single() {

    override fun transform(klass: ClassNode) {
        for (m in klass.methods) {
            val frames = Analyzer(SourceInterpreter()).analyze(klass.name, m)
            val insns = m.instructions.toArray()
            insns.forEachIndexed { i, insn ->
                val frame = frames[i]
                when (insn.opcode) {
                    IFLE, IFEQ, IFGE, IFNE, IFLT, IFGT -> {
                        val na = frame.getStack(frame.stackSize - 1).insns.singleOrNull() ?: return@forEachIndexed
                        if (!isInv(na)) return@forEachIndexed

                        m.instructions.insert(na, InsnNode(ICONST_M1))
                        inv(na, m.instructions)
                        insn as JumpInsnNode
                        val j = JumpInsnNode(insn.opcode + 6, insn.label)
                        m.instructions.set(insn, j)
                        flip(j, m.instructions)
                    }
                    IF_ICMPEQ, IF_ICMPGE, IF_ICMPGT, IF_ICMPLT, IF_ICMPNE, IF_ICMPLE -> {
                        val na = frame.getStack(frame.stackSize - 1).insns.singleOrNull() ?: return@forEachIndexed
                        val nb = frame.getStack(frame.stackSize - 2).insns.singleOrNull() ?: return@forEachIndexed
                        if (!isInv(na) && !isInv(nb)) return@forEachIndexed
                        if (!isInv(na) && intValue(na) == null) return@forEachIndexed
                        if (!isInv(nb) && intValue(nb) == null) return@forEachIndexed

                        inv(na, m.instructions)
                        inv(nb, m.instructions)
                        flip(insn as JumpInsnNode, m.instructions)
                    }
                    LCMP -> {
                        val na = frame.getStack(frame.stackSize - 1).insns.singleOrNull() ?: return@forEachIndexed
                        val nb = frame.getStack(frame.stackSize - 2).insns.singleOrNull() ?: return@forEachIndexed
                        if (!isInvLong(na) && !isInvLong(nb)) return@forEachIndexed
                        if (!isInvLong(na) && longValue(na) == null) return@forEachIndexed
                        if (!isInvLong(nb) && longValue(nb) == null) return@forEachIndexed

                        invLong(na, m.instructions)
                        invLong(nb, m.instructions)
                        flipLong(insn, m.instructions)
                    }
                }
            }
        }
    }

    private fun isInv(n: AbstractInsnNode): Boolean {
        return n.opcode == IXOR && n.previous.opcode == ICONST_M1
    }

    private fun isInvLong(n: AbstractInsnNode): Boolean {
        return n.opcode == LXOR && longValue(n.previous) == -1L
    }

    private fun flip(insn: JumpInsnNode, insns: InsnList) {
        insns.set(insn, JumpInsnNode(flip(insn.opcode), insn.label))
    }

    private fun flipLong(insn: AbstractInsnNode, insns: InsnList) {
        flip(insn.next as JumpInsnNode, insns)
    }

    private fun flip(opcode: Int): Int {
        return when (opcode) {
            IF_ICMPEQ, IF_ICMPNE, IFEQ, IFNE -> opcode
            IF_ICMPGT -> IF_ICMPLT
            IF_ICMPLT -> IF_ICMPGT
            IF_ICMPGE ->  IF_ICMPLE
            IF_ICMPLE -> IF_ICMPGE
            IFLT -> IFGT
            IFLE -> IFGE
            IFGT -> IFLT
            IFGE -> IFLE
            else -> throw AssertionError()
        }
    }

    private fun inv(insn: AbstractInsnNode, insns: InsnList) {
        if (insn.opcode == IXOR) {
            insns.remove(insn.previous)
            insns.remove(insn)
        } else {
            insns.set(insn, loadInt(intValue(insn)!!.inv()))
        }
    }

    private fun invLong(insn: AbstractInsnNode, insns: InsnList) {
        if (insn.opcode == LXOR) {
            insns.remove(insn.previous)
            insns.remove(insn)
        } else {
            insns.set(insn, loadLong(longValue(insn)!!.inv()))
        }
    }
}