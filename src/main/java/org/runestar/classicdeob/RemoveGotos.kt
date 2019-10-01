package org.runestar.classicdeob

import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.JumpInsnNode

object RemoveGotos : Transformer.Single() {

    override fun transform(klass: ClassNode) {
        for (m in klass.methods) {
            val insns = m.instructions.iterator()
            for (insn in insns) {
                val next = insn.next ?: break
                if (insn.opcode == Opcodes.GOTO && insn is JumpInsnNode && insn.label == next) {
                    insns.remove()
                }
            }
        }
    }
}