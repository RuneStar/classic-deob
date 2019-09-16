package org.runestar.classicdeob

import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode

object RemoveRethrows : Transformer.Single {

    override fun transform(klass: ClassNode): ClassNode {
        for (m in klass.methods) {
            m.tryCatchBlocks.removeIf { it.type == "java/lang/RuntimeException" }
            val tcbs = m.tryCatchBlocks.iterator()
            for (tcb in tcbs) {
                if (tcb.handler.next.opcode == Opcodes.ATHROW) {
                    tcbs.remove()
                }
            }
        }
        return klass
    }
}