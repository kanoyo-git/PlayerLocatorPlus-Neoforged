package sh.sit.plp.config

import com.akuleshov7.ktoml.annotations.TomlInteger
import com.akuleshov7.ktoml.writers.IntegerRepresentation
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import sh.sit.plp.PlayerLocatorPlus

@Serializable
class ModConfig {
    var enabled = true
    var sendServerConfig = true
    var sendDistance = true
    var maxDistance = 0
    var directionPrecision = 300f
    var ticksBetweenUpdates = 5
    var sneakingHides = true
    var pumpkinHides = true
    var mobHeadsHide = true
    var invisibilityHides = true

    var visible = true
    var visibleEmpty = false
    var alwaysVisibleInSpectator = false
    var acceptServerConfig = true
    var fadeMarkers = false
    var fadeStart = 100
    var fadeEnd = 1000
    var fadeEndOpacity = 0.3f
    var shrinkMarkers = true
    var shrinkStart = 70
    var shrinkEnd = 500
    var showHeight = true
    var alwaysShowHeads = false
    var showHeadsOnTab = true
    var showNamesOnTab = true

    var colorMode = ColorMode.UUID
    @TomlInteger(IntegerRepresentation.HEX)
    var constantColor = 0xFFFFFF

    enum class ColorMode { UUID, TEAM_COLOR, CUSTOM, CONSTANT }

    fun validatePostLoad() {
        if (fadeStart < 0) {
            PlayerLocatorPlus.logger.warn("invalid config: fadeStart < 0")
            fadeStart = 0
        }
        if (fadeEnd < 1 || fadeEnd <= fadeStart) {
            PlayerLocatorPlus.logger.warn("invalid config: fadeEnd < 1 or fadeEnd <= fadeStart")
            fadeEnd = fadeStart + 1
        }
        if (fadeEndOpacity !in 0.0..1.0) {
            PlayerLocatorPlus.logger.warn("invalid config: fadeEndOpacity not in [0, 1]")
            fadeEndOpacity = 0.3f
        }
        if (shrinkEnd < 1 || shrinkEnd <= shrinkStart) {
            PlayerLocatorPlus.logger.warn("invalid config: shrinkEnd < 1 or shrinkEnd <= shrinkStart")
            shrinkEnd = shrinkStart + 1
        }
        if (ticksBetweenUpdates < 0) {
            PlayerLocatorPlus.logger.warn("invalid config: ticksBetweenUpdates < 0")
            ticksBetweenUpdates = 0
        }
        if (directionPrecision <= 1) {
            PlayerLocatorPlus.logger.warn("invalid config: directionPrecision <= 1")
            directionPrecision = 300f
        }
        if (maxDistance < 0) {
            PlayerLocatorPlus.logger.warn("invalid config: maxDistance < 0")
            maxDistance = 0
        }
    }

    companion object {
        val PACKET_CODEC = object : StreamCodec<FriendlyByteBuf, ModConfig> {
            val json = Json {
                encodeDefaults = true
                ignoreUnknownKeys = true
            }

            override fun encode(buf: FriendlyByteBuf, value: ModConfig) {
                buf.writeUtf(json.encodeToString(value), 16 * 1024)
            }

            override fun decode(buf: FriendlyByteBuf): ModConfig {
                val data = buf.readUtf(16 * 1024)
                return json.decodeFromString(data)
            }
        }
    }
}
