package org.runestar.classicdeob

import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.analysis.Analyzer
import org.objectweb.asm.tree.analysis.BasicInterpreter
import java.lang.Exception
import java.nio.file.Path

interface Transformer {

    fun transform(dir: Path) {
        val classes = readClasses(dir)
        val classes2 = transform(classes)
        writeClasses(classes2, dir)
        for (c in classes2) {
            for (m in c.methods) {
                try {
                    Analyzer(BasicInterpreter()).analyze(c.name, m)
                } catch (e: Exception) {
                    System.err.println("${c.name}.${m.name}${m.desc}")
                    throw e
                }
            }
        }
    }

    fun transform(klasses: Collection<ClassNode>): Collection<ClassNode>

    interface Single : Transformer {

        override fun transform(klasses: Collection<ClassNode>): Collection<ClassNode> = klasses.map { transform(it) }

        fun transform(klass: ClassNode): ClassNode
    }

    class Composite(vararg val transformers: Transformer) : Transformer {

        override fun transform(dir: Path) {
            transformers.forEach { it.transform(dir) }
        }

        override fun transform(klasses: Collection<ClassNode>): Collection<ClassNode> {
            var ks = klasses
            transformers.forEach { ks = it.transform(ks) }
            return ks
        }
    }
}