package org.runestar.classicdeob

import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LdcInsnNode

object MaskShifts : Transformer.Single {

    override fun transform(klass: ClassNode): ClassNode {
        for (m in klass.methods) {
            loop@ for (insn in m.instructions) {
                if (insn !is LdcInsnNode) continue
                val cst = insn.cst as? Int ?: continue
                val next = insn.next
                val loadMasked = when (next.opcode) {
                    Opcodes.ISHL, Opcodes.ISHR, Opcodes.IUSHR -> loadInt(cst and 0x1f)
                    Opcodes.LSHL, Opcodes.LSHR, Opcodes.LUSHR -> loadInt(cst and 0x3f)
                    else -> continue@loop
                }
                m.instructions.set(insn, loadMasked)
            }
        }
        return klass
    }
}