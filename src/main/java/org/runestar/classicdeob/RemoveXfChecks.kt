package org.runestar.classicdeob

import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.IincInsnNode
import org.objectweb.asm.tree.InsnNode
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.VarInsnNode

object RemoveXfChecks : Transformer.Tree() {

    override fun transform(klasses: List<ClassNode>) {
        val fieldNames = HashSet<String>()
        for (c in klasses) {
            for (m in c.methods) {
                val insn0 = m.instructions.first ?: continue
                val insn1 = insn0.next ?: continue
                if (insn0.opcode != Opcodes.GETSTATIC) continue
                insn0 as FieldInsnNode
                if (insn1.opcode != Opcodes.ISTORE) continue
                insn1 as VarInsnNode
                if (insn1.`var` == 0 && m.maxLocals != 1) continue
                if (insn0.owner.length <= 3) continue

                if (insn1.`var` == m.maxLocals - 1) m.maxLocals--
                fieldNames.add("${insn0.owner}.${insn0.name}")
                m.instructions.remove(insn0)
                m.instructions.remove(insn1)

                for (insn in m.instructions) {
                    if (insn.opcode == Opcodes.PUTSTATIC && insn is FieldInsnNode && insn.owner == insn0.owner && insn0.name == insn.name) {
                        m.instructions.set(insn, InsnNode(Opcodes.POP))
                    }
                }

                val insns = m.instructions.iterator()
                for (insn in insns) {
                    if (insn is VarInsnNode && insn.opcode == Opcodes.ILOAD && insn.`var` == insn1.`var`) {
                        insns.remove()
                        if (insns.next() is LabelNode) insns.next()
                        insns.remove()
                    } else if (insn is IincInsnNode && insn.`var` == insn1.`var`) {
                        insns.remove()
                    }
                }
            }
        }
        val fieldName = fieldNames.singleOrNull() ?: throw IllegalStateException(fieldNames.toString())
        for (c in klasses) {
            for (m in c.methods) {
                val insns = m.instructions.iterator()
                for (insn in insns) {
                    if (insn.opcode == Opcodes.GETSTATIC && insn is FieldInsnNode && "${insn.owner}.${insn.name}" == fieldName) {
                        insns.remove()
                        if (insns.next() is LabelNode) insns.next()
                        insns.remove()
                    }
                }
            }
        }
        RemoveDeadCode.transform(klasses)
        RemoveGotos.transform(klasses)
    }
}