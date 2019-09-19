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
import org.objectweb.asm.tree.analysis.Frame
import org.objectweb.asm.tree.analysis.Value
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Stream

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

fun <V : Value> Frame<V>.getStackLast(count: Int): List<V> = List(count) { getStack(stackSize - count + it) }

fun Type.removeArgumentAt(index: Int): Type {
    val args = argumentTypes.toMutableList()
    args.removeAt(index)
    return Type.getMethodType(returnType, *args.toTypedArray())
}

fun <T> Stream<T>.forEachClose(action: (T) -> Unit) {
    forEach(action)
    close()
}

fun readClasses(dir: Path): Collection<ClassNode> {
    val classes = ArrayList<ClassNode>()
    Files.walk(dir).forEachClose { f ->
        if (Files.isDirectory(f) || !f.toString().endsWith(".class")) return@forEachClose
        val reader = ClassReader(Files.readAllBytes(f))
        val node = ClassNode()
        reader.accept(node, ClassReader.SKIP_FRAMES or ClassReader.SKIP_DEBUG)
        classes.add(node)
    }
    return classes
}

fun writeClasses(classes: Collection<ClassNode>, dir: Path) {
    classes.forEach { node ->
        val copy = ClassNode()
        node.accept(copy)
        val writer = ClassWriter(0)
        node.accept(writer)
        val file = dir.resolve(node.name + ".class")
        Files.createDirectories(file.parent)
        Files.write(file, writer.toByteArray())
    }
}