package sh.sit.plp

import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.common.Mod
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.RegisterCommandsEvent
import net.neoforged.neoforge.event.entity.player.PlayerEvent
import net.neoforged.neoforge.event.server.ServerStoppedEvent
import net.neoforged.neoforge.event.tick.ServerTickEvent
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent
import net.neoforged.neoforge.network.registration.PayloadRegistrar
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagKey
import org.slf4j.LoggerFactory
import sh.sit.plp.color.PLPCommand
import sh.sit.plp.config.ConfigManager
import sh.sit.plp.network.ModConfigS2CPayload
import sh.sit.plp.network.PlayerLocationsS2CPayload

@Mod(PlayerLocatorPlus.MOD_ID)
class PlayerLocatorPlus(modEventBus: IEventBus) {
    init {
        ConfigManager.init()

        modEventBus.addListener(::registerPayloads)
        NeoForge.EVENT_BUS.addListener(::onPlayerLogin)
        NeoForge.EVENT_BUS.addListener(::onPlayerChangedDimension)
        NeoForge.EVENT_BUS.addListener(::onServerTick)
        NeoForge.EVENT_BUS.addListener(::onServerStopped)
        NeoForge.EVENT_BUS.addListener(::onRegisterCommands)

        logger.info("Player Locator Plus initialized")
    }

    private fun registerPayloads(event: RegisterPayloadHandlersEvent) {
        val registrar: PayloadRegistrar = event.registrar("1")
        registrar.playToClient(PlayerLocationsS2CPayload.TYPE, PlayerLocationsS2CPayload.CODEC) { payload, context ->
            context.enqueueWork { PlayerLocatorPlusClient.handlePlayerLocations(payload) }
        }
        registrar.playToClient(ModConfigS2CPayload.TYPE, ModConfigS2CPayload.CODEC) { payload, context ->
            context.enqueueWork { PlayerLocatorPlusClient.handleServerConfig(payload) }
        }
    }

    private fun onPlayerLogin(event: PlayerEvent.PlayerLoggedInEvent) {
        val player = event.entity as? net.minecraft.server.level.ServerPlayer ?: return
        BarUpdater.fullResend(player)
        ConfigManager.sendConfig(player)
    }

    private fun onPlayerChangedDimension(event: PlayerEvent.PlayerChangedDimensionEvent) {
        val player = event.entity as? net.minecraft.server.level.ServerPlayer ?: return
        BarUpdater.fullResend(player)
    }

    private fun onServerTick(event: ServerTickEvent.Post) {
        if (tickCounter < config.ticksBetweenUpdates) {
            tickCounter++
            return
        }
        tickCounter = 0

        BarUpdater.update(event.server)
    }

    private fun onServerStopped(event: ServerStoppedEvent) {
        BarUpdater.reset()
        ConfigManager.clearServer()
    }

    private fun onRegisterCommands(event: RegisterCommandsEvent) {
        PLPCommand.register(event.dispatcher)
    }

    companion object {
        const val MOD_ID = "player_locator_plus"
        const val RESOURCE_NAMESPACE = "player-locator-plus"

        val logger = LoggerFactory.getLogger(MOD_ID)!!

        val HIDING_EQUIPMENT_TAG: TagKey<net.minecraft.world.item.Item> = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(RESOURCE_NAMESPACE, "hiding_equipment"),
        )

        private var tickCounter = 0

        val config get() = ConfigManager.getConfig()
    }
}
