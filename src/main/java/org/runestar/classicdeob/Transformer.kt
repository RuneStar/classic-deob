package org.runestar.classicdeob

import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.ClassNode

interface Transformer {

    fun transform(classes: List<ByteArray>): List<ByteArray>

    abstract class Tree : Transformer {

        final override fun transform(classes: List<ByteArray>): List<ByteArray> {
            val nodes = classes.map { ClassNode(it, ClassReader.SKIP_FRAMES or ClassReader.SKIP_DEBUG) }
            transform(nodes)
            nodes.forEach { analyze(it) }
            return nodes.map { it.toByteArray() }
        }

        abstract fun transform(klasses: List<ClassNode>)
    }

    abstract class Single : Tree() {

        final override fun transform(klasses: List<ClassNode>) {
            klasses.forEach { transform(it) }
        }

        abstract fun transform(klass: ClassNode)
    }

    class Composite(vararg val transformers: Transformer) : Transformer {

        override fun transform(classes: List<ByteArray>): List<ByteArray> {
            var cs = classes
            transformers.forEach { cs = it.transform(cs) }
            return cs
        }
    }
}