package org.runestar.classicdeob

import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.analysis.Analyzer
import org.objectweb.asm.tree.analysis.BasicInterpreter

object RemoveDeadCode : Transformer.Single {

    override fun transform(klass: ClassNode): ClassNode {
        for (m in klass.methods) {
            val frames = Analyzer(BasicInterpreter()).analyze(klass.name, m)
            val insns = m.instructions.toArray()
            for (i in frames.indices) {
                if (frames[i] == null) {
//                    println("${klass.name} ${m.name} ${m.desc}")
                    m.instructions.remove(insns[i])
                }
            }
        }
        return klass
    }
}