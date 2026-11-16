package com.example.domain.security

import android.util.Base64
import com.example.data.entity.UserProfileEntity
import com.example.domain.model.UserRole
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

data class PortableProvisioningBundle(
    val pairingCode: String,
    val assignedBranchId: Long,
    val deviceAlias: String,
    val approvedBy: String,
    val profiles: List<UserProfileEntity>
)

/**
 * Codificador compacto y verificable (checksum SHA-256) para transferir desde Nivel 3
 * los permisos de PINs (ya hasheados con SHA-256) y la autorización de sucursal
 * hacia un teléfono externo sin requerir que la cuenta Google del dueño quede abierta en el local.
 */
object DeviceProvisioningCodec {
    private const val PREFIX = "DRKPRO1:"

    fun encode(
        pairingCode: String,
        assignedBranchId: Long,
        deviceAlias: String,
        approvedBy: String,
        profiles: List<UserProfileEntity>
    ): String {
        val cleanProfiles = profiles.map { u ->
            val safeHash = if (PinHasher.isHashed(u.pinCode)) u.pinCode else PinHasher.hash(u.username, u.pinCode)
            u.copy(pinCode = safeHash)
        }
        val arr = JSONArray()
        cleanProfiles.forEach { u ->
            arr.put(
                JSONObject().apply {
                    put("u", u.username)
                    put("n", u.fullName)
                    put("h", u.pinCode)
                    put("r", u.role.name)
                    put("b", u.assignedBranchId)
                    put("a", u.isActive)
                }
            )
        }
        val root = JSONObject().apply {
            put("c", pairingCode.trim().uppercase())
            put("b", assignedBranchId)
            put("d", deviceAlias.ifBlank { "Terminal Sucursal K0$assignedBranchId" })
            put("o", approvedBy.ifBlank { "Propietario Nivel 3" })
            put("p", arr)
        }
        val jsonBytes = root.toString().toByteArray(Charsets.UTF_8)
        val bos = ByteArrayOutputStream()
        GZIPOutputStream(bos).use { gzip -> gzip.write(jsonBytes) }
        val compressed = bos.toByteArray()
        val bodyB64 = Base64.encodeToString(compressed, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        val sig = shortChecksum(bodyB64)
        return "$PREFIX$bodyB64.$sig"
    }

    private val TOKEN_REGEX = Regex(
        pattern = "DRKPRO1:([A-Za-z0-9_\\-\\s]+?)\\.((?:[0-9a-fA-F]\\s*){7}[0-9a-fA-F])",
        option = RegexOption.IGNORE_CASE
    )

    fun extractToken(rawInput: String): String? {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) return null
        val match = TOKEN_REGEX.find(trimmed)
        if (match != null) {
            val body = match.groupValues[1].replace(Regex("\\s+"), "")
            val sig = match.groupValues[2].replace(Regex("\\s+"), "").lowercase()
            return "$PREFIX$body.$sig"
        }
        val idx = trimmed.indexOf(PREFIX, ignoreCase = true)
        if (idx >= 0) {
            val candidate = trimmed.substring(idx).replace(Regex("\\s+"), "")
            if (candidate.startsWith(PREFIX, ignoreCase = true)) {
                return PREFIX + candidate.substring(PREFIX.length)
            }
        }
        return null
    }

    fun isPortableBundle(rawInput: String): Boolean = extractToken(rawInput) != null

    fun decode(rawToken: String): Result<PortableProvisioningBundle> {
        return runCatching {
            val normalized = extractToken(rawToken)
                ?: throw IllegalArgumentException("El código no tiene formato portátil Drakos (DRKPRO1:...).")
            val payloadWithSig = normalized.substring(PREFIX.length)
            val dotIdx = payloadWithSig.lastIndexOf('.')
            require(dotIdx > 0) { "Código de vinculación incompleto o corrupto." }
            val bodyB64 = payloadWithSig.substring(0, dotIdx)
            val sig = payloadWithSig.substring(dotIdx + 1)
            require(sig.equals(shortChecksum(bodyB64), ignoreCase = true)) {
                "El código de vinculación fue alterado o copiado de forma incompleta."
            }
            val compressed = Base64.decode(bodyB64, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
            val jsonStr = GZIPInputStream(ByteArrayInputStream(compressed)).bufferedReader(Charsets.UTF_8).use {
                it.readText()
            }
            val root = JSONObject(jsonStr)
            val pairingCode = root.optString("c", "").ifBlank { "DRK-PORTABLE" }
            val assignedBranchId = root.optLong("b", 1L).coerceIn(1L, 100L)
            val deviceAlias = root.optString("d", "Terminal Sucursal K0$assignedBranchId")
            val approvedBy = root.optString("o", "Propietario Nivel 3")
            val arr = root.optJSONArray("p") ?: JSONArray()
            val profiles = buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val username = obj.getString("u").trim().lowercase()
                    val pinHash = obj.getString("h").trim().lowercase()
                    if (username.length >= 3 && PinHasher.isHashed(pinHash)) {
                        val role = runCatching { UserRole.valueOf(obj.getString("r")) }
                            .getOrDefault(UserRole.EMPLOYEE)
                        add(
                            UserProfileEntity(
                                id = 0L,
                                username = username,
                                fullName = obj.optString("n", username),
                                pinCode = pinHash,
                                role = role,
                                assignedBranchId = obj.optLong("b", assignedBranchId).coerceIn(1L, 100L),
                                isActive = obj.optBoolean("a", true)
                            )
                        )
                    }
                }
            }
            PortableProvisioningBundle(
                pairingCode = pairingCode,
                assignedBranchId = assignedBranchId,
                deviceAlias = deviceAlias,
                approvedBy = approvedBy,
                profiles = profiles
            )
        }
    }

    private fun shortChecksum(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(("drakos_port_v1:$input").toByteArray(Charsets.UTF_8))
        return digest.take(4).joinToString("") { "%02x".format(it) }
    }
}
