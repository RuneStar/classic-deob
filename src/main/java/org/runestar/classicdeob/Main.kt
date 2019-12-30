package org.runestar.classicdeob

import com.strobel.decompiler.DecompilerDriver
import org.benf.cfr.reader.Main
import org.jetbrains.java.decompiler.main.decompiler.ConsoleDecompiler
import org.zeroturnaround.zip.ZipUtil
import java.io.OutputStream
import java.io.PrintStream
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.time.Instant

fun main() {
    val start = Instant.now()
    val input = Path.of("input")
    val output = Path.of("output")
    Files.walk(input).filter { it.fileName.toString() == "gamepack.jar" }.forEachClose { gamepack ->
        deob(input, gamepack, output)
    }
    println("total: ${Duration.between(start, Instant.now())}")
}

private fun deob(input: Path, gamepack: Path, output: Path) {
    val start = Instant.now()
    val dir = output.resolve(input.relativize(gamepack.parent))
    val outgamepack = dir.resolve("gamepack.jar")
    dir.toFile().deleteRecursively()

    val transformer = Transformer.Composite(
            RemoveRethrows,
            DecryptStrings,
            ReplaceCfn,
            ResolveFields,
            RemoveXfChecks,
            RemoveUnusedMath,
            UndoComplementComparisons,
            RemoveCounters,
            FixNegatives,
            Transformer.Remap(Rename),
            RemoveOpaquePredicates,
            MaskShifts
    )

    Files.createDirectories(dir)
    writeClasses(transformer.transform(readClasses(gamepack)), outgamepack)

//    val classes = dir.resolve("classes")
//    ZipUtil.unpack(outgamepack.toFile(), classes.toFile())

//    val cfrDir = dir.resolve("cfr")
//    Files.createDirectories(cfrDir)
//    decompileCfr(outgamepack, cfrDir)

//    val fernflowerDir = dir.resolve("fernflower")
//    Files.createDirectories(fernflowerDir)
//    decompileFernflower(classes, fernflowerDir)

//    val procyonDir = dir.resolve("procyon")
//    Files.createDirectories(procyonDir)
//    decompileProcyon(outgamepack, procyonDir)

    println("${gamepack.parent.fileName}: ${Duration.between(start, Instant.now())}")
}

private fun decompileCfr(input: Path, output: Path) {
    Main.main(arrayOf(
            input.toString(),
            "--outputpath", output.toString(),
            "--silent", "true"
    ))
}

private fun decompileFernflower(input: Path, output: Path) {
    ConsoleDecompiler.main(arrayOf(
            "-log=WARN",
            input.toString(),
            output.toString()
    ))
}

private fun decompileProcyon(input: Path, output: Path) {
    val out = System.out
    System.setOut(PrintStream(OutputStream.nullOutputStream()))
    DecompilerDriver.main(arrayOf(
            "-jar", input.toString(),
            "-o", output.toString()
    ))
    System.setOut(out)
}