package org.runestar.classicdeob

import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.InsnNode
import org.objectweb.asm.tree.analysis.Analyzer
import org.objectweb.asm.tree.analysis.SourceInterpreter

object FixNegatives : Transformer.Single() {

    override fun transform(klass: ClassNode) {
        for (m in klass.methods) {
            val frames = Analyzer(SourceInterpreter()).analyze(klass.name, m)
            val insns = m.instructions.toArray()
            insns.forEachIndexed { i, insn ->
                val frame = frames[i]
                when (insn.opcode) {
                    Opcodes.IADD -> {
                        val right = frame.getStack(frame.stackSize - 1).insns.singleOrNull() ?: return@forEachIndexed
                        val left = frame.getStack(frame.stackSize - 2).insns.singleOrNull() ?: return@forEachIndexed

                        if (right.opcode == Opcodes.INEG) {
                            m.instructions.remove(right)
                            m.instructions.set(insn, InsnNode(Opcodes.ISUB))
                        } else if (intValue(right) ?: 0 < 0) {
                            m.instructions.set(right, loadInt(-intValue(right)!!))
                            m.instructions.set(insn, InsnNode(Opcodes.ISUB))
                        } else if (intValue(left) ?: 0 < 0) {
                            m.instructions.insertBefore(insn, loadInt(-intValue(left)!!))
                            m.instructions.remove(left)
                            m.instructions.set(insn, InsnNode(Opcodes.ISUB))
                        }
                    }
                    Opcodes.LADD -> {
                        val right = frame.getStack(frame.stackSize - 1).insns.singleOrNull() ?: return@forEachIndexed
                        val left = frame.getStack(frame.stackSize - 2).insns.singleOrNull() ?: return@forEachIndexed

                        if (right.opcode == Opcodes.LNEG) {
                            m.instructions.remove(right)
                            m.instructions.set(insn, InsnNode(Opcodes.LSUB))
                        } else if (longValue(right) ?: 0 < 0) {
                            m.instructions.set(right, loadLong(-longValue(right)!!))
                            m.instructions.set(insn, InsnNode(Opcodes.LSUB))
                        } else if (longValue(left) ?: 0 < 0) {
                            m.instructions.insertBefore(insn, loadLong(-longValue(left)!!))
                            m.instructions.remove(left)
                            m.instructions.set(insn, InsnNode(Opcodes.LSUB))
                        }
                    }
                    Opcodes.ISUB -> {
                        val right = frame.getStack(frame.stackSize - 1).insns.singleOrNull() ?: return@forEachIndexed
                        // val left = frame.getStack(frame.stackSize - 2).insns.singleOrNull() ?: return@forEachIndexed

                        if (right.opcode == Opcodes.INEG) {
                            m.instructions.remove(right)
                            m.instructions.set(insn, InsnNode(Opcodes.IADD))
                        } else if (intValue(right) ?: 0 < 0) {
                            m.instructions.set(right, loadInt(-intValue(right)!!))
                            m.instructions.set(insn, InsnNode(Opcodes.IADD))
                        }
                    }
                    Opcodes.LSUB -> {
                        val right = frame.getStack(frame.stackSize - 1).insns.singleOrNull() ?: return@forEachIndexed
                        // val left = frame.getStack(frame.stackSize - 2).insns.singleOrNull() ?: return@forEachIndexed

                        if (right.opcode == Opcodes.LNEG) {
                            m.instructions.remove(right)
                            m.instructions.set(insn, InsnNode(Opcodes.LADD))
                        } else if (longValue(right) ?: 0 < 0) {
                            m.instructions.set(right, loadLong(-longValue(right)!!))
                            m.instructions.set(insn, InsnNode(Opcodes.LADD))
                        }
                    }
                }
            }
        }
    }
}