package org.runestar.classicdeob

import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.VarInsnNode

object RemoveUnusedMath : Transformer.Single {

    override fun transform(klass: ClassNode): ClassNode {
        out@
        for (m in klass.methods) {
            val loads = HashSet<Int>()
            val stores = HashMap<Int, VarInsnNode>()
            for (insn in m.instructions) {
                if (insn !is VarInsnNode) continue
                when (insn.opcode) {
                    ILOAD -> loads.add(insn.`var`)
                    ISTORE -> stores[insn.`var`] = insn
                }
            }
            val unusedStores = stores.filter { it.key !in loads }.map { it.value }
            if (unusedStores.isEmpty()) continue
            val store = unusedStores.single()
            val prevs = m.instructions.toArray().takeWhile { it != store }.takeLast(7)
            for (p in prevs) {
                when (p.opcode) {
                    ILOAD, IADD, IDIV, ISUB, IREM, BIPUSH,
                    ICONST_0, ICONST_1, ICONST_2, ICONST_3, ICONST_4, ICONST_5, ICONST_M1 -> {}
                    else -> continue@out
                }
            }
            for (p in prevs) {
                m.instructions.remove(p)
            }
            m.instructions.remove(store)
        }
        return klass
    }
}