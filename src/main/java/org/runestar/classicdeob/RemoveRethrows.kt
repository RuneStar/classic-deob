package org.runestar.classicdeob

import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode

object RemoveRethrows : Transformer.Single {

    override fun transform(klass: ClassNode): ClassNode {
        for (m in klass.methods) {
            m.tryCatchBlocks.removeIf { it.type == "java/lang/RuntimeException" || it.handler.next.opcode == Opcodes.ATHROW }
        }
        return RemoveDeadCode.transform(klass)
    }
}