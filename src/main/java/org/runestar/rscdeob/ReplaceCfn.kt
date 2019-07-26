package org.runestar.rscdeob

import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodInsnNode

object ReplaceCfn : Transformer.Single {

    override fun transform(klass: ClassNode): ClassNode {
        klass.methods.forEach { m ->
            val insns = m.instructions
            val itr = insns.iterator()
            while (itr.hasNext()) {
                val insn = itr.next()
                if (!itr.hasNext()) return@forEach
                if (insn.opcode != Opcodes.LDC) continue
                insn as LdcInsnNode
                val cst = insn.cst as? String ?: continue

                val next = itr.next()
                if (next.opcode != Opcodes.INVOKESTATIC) continue
                next as MethodInsnNode

                if (next.owner != "java/lang/Class") continue
                if (next.name != "forName") continue
                if (next.desc != "(Ljava/lang/String;)Ljava/lang/Class;") continue

                insns.remove(insn)
                insns.set(next, LdcInsnNode(Type.getObjectType(cst.replace('.', '/'))))
            }
        }
        return klass
    }
}