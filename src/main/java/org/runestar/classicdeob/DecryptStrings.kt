package org.runestar.classicdeob

import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodNode
import java.lang.AssertionError

object DecryptStrings : Transformer.Single {

    override fun transform(klass: ClassNode): ClassNode {
        val m1 = findDecryptMethod1(klass) ?: return klass
        val m2 = findDecryptMethod2(klass) ?: throw AssertionError()
        val key1 = findKey1(m1)
        val key2 = findKey2(m2)
        val clinit = klass.methods.first { it.name == "<clinit>" }
        decryptLoads(clinit, key1, key2)
        inlineStringArray(klass, clinit)
        inlineSingleString(klass, clinit)
        return klass
    }

    private fun decryptLoads(m: MethodNode, key1: Int, key2: IntArray) {
        val insns = m.instructions.iterator()
        for (insn in insns) {
            if (insn is LdcInsnNode && insn.cst is String && insn.cst != "" && insn.next.opcode == INVOKESTATIC) {
                val loadDecrypted = LdcInsnNode(decrypt(insn.cst as String, key1, key2))
                m.instructions.insertBefore(insn, loadDecrypted)
                insns.remove()
                insns.next()
                insns.remove()
                insns.next()
                insns.remove()
            }
        }
    }

    private fun inlineStringArray(klass: ClassNode, clinit: MethodNode) {
        val stringsField = klass.fields.firstOrNull { it.desc == "[Ljava/lang/String;" && it.access == (ACC_PRIVATE or ACC_STATIC or ACC_FINAL) } ?: return
        var zinsns = clinit.instructions.toArray().reversed()
        val start = zinsns.indexOfFirst { it is FieldInsnNode && it.name == stringsField.name && it.opcode == PUTSTATIC && it.desc == stringsField.desc }
        zinsns = zinsns.subList(start, zinsns.size)
        val end = zinsns.indexOfFirst { it.opcode == ANEWARRAY }
        zinsns = zinsns.subList(0, end + 2)
        val strings = zinsns.reversed().filter { it.opcode == LDC }.map { (it as LdcInsnNode).cst as String }
        zinsns.forEach { clinit.instructions.remove(it) }
        klass.fields.remove(stringsField)
        for (m in klass.methods) {
            val insns = m.instructions.iterator()
            for (insn in insns) {
                if (insn is FieldInsnNode && insn.name == stringsField.name && insn.desc == stringsField.desc && insn.owner == klass.name) {
                    val idx = checkNotNull(intValue(insn.next))
                    m.instructions.insertBefore(insn, LdcInsnNode(strings[idx]))
                    insns.remove()
                    insns.next()
                    insns.remove()
                    insns.next()
                    insns.remove()
                }
            }
        }
    }

    private fun inlineSingleString(klass: ClassNode, clinit: MethodNode) {
        val stringField = klass.fields.firstOrNull { it.desc == "Ljava/lang/String;" && it.access == (ACC_PRIVATE or ACC_STATIC or ACC_FINAL) } ?: return
        val putStatic = clinit.instructions.toArray().first { it is FieldInsnNode && it.name == stringField.name && it.desc == stringField.desc }
        val s = (putStatic.previous as LdcInsnNode).cst as String
        clinit.instructions.remove(putStatic.previous)
        clinit.instructions.remove(putStatic)
        klass.fields.remove(stringField)
        for (m in klass.methods) {
            for (insn in m.instructions) {
                if (insn is FieldInsnNode && insn.opcode == GETSTATIC && insn.name == stringField.name && insn.desc == stringField.desc) {
                    m.instructions.set(insn, LdcInsnNode(s))
                }
            }
        }
    }

    private fun findDecryptMethod1(klass: ClassNode): MethodNode? {
        val ms = klass.methods.iterator()
        for (m in ms) {
            if (m.name == "z" && m.desc == "(Ljava/lang/String;)[C") {
                ms.remove()
                return m
            }
        }
        return null
    }

    private fun findDecryptMethod2(klass: ClassNode): MethodNode? {
        val ms = klass.methods.iterator()
        for (m in ms) {
            if (m.name == "z" && m.desc == "([C)Ljava/lang/String;") {
                ms.remove()
                return m
            }
        }
        return null
    }

    private fun findKey1(m: MethodNode): Int {
        for (insn in m.instructions) {
            if (insn.opcode == CALOAD) return checkNotNull(intValue(insn.next))
        }
        throw AssertionError()
    }

    private fun findKey2(m: MethodNode): IntArray {
        val keys = IntArray(5)
        var i = -2
        for (insn in m.instructions) {
            val v = intValue(insn)
            if (v != null) {
                if (i >= 0) keys[i] = v
                i++
            }
        }
        return keys
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