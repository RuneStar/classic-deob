package org.runestar.classicdeob

import com.strobel.decompiler.Decompiler
import com.strobel.decompiler.DecompilerSettings
import com.strobel.decompiler.PlainTextOutput
import org.benf.cfr.reader.Main
import org.jd.core.v1.ClassFileToJavaSourceDecompiler
import org.jd.core.v1.api.loader.Loader
import org.jetbrains.java.decompiler.main.decompiler.ConsoleDecompiler
import org.zeroturnaround.zip.ZipUtil
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration
import java.time.Instant

fun main() {
    val start = Instant.now()
    val input = Paths.get("input")
    val output = Paths.get("output")
    Files.walk(input).filter { it.fileName.toString() == "gamepack.jar" }.forEachClose { gamepack ->
        deob(input, gamepack, output)
    }
    println("total: ${Duration.between(start, Instant.now())}")
}

private fun deob(input: Path, gamepack: Path, output: Path) {
    val start = Instant.now()
    val dir = output.resolve(input.relativize(gamepack.parent))
    val temp = dir.resolve("temp")
    val outgamepack = dir.resolve("gamepack.jar")
    dir.toFile().deleteRecursively()

    val transformer = Transformer.Composite(
            RemoveRethrows,
            DecryptStrings,
            ReplaceCfn,
            FieldResolver,
            RemoveXfChecks,
            RemoveUnusedMath,
            UndoComplementComparisons,
            RemoveCounters,
            FixNegatives,
            RemoveOpaquePredicates,
            MaskShifts
    )

    Files.createDirectories(temp)
    ZipUtil.unpack(gamepack.toFile(), temp.toFile())
    writeClasses(transformer.transform(readClasses(temp)), temp)
    ZipUtil.pack(temp.toFile(), outgamepack.toFile())

//    val cfrDir = dir.resolve("cfr")
//    Files.createDirectories(cfrDir)
//    decompileCfr(outgamepack, cfrDir)

//    val fernflowerDir = dir.resolve("fernflower")
//    Files.createDirectories(fernflowerDir)
//    decompileFernflower(temp, fernflowerDir)

//    val jdDir = dir.resolve("jd")
//    Files.createDirectories(jdDir)
//    decompileJd(temp, jdDir)

//    val procyonDir = dir.resolve("procyon")
//    Files.createDirectories(procyonDir)
//    decompileProcyon(temp, procyonDir)

    println("${gamepack.parent.fileName}: ${Duration.between(start, Instant.now())}")
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
    Files.walk(input).forEachClose { f ->
        if (Files.isDirectory(f) || !f.toString().endsWith(".class")) return@forEachClose
        val classSimpleName = f.fileName.toString().substringBeforeLast('.')
        val outFile = output.resolve(input.relativize(f)).resolveSibling("$classSimpleName.java")
        Files.createDirectories(outFile.parent)
        val printer = JdPrinter()
        val decompiler = ClassFileToJavaSourceDecompiler()
        println(f)
        try {
            decompiler.decompile(loader, printer, input.relativize(f).toString().substringBeforeLast('.'))
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        Files.write(outFile, printer.toString().toByteArray())
    }
}

private fun decompileProcyon(input: Path, output: Path) {
    val settings = DecompilerSettings.javaDefaults()
    Files.walk(input).forEachClose { f ->
        if (Files.isDirectory(f) || !f.toString().endsWith(".class")) return@forEachClose
        val classSimpleName = f.fileName.toString().substringBeforeLast('.')
        val outFile = output.resolve(input.relativize(f)).resolveSibling("$classSimpleName.java")
        Files.createDirectories(outFile.parent)
        println(f)
        try {
            Files.newBufferedWriter(outFile).use { writer ->
                Decompiler.decompile(f.toString(), PlainTextOutput(writer), settings)
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
}