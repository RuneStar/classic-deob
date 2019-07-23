package org.runestar.rscdeob

import org.objectweb.asm.tree.ClassNode

interface Transformer {

    fun transform(klasses: Collection<ClassNode>): Collection<ClassNode>

    interface Single : Transformer {

        override fun transform(klasses: Collection<ClassNode>): Collection<ClassNode> = klasses.map { transform(it) }

        fun transform(klass: ClassNode): ClassNode
    }

    class Composite(vararg val transformers: Transformer) : Transformer {

        override fun transform(klasses: Collection<ClassNode>): Collection<ClassNode> {
            var ks = klasses
            transformers.forEach { ks = it.transform(ks) }
            return ks
        }
    }
}