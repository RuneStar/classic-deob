package org.runestar.classicdeob

import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.analysis.Analyzer
import org.objectweb.asm.tree.analysis.BasicInterpreter
import java.lang.Exception

object RemoveDeadCode : Transformer.Single() {

    override fun transform(klass: ClassNode) {
        for (m in klass.methods) {
            var changed = true // todo
            while (changed) {
                changed = false
                try {
                    val frames = Analyzer(BasicInterpreter()).analyze(klass.name, m)
                    val insns = m.instructions.toArray()
                    for (i in frames.indices) {
                        if (frames[i] == null) {
                            changed = true
                            m.instructions.remove(insns[i])
                        }
                    }
                } catch (e: Exception) {
                    throw Exception("${klass.name}.${m.name}${m.desc}", e)
                }
            }
        }
    }
}