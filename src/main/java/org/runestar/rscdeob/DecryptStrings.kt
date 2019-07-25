package org.runestar.rscdeob

import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.ACC_FINAL
import org.objectweb.asm.Opcodes.ACC_PRIVATE
import org.objectweb.asm.Opcodes.ACC_STATIC
import org.objectweb.asm.Opcodes.GETSTATIC
import org.objectweb.asm.Opcodes.ICONST_5
import org.objectweb.asm.Opcodes.ICONST_M1
import org.objectweb.asm.Opcodes.LDC
import org.objectweb.asm.Opcodes.PUTSTATIC
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodNode
import java.lang.AssertionError

object DecryptStrings : Transformer.Single {

    override fun transform(klass: ClassNode): ClassNode {
        val m1 = decrypt1(klass) ?: return klass
        val m2 = decrypt2(klass) ?: throw AssertionError()
        val key1 = key1(m1)
        val key2 = key2(m2)
        val clinit = klass.methods.first { it.name == "<clinit>" }
        val cinsns = clinit.instructions.iterator()
        for (insn in cinsns) {
            if (insn is LdcInsnNode && insn.cst is String) {
                if (insn.cst == "") continue
                val decrypted = decrypt(insn.cst as String, key1, key2)
                clinit.instructions.insertBefore(insn, LdcInsnNode(decrypted))
                cinsns.remove()
                cinsns.next()
                cinsns.remove()
                cinsns.next()
                cinsns.remove()
            }
        }
        val stringsField = klass.fields.firstOrNull { it.desc == "[Ljava/lang/String;" && it.access == (ACC_PRIVATE or ACC_STATIC or ACC_FINAL) }
        if (stringsField != null) {
            var zinsns = clinit.instructions.toArray().reversed()
            val start = zinsns.indexOfFirst { it is FieldInsnNode && it.name == stringsField.name && it.opcode == PUTSTATIC && it.desc == stringsField.desc }
            zinsns = zinsns.subList(start, zinsns.size)
            val end = zinsns.indexOfFirst { it.opcode == Opcodes.ANEWARRAY }
            zinsns = zinsns.subList(0, end + 2)
            val strings = zinsns.reversed().filter { it.opcode == LDC }.map { (it as LdcInsnNode).cst as String }
            zinsns.forEach { clinit.instructions.remove(it) }
            klass.fields.remove(stringsField)
            for (m in klass.methods) {
                val insns = m.instructions.iterator()
                for (insn in insns) {
                    if (insn is FieldInsnNode && insn.name == stringsField.name && insn.desc == stringsField.desc) {
                        val idx = intValue(insn.next)!!
                        val s = strings[idx]
                        m.instructions.insertBefore(insn, LdcInsnNode(s))
                        insns.remove()
                        insns.next()
                        insns.remove()
                        insns.next()
                        insns.remove()
                    }
                }
            }
        } else {
            val stringField = klass.fields.firstOrNull { it.desc == "Ljava/lang/String;" && it.access == (ACC_PRIVATE or ACC_STATIC or ACC_FINAL) }
            if (stringField != null) {
                val getStatic = clinit.instructions.toArray().asList().first { it is FieldInsnNode && it.name == stringField.name && it.desc == stringField.desc }
                val s = (getStatic.previous as LdcInsnNode).cst as String
                clinit.instructions.remove(getStatic.previous)
                clinit.instructions.remove(getStatic)
                klass.fields.remove(stringField)
                for (m in klass.methods) {
                    for (insn in m.instructions) {
                        if (insn is FieldInsnNode && insn.opcode == GETSTATIC && insn.name == stringField.name && insn.desc == stringField.desc) {
                            m.instructions.insertBefore(insn, LdcInsnNode(s))
                            m.instructions.remove(insn)
                        }
                    }
                }
            }
        }
        return klass
    }

    private fun decrypt1(klass: ClassNode): MethodNode? {
        val ms = klass.methods.iterator()
        for (m in ms) {
            if (m.name == "z" && m.desc == "(Ljava/lang/String;)[C") {
                ms.remove()
                return m
            }
        }
        return null
    }

    private fun decrypt2(klass: ClassNode): MethodNode? {
        val ms = klass.methods.iterator()
        for (m in ms) {
            if (m.name == "z" && m.desc == "([C)Ljava/lang/String;") {
                ms.remove()
                return m
            }
        }
        return null
    }

    private fun key1(m: MethodNode): Int {
        for (insn in m.instructions) {
            if (insn.opcode == Opcodes.CALOAD) {
                return intValue(insn.next)!!
            }
        }
        throw AssertionError()
    }

    private fun key2(m: MethodNode): IntArray {
        val keys = IntArray(5)
        var i = -2
        for (insn in m.instructions) {
            if (isIntValue(insn)) {
                if (i >= 0) keys[i] = intValue(insn)!!
                i++
            }
        }
        return keys
    }

    private fun isIntValue(insn: AbstractInsnNode): Boolean {
        return insn.opcode in ICONST_M1..ICONST_5 || insn.type == AbstractInsnNode.INT_INSN
    }

    private fun decrypt(s: String, key1: Int, key2: IntArray): String {
        val chars = s.toCharArray()
        if (chars.size == 1) chars[0] = (chars[0].toInt() xor key1).toChar()
        chars.forEachIndexed { i, c ->
            chars[i] = (c.toInt() xor key2[i % 5]).toChar()
        }
        return String(chars)
    }
}