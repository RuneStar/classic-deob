package org.runestar.rscdeob

import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode

object RemoveRethrows : Transformer.Single {

    override fun transform(klass: ClassNode): ClassNode {
        for (m in klass.methods) {
//            println(klass.name + " "+ m.name +" " + m.desc)
            val handlers = m.tryCatchBlocks.mapTo(HashSet()) { it.handler }
            for (handler in handlers) {
                var insns = m.instructions.toArray().asList()
                val start = insns.indexOf(handler) + 1
                insns = insns.subList(start, insns.size)
                insns = insns.takeWhile { it.type != AbstractInsnNode.LABEL }
                if (insns.isEmpty() || insns.last().opcode != Opcodes.ATHROW) continue
                insns.forEach { m.instructions.remove(it) }
            }
            m.tryCatchBlocks.removeIf { it.handler in handlers }
        }
        return klass
    }
}