package org.runestar.classicdeob

import org.objectweb.asm.commons.Remapper

object Rename : Remapper() {

    override fun map(internalName: String): String {
        return if (isGamepackClass(internalName) && internalName.substringAfterLast('/').length <= 2) internalName + '_'
        else internalName
    }

    override fun mapMethodName(owner: String, name: String, descriptor: String): String {
        return if (isGamepackClass(owner) && name.length <= 2) name + descriptor.hashCode().toString().takeLast(3)
        else name
    }

    override fun mapFieldName(owner: String, name: String, descriptor: String): String {
        return if (isGamepackClass(owner) && name.length <= 2) '_' + name
        else  name
    }

    private fun isGamepackClass(internalName: String): Boolean {
        return !internalName.contains('/') ||
                internalName.startsWith("jaclib/") ||
                internalName.startsWith("jag")
    }
}