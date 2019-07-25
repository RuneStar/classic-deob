package org.runestar.rscdeob

import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LdcInsnNode

object ShiftFix : Transformer.Single {

    override fun transform(klass: ClassNode): ClassNode {
        for (m in klass.methods) {
            loop@ for (insn in m.instructions) {
                if (insn !is LdcInsnNode) continue
                val cst = insn.cst
                if (cst !is Int) continue
                val next = insn.next
                val o = when (next.opcode) {
                    Opcodes.ISHL, Opcodes.ISHR, Opcodes.IUSHR -> loadInt(cst and 0x1f)
                    Opcodes.LSHL, Opcodes.LSHR -> loadInt(cst and 0x3f)
                    else -> continue@loop
                }
                m.instructions.insertBefore(insn, o)
                m.instructions.remove(insn)
            }
        }
        return klass
    }
}