package org.runestar.classicdeob

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.InsnNode
import org.objectweb.asm.tree.IntInsnNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.analysis.Analyzer
import org.objectweb.asm.tree.analysis.BasicInterpreter
import org.objectweb.asm.tree.analysis.Frame
import org.objectweb.asm.tree.analysis.Value
import org.zeroturnaround.zip.ByteSource
import org.zeroturnaround.zip.ZipUtil
import java.lang.Exception
import java.nio.file.Path
import java.util.stream.Stream

fun loadInt(n: Int): AbstractInsnNode = when (n) {
    in -1..5 -> InsnNode(n + 3)
    in Byte.MIN_VALUE..Byte.MAX_VALUE -> IntInsnNode(BIPUSH, n)
    in Short.MIN_VALUE..Short.MAX_VALUE -> IntInsnNode(SIPUSH, n)
    else -> LdcInsnNode(n)
}

fun loadLong(n: Long): AbstractInsnNode = when (n) {
    0L, 1L -> InsnNode((n + 9).toInt())
    else -> LdcInsnNode(n)
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

fun <V : Value> Frame<V>.getStackLast(count: Int) = List(count) { getStack(stackSize - count + it) }

fun Type.removeArgumentAt(index: Int): Type {
    val args = argumentTypes.toMutableList()
    args.removeAt(index)
    return Type.getMethodType(returnType, *args.toTypedArray())
}

fun <T> Stream<T>.forEachClose(action: (T) -> Unit) {
    forEach(action)
    close()
}

fun readClasses(jar: Path): List<ByteArray> {
    val classes = ArrayList<ByteArray>()
    ZipUtil.iterate(jar.toFile()) { input, entry ->
        if (!entry.name.endsWith(".class")) return@iterate
        classes.add(input.readAllBytes())
    }
    return classes
}

fun writeClasses(classes: Iterable<ByteArray>, jar: Path) {
    ZipUtil.pack(classes.map { ByteSource("${ClassReader(it).className}.class", it) }.toTypedArray(), jar.toFile())
}

fun analyze(classNode: ClassNode) {
    for (m in classNode.methods) {
        try {
            Analyzer(BasicInterpreter()).analyze(classNode.name, m)
        } catch (e: Exception) {
            throw Exception("${classNode.name}.${m.name}${m.desc}", e)
        }
    }
}

fun ClassNode(classFile: ByteArray, parsingOptions: Int): ClassNode {
    val c = ClassNode()
    ClassReader(classFile).accept(c, parsingOptions)
    return c
}

fun ClassNode.toByteArray(): ByteArray {
    val w = ClassWriter(0)
    accept(w)
    return w.toByteArray()
}