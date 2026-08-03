package sh.sit.plp.config

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.TomlInputConfig
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import net.neoforged.fml.loading.FMLPaths
import net.neoforged.neoforge.network.PacketDistributor
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import sh.sit.plp.PlayerLocatorPlus
import sh.sit.plp.network.ModConfigS2CPayload
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

/**
 * Keeps the original single TOML file format.  The local configuration is used by
 * an integrated or dedicated server and is sent to connected clients when enabled.
 */
object ConfigManager {
    private val toml = Toml(inputConfig = TomlInputConfig(ignoreUnknownNames = true))
    private val configPath: Path
        get() = FMLPaths.CONFIGDIR.get().resolve("player-locator-plus.toml")

    private lateinit var localConfig: ModConfig
    private var server: MinecraftServer? = null

    var configOverride: ModConfig? = null

    fun init() {
        if (::localConfig.isInitialized) return

        localConfig = load()
        save(localConfig)
    }

    fun reload(fromDisk: Boolean = false, minecraftServer: MinecraftServer? = null) {
        if (fromDisk) {
            localConfig = load()
            save(localConfig)
        }
        server = minecraftServer ?: server

        val currentServer = server
        if (!localConfig.sendServerConfig || currentServer == null) {
            PlayerLocatorPlus.logger.info("Player Locator Plus config reloaded")
            return
        }

        currentServer.playerList.players.forEach(::sendConfig)
        PlayerLocatorPlus.logger.info("Player Locator Plus config reloaded and resent to clients")
    }

    fun sendConfig(player: ServerPlayer) {
        server = player.server
        if (localConfig.sendServerConfig) {
            PacketDistributor.sendToPlayer(player, ModConfigS2CPayload(localConfig))
        }
    }

    fun clearServer() {
        server = null
    }

    fun getConfig(): ModConfig = configOverride?.takeIf { localConfig.acceptServerConfig } ?: localConfig

    private fun load(): ModConfig {
        val defaultConfig = ModConfig()
        if (!Files.exists(configPath)) return defaultConfig

        return try {
            toml.decodeFromString<ModConfig>(Files.readString(configPath)).also(ModConfig::validatePostLoad)
        } catch (error: IOException) {
            PlayerLocatorPlus.logger.error("Could not read {}. Using defaults.", configPath, error)
            defaultConfig
        } catch (error: kotlinx.serialization.SerializationException) {
            PlayerLocatorPlus.logger.error("Could not parse {}. Using defaults.", configPath, error)
            defaultConfig
        }
    }

    private fun save(config: ModConfig) {
        try {
            Files.createDirectories(configPath.parent)
            Files.writeString(configPath, toml.encodeToString(config))
        } catch (error: IOException) {
            PlayerLocatorPlus.logger.error("Could not write {}", configPath, error)
        }
    }
}
