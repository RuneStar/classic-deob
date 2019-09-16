package org.runestar.classicdeob

import org.jd.core.v1.api.printer.Printer

class JdPrinter : Printer {

    private val TAB = "\t"

    private val NEWLINE = "\n"

    private var indentationCount = 0

    private var sb = StringBuilder()

    override fun toString(): String {
        return sb.toString()

    }
    override fun start(maxLineNumber: Int, majorVersion: Int, minorVersion: Int) {}

    override fun end() {}

    override fun printText(text: String?) {
        sb.append(text)
    }
    override fun printNumericConstant(constant: String) {
        sb.append(constant)
    }

    override fun printStringConstant(constant: String, ownerInternalName: String?) {
        sb.append(constant)
    }

    override fun printKeyword(keyword: String) {
        sb.append(keyword)
    }

    override fun printDeclaration(flags: Int, internalTypeName: String, name: String, descriptor: String?) {
        sb.append(name)
    }

    override fun printReference(flags: Int, internalTypeName: String, name: String, descriptor: String?, ownerInternalName: String?) {
        sb.append(name)
    }

    override fun indent() {
        this.indentationCount++
    }

    override fun unindent() {
        if (this.indentationCount > 0) this.indentationCount--
    }

    override fun startLine(lineNumber: Int) {
        for (i in 0 until indentationCount) sb.append(TAB)
    }

    override fun endLine() {
        sb.append(NEWLINE)
    }

    override fun extraLine(count: Int) {
        var count = count
        while (count-- > 0) sb.append(NEWLINE)
    }

    override fun startMarker(type: Int) {}

    override fun endMarker(type: Int) {}
}