package org.runestar.classicdeob

import org.objectweb.asm.commons.ClassRemapper
import org.objectweb.asm.commons.Remapper
import org.objectweb.asm.tree.ClassNode

object Rename : Transformer.Single, Remapper() {

    private val names = javaClass.getResource("names.csv")
            .openStream()
            .bufferedReader()
            .readLines()
            .associate { it.split(',').let { it[0] to it[1] } }

    override fun transform(klass: ClassNode): ClassNode {
        val newKlass = ClassNode()
        klass.accept(ClassRemapper(newKlass, this))
        return newKlass
    }

    override fun map(internalName: String): String {
        return names[internalName] ?: internalName
    }

    override fun mapFieldName(owner: String, name: String, descriptor: String): String {
        return names["$owner.$name"] ?: name
    }

    override fun mapMethodName(owner: String, name: String, descriptor: String): String {
        return names["$owner.$name$descriptor"] ?: name
    }
}