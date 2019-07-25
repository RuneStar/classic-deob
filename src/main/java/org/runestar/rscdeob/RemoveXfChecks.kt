package org.runestar.rscdeob

import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.VarInsnNode

object RemoveXfChecks : Transformer.Single {

    override fun transform(klass: ClassNode): ClassNode {
        for (m in klass.methods) {
            val insn0 = m.instructions.first ?: continue
            if (insn0 is FieldInsnNode && insn0.name == "xf" && insn0.owner == "client") {
                val store = (insn0.next as VarInsnNode)
                val slot = store.`var`
                if (klass.name != "fa") {
                    m.instructions.remove(insn0.next)
                    m.instructions.remove(insn0)
                    m.maxLocals--
                }
                val insns = m.instructions.iterator()
                for (insn in insns) {
                    if (insn is VarInsnNode && insn.opcode == Opcodes.ILOAD && insn.`var` == slot) {
                        val p2 = insn.previous.previous
                        if (p2 is FieldInsnNode && p2.name == "nb" && p2.owner == "vb") continue
                        val next = insn.next
                        insns.remove()
                        if (next.type == AbstractInsnNode.LABEL) {
                            insns.next()
                        }
                        insns.next()
                        insns.remove()
                    }
                }
            } else {
                val insns = m.instructions.iterator()
                for (insn in insns) {
                    if (insn is FieldInsnNode && insn.name == "xf" && insn.owner == "client") {
                        insns.remove()
                        insns.next()
                        insns.remove()
                    }
                }
            }
        }
        return klass
    }
}