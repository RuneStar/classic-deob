package org.runestar.classicdeob

import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.MethodNode

object RemoveCounters : Transformer {

    override fun transform(klasses: Collection<ClassNode>): Collection<ClassNode> {
        for (k in klasses) {
            val fields = k.fields.iterator()
            for (f in fields) {
                if (f.access != Opcodes.ACC_STATIC || f.desc != "I") continue
                val gets = ArrayList<MethodNode>()
                val puts = ArrayList<MethodNode>()
                for (c in klasses) {
                    for (m in c.methods) {
                        for (insn in m.instructions) {
                            if (insn is FieldInsnNode && insn.owner == k.name && insn.name == f.name) {
                                if (insn.opcode == Opcodes.GETSTATIC) gets.add(m)
                                else if (insn.opcode == Opcodes.PUTSTATIC) puts.add(m)
                            }
                        }
                    }
                }
                if (gets.size == 1 && gets == puts) {
                    val m = gets.single()
                    for (insn in m.instructions) {
                        if (insn !is FieldInsnNode || insn.opcode != Opcodes.GETSTATIC || insn.owner != k.name || insn.name != f.name) continue
                        val insn2 = insn.next
                        if (insn2.opcode != Opcodes.ICONST_1) continue
                        val insn3 = insn2.next
                        if (insn3.opcode != Opcodes.IADD) continue
                        val insn4 = insn3.next
                        if (insn4 !is FieldInsnNode || insn4.opcode != Opcodes.PUTSTATIC || insn4.owner != k.name || insn4.name != f.name) continue
                        fields.remove()
                        m.instructions.remove(insn)
                        m.instructions.remove(insn2)
                        m.instructions.remove(insn3)
                        m.instructions.remove(insn4)
                        break
                    }
                }
            }
        }
        return klasses
    }
}