package org.runestar.rscdeob

import org.objectweb.asm.Opcodes.GETSTATIC
import org.objectweb.asm.Opcodes.PUTSTATIC
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import java.lang.reflect.Modifier

object FieldResolver : Transformer {

    override fun transform(klasses: Collection<ClassNode>): Collection<ClassNode> {
        val resolver = Resolver(klasses)
        klasses.forEach { cn ->
            cn.methods.forEach { mn ->
                mn.instructions.iterator().forEach { insn ->
                    if (insn is FieldInsnNode) {
                        val old = insn.owner
                        insn.owner = resolver.getOwner(
                            insn.owner, insn.name, insn.desc,
                            insn.opcode == GETSTATIC || insn.opcode == PUTSTATIC
                        )
                    }
                }
            }
        }

        return klasses
    }

    private class Resolver(classNodes: Collection<ClassNode>) {

        private val classNodesByNames = classNodes.associateBy { it.name }

        fun getOwner(owner: String, name: String, desc: String, isStatic: Boolean): String {
            var cn = classNodesByNames[owner] ?: return owner
            while (true) {
                if (cn.hasDeclaredField(name, desc, isStatic)) {
                    return cn.name
                }
                val superName = cn.superName
                cn = classNodesByNames[superName] ?: return superName
            }
        }

        private fun ClassNode.hasDeclaredField(name: String, desc: String, isStatic: Boolean): Boolean {
            return fields.any {
                it.name == name && it.desc == desc && Modifier.isStatic(it.access) == isStatic
            }
        }
    }
}