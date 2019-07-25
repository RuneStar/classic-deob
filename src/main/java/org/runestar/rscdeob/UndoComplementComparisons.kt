package org.runestar.rscdeob

import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type.*
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.JumpInsnNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.analysis.Analyzer
import org.objectweb.asm.tree.analysis.BasicInterpreter
import org.objectweb.asm.tree.analysis.BasicValue
import org.objectweb.asm.tree.analysis.Frame
import java.lang.AssertionError

object UndoComplementComparisons : Transformer.Single {

    override fun transform(klass: ClassNode): ClassNode {
        for (m in klass.methods) {
            val frames = Analyzer(BasicInterpreter()).analyze(klass.name, m)
            val insns = m.instructions.toArray()
            insns.forEachIndexed { i, insn ->
                when (insn.opcode) {
                    IF_ICMPEQ, IF_ICMPGE, IF_ICMPGT, IF_ICMPLT, IF_ICMPNE, IF_ICMPLE, LCMP -> {
                        val candidates = ArrayList<AbstractInsnNode>()
                        var k = i - 1
                        while (candidates.size < 2) {
                            val f1 = frames[k]
                            val f2 = frames[k + 1]
                            val n = insns[k]
                            if (stacked(f2) && ((isXor(n) && n.previous != null && isMinus1(n.previous)) || integerValue(n) != null)) {
                                candidates.add(n)
                            } else if (f1.stackSize == 0) {
                                return@forEachIndexed
                            }
                            k--
                        }
                        if (candidates.none { isXor(it) }) return@forEachIndexed
                        for (c in candidates) {
                            val u = integerValue(c)
                            if (u != null) {
                                val rep = when (u) {
                                    is Int -> loadInt(u.inv())
                                    is Long -> loadLong(u.inv())
                                    else -> throw AssertionError()
                                }
                                m.instructions.set(c, rep)
                            } else if (isXor(c)) {
                                m.instructions.remove(c.previous)
                                m.instructions.remove(c)
                            } else {
                                error(c)
                            }
                        }
                        flip(insn, m.instructions)
                    }
                }
            }
        }
        return klass
    }

    private fun stacked(f: Frame<BasicValue>): Boolean {
        val stackSize = f.stackSize
        if (stackSize !in 1..2) return false
        val ts = List(stackSize) { f.getStack(it).type }
        return ts.all { it == INT_TYPE } || ts.all { it == LONG_TYPE }
    }

    private fun flip(insn: AbstractInsnNode, insns: InsnList) {
        val cmp = (if (insn.opcode == LCMP) insn.next else insn) as JumpInsnNode
        insns.set(cmp, JumpInsnNode(flip(cmp.opcode), cmp.label))
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

    private fun isXor(insn: AbstractInsnNode): Boolean {
        return insn.opcode == IXOR || insn.opcode == LXOR
    }

    private fun isMinus1(insn: AbstractInsnNode): Boolean {
        return insn.opcode == ICONST_M1 || (insn is LdcInsnNode && insn.cst == -1L)
    }
}