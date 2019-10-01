package org.runestar.classicdeob

import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode

object RemoveRethrows : Transformer.Single() {

    override fun transform(klass: ClassNode) {
        for (m in klass.methods) {
            m.tryCatchBlocks.removeIf { it.type == "java/lang/RuntimeException" || it.handler.next.opcode == Opcodes.ATHROW }
        }
        RemoveDeadCode.transform(klass)
    }
}