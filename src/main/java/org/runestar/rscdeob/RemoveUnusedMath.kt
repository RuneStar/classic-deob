package org.runestar.rscdeob

import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.VarInsnNode

object RemoveUnusedMath : Transformer.Single {

    override fun transform(klass: ClassNode): ClassNode {
        for (m in klass.methods) {
            val loads = HashSet<Int>()
            val stores = HashMap<Int, VarInsnNode>()
            for (insn in m.instructions) {
                if (insn !is VarInsnNode) continue
                when (insn.opcode) {
                    Opcodes.ILOAD -> loads.add(insn.`var`)
                    Opcodes.ISTORE -> stores[insn.`var`] = insn
                }
            }
            val store = stores.filter { it.key !in loads }.map { it.value }.singleOrNull() ?: continue
            repeat(7) {
                m.instructions.remove(store.previous)
            }
            m.instructions.remove(store)
        }
        return klass
    }
}