package org.runestar.rscdeob

import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.InsnNode
import org.objectweb.asm.tree.IntInsnNode
import org.objectweb.asm.tree.LdcInsnNode

fun loadInt(n: Int): AbstractInsnNode {
    return when (n) {
        in -1..5 -> InsnNode(n + 3)
        in Byte.MIN_VALUE..Byte.MAX_VALUE -> IntInsnNode(BIPUSH, n)
        in Short.MIN_VALUE..Short.MAX_VALUE -> IntInsnNode(SIPUSH, n)
        else -> LdcInsnNode(n)
    }
}

fun loadLong(n: Long): AbstractInsnNode {
    return when (n) {
        in 0..1 -> InsnNode((n + 9).toInt())
        else -> LdcInsnNode(n)
    }
}

fun intValue(insn: AbstractInsnNode): Int? {
    if (insn.opcode in 2..8) return insn.opcode - 3
    if (insn.opcode == BIPUSH || insn.opcode == SIPUSH) return (insn as IntInsnNode).operand
    if (insn is LdcInsnNode && insn.cst is Int) return insn.cst as Int
    return null
}

fun longValue(insn: AbstractInsnNode): Long? {
    if (insn.opcode in 9..10) return (insn.opcode - 9).toLong()
    if (insn is LdcInsnNode && insn.cst is Long) return insn.cst as Long
    return null
}

fun integerValue(insn: AbstractInsnNode): Number? = intValue(insn) ?: longValue(insn)