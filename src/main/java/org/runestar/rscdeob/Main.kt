package org.runestar.rscdeob

import com.strobel.decompiler.Decompiler
import com.strobel.decompiler.DecompilerSettings
import com.strobel.decompiler.PlainTextOutput
import org.benf.cfr.reader.Main
import org.jd.core.v1.ClassFileToJavaSourceDecompiler
import org.jd.core.v1.api.loader.Loader
import org.jetbrains.java.decompiler.main.decompiler.ConsoleDecompiler
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode
import org.zeroturnaround.zip.ZipUtil
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

fun main() {
    deob(Paths.get("rsc.jar"))
}

private fun deob(src: Path) {
    val srcSimple = src.fileName.toString().substringBeforeLast('.')
    val tempInDir = Paths.get("temp-in")
    val tempOutDir = Paths.get("temp-out")
    val outputCfr = src.resolveSibling("$srcSimple-cfr")
    val outputFernflower = src.resolveSibling("$srcSimple-fernflower")
    val outputJd = src.resolveSibling("$srcSimple-jd")
    val outputProcyon = src.resolveSibling("$srcSimple-procyon")
    val outputJar = src.resolveSibling("out-" + src.fileName.toString())

    tempInDir.toFile().deleteRecursively()
    tempOutDir.toFile().deleteRecursively()
    outputCfr.toFile().deleteRecursively()
    outputFernflower.toFile().deleteRecursively()
    outputJd.toFile().deleteRecursively()
    outputProcyon.toFile().deleteRecursively()
    Files.deleteIfExists(outputJar)

    Files.createDirectories(tempInDir)
    Files.createDirectories(tempOutDir)
    Files.createDirectories(outputCfr)
    Files.createDirectories(outputFernflower)
    Files.createDirectories(outputJd)
    Files.createDirectories(outputProcyon)

    ZipUtil.unpack(src.toFile(), tempInDir.toFile())
    val transformer = Transformer.Composite(
            FieldResolver,
            MaskShifts,
            DecryptStrings,
            ReplaceCfn,
            RemoveRethrows,
            RemoveDeadCode,
            RemoveXfChecks,
            RemoveUnusedMath,
            UndoComplementComparisons,
            RemoveCounters,
            Rename
    )
    val classes = transformer.transform(readClasses(tempInDir))
    writeClasses(classes, tempOutDir)
    ZipUtil.pack(tempOutDir.toFile(), outputJar.toFile())

//    decompileCfr(outputJar, outputCfr)
//    decompileFernflower(tempOutDir, outputFernflower)
//    decompileJd(tempOutDir, outputJd)
//    decompileProcyon(tempOutDir, outputProcyon)
}

private fun decompileCfr(input: Path, output: Path) {
    Main.main(arrayOf(
        input.toString(),
        "--outputpath", output.toString()
    ))
}

private fun decompileFernflower(input: Path, output: Path) {
    ConsoleDecompiler.main(arrayOf(
        input.toString(),
        output.toString()
    ))
}

private fun decompileJd(input: Path, output: Path) {
    val loader = object : Loader {
        override fun canLoad(p0: String): Boolean = Files.exists(input.resolve("$p0.class"))
        override fun load(p0: String): ByteArray = Files.readAllBytes(input.resolve("$p0.class"))
    }
    Files.walk(input).forEach { f ->
        if (Files.isDirectory(f)) return@forEach
        val classSimpleName = f.fileName.toString().substringBeforeLast('.')
        val outFile = output.resolve(input.relativize(f)).resolveSibling("$classSimpleName.java")
        Files.createDirectories(outFile.parent)
        val printer = JdPrinter()
        val decompiler = ClassFileToJavaSourceDecompiler()
        println(f)
        try {
            decompiler.decompile(loader, printer, input.relativize(f).toString().substringBeforeLast('.'))
        } catch (e: Exception) {
            e.printStackTrace()
        }
        Files.write(outFile, printer.toString().toByteArray())
    }
}

private fun decompileProcyon(input: Path, output: Path) {
    val settings = DecompilerSettings.javaDefaults()
    Files.walk(input).forEach { f ->
        if (Files.isDirectory(f)) return@forEach
        val classSimpleName = f.fileName.toString().substringBeforeLast('.')
        val outFile = output.resolve(input.relativize(f)).resolveSibling("$classSimpleName.java")
        Files.createDirectories(outFile.parent)
        println(f)
        try {
            Files.newBufferedWriter(outFile).use { writer ->
                Decompiler.decompile(f.toString(), PlainTextOutput(writer), settings)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

private fun readClasses(dir: Path): Collection<ClassNode> {
    val classes = ArrayList<ClassNode>()
    Files.walk(dir).forEach { f ->
        if (Files.isDirectory(f) || !f.toString().endsWith(".class")) return@forEach
        val reader = ClassReader(Files.readAllBytes(f))
        val node = ClassNode()
        reader.accept(node, ClassReader.SKIP_FRAMES or ClassReader.SKIP_DEBUG)
        classes.add(node)
    }
    return classes
}

private fun writeClasses(classes: Collection<ClassNode>, dir: Path) {
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