/*
 * Block Blast patches (Morphe). Licensed under the GNU General Public License v3.0.
 */
package app.blockblast.patches.network

import app.blockblast.patches.shared.Constants.COMPATIBILITY_BLOCK_BLAST
import app.blockblast.patches.shared.Constants.EXTENSION
import app.blockblast.patches.shared.Constants.EXTENSION_PREFIX
import app.blockblast.patches.shared.Constants.NET_BLOCK_CLASS
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.returnEarly
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val URL_CLASS = "Ljava/net/URL;"
private const val OKHTTP_DNS = "Lokhttp3/Dns;"

/**
 * Classes never rewritten: the extension (it opens the real connections) and the NetEase packer
 * that loads the encrypted part of the app.
 */
private val EXCLUDED_PREFIXES = listOf(EXTENSION_PREFIX, "Lcom/netease/nis/")

/**
 * Routes the network calls of the whole app through NetBlock, which refuses the hosts of the
 * lists the other patches turn on. Hosts, not SDK internals, so it keeps working when the SDKs
 * are obfuscated or updated:
 *
 * - every `URL.openConnection()` call becomes `NetBlock.openConnection(url)`
 *   (HttpURLConnection: Firebase, AppsFlyer, Bugsnag, ThinkingData, most ad SDKs),
 * - every OkHttp `Dns.lookup(hostname)` calls `NetBlock.checkHost(hostname)` first.
 */
internal val networkBlockerPatch = bytecodePatch {
    compatibleWith(COMPATIBILITY_BLOCK_BLAST)

    extendWith(EXTENSION)

    execute {
        fun ClassDef.isExcluded() = EXCLUDED_PREFIXES.any { type.startsWith(it) }

        val connectionClasses = ArrayList<ClassDef>()
        val dnsClasses = ArrayList<ClassDef>()
        @Suppress("DEPRECATION")
        classDefForEach { classDef ->
            if (classDef.isExcluded()) return@classDefForEach
            if (classDef.methods.any { method -> method.openConnectionIndices().isNotEmpty() }) {
                connectionClasses += classDef
            }
            if (OKHTTP_DNS in classDef.interfaces) dnsClasses += classDef
        }

        var calls = 0
        connectionClasses.forEach { classDef ->
            mutableClassDefBy(classDef).methods.forEach { method ->
                method.openConnectionIndices().forEach { index ->
                    method.replaceInstruction(index, method.staticOpenConnection(index))
                    calls++
                }
            }
        }
        if (calls == 0) throw PatchException("No URL.openConnection call found")

        dnsClasses.forEach { classDef ->
            mutableClassDefBy(classDef).methods.filter { method ->
                method.name == "lookup" &&
                    method.parameterTypes.map { it.toString() } == listOf("Ljava/lang/String;") &&
                    method.implementation != null
            }.forEach { method ->
                method.addInstructions(
                    0,
                    "invoke-static/range { p1 .. p1 }, $NET_BLOCK_CLASS->checkHost(Ljava/lang/String;)V",
                )
            }
        }
    }
}

/** Replaced with `return true` to block the hosts of [list] ("blockAds" or "blockTelemetry"). */
internal fun app.morphe.patcher.patch.BytecodePatchContext.enableBlockList(list: String) {
    mutableClassDefBy(NET_BLOCK_CLASS).methods.first { it.name == list }.returnEarly(true)
}

private fun Instruction.isOpenConnection(): Boolean {
    if (opcode != Opcode.INVOKE_VIRTUAL && opcode != Opcode.INVOKE_VIRTUAL_RANGE) return false
    val reference = (this as ReferenceInstruction).reference as? MethodReference ?: return false
    return reference.definingClass == URL_CLASS && reference.name == "openConnection"
}

private fun Method.openConnectionIndices(): List<Int> =
    implementation?.instructions?.withIndex()
        ?.filter { (_, instruction) -> instruction.isOpenConnection() }
        ?.map { it.index }
        .orEmpty()

/**
 * The static call replacing the instruction at [index]: same registers, the receiver becomes
 * the first argument. `openConnection()` and `openConnection(Proxy)` keep their return type, so
 * the move-result that follows is unchanged.
 */
private fun MutableMethod.staticOpenConnection(index: Int): String {
    val instruction = implementation!!.instructions[index]
    val reference = (instruction as ReferenceInstruction).reference as MethodReference
    val parameters = reference.parameterTypes.joinToString("") { it.toString() }
    val target = "$NET_BLOCK_CLASS->openConnection($URL_CLASS$parameters)Ljava/net/URLConnection;"

    return when (instruction) {
        is RegisterRangeInstruction -> {
            val first = instruction.startRegister
            val last = first + instruction.registerCount - 1
            "invoke-static/range { v$first .. v$last }, $target"
        }

        is FiveRegisterInstruction -> {
            val registers = listOf(
                instruction.registerC,
                instruction.registerD,
                instruction.registerE,
                instruction.registerF,
                instruction.registerG,
            ).take(instruction.registerCount)
            "invoke-static { ${registers.joinToString { "v$it" }} }, $target"
        }

        else -> throw PatchException("Unexpected instruction $instruction in $this")
    }
}
