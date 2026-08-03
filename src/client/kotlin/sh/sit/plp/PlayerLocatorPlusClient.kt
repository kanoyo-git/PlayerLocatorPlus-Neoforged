package sh.sit.plp

import net.minecraft.client.Minecraft
import net.minecraft.client.DeltaTracker
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.PlayerFaceRenderer
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.phys.Vec3
import net.minecraft.world.level.GameType
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent
import net.neoforged.neoforge.client.gui.VanillaGuiLayers
import org.joml.Vector2d
import org.joml.Vector3f
import sh.sit.plp.PlayerLocatorPlus.Companion.config
import sh.sit.plp.config.ConfigManager
import sh.sit.plp.network.ModConfigS2CPayload
import sh.sit.plp.network.PlayerLocationsS2CPayload
import sh.sit.plp.network.RelativePlayerLocation
import sh.sit.plp.util.Animatable
import sh.sit.plp.util.MathUtils
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.math.abs
import kotlin.math.round
import kotlin.math.roundToInt
import kotlin.math.sqrt

@EventBusSubscriber(modid = PlayerLocatorPlus.MOD_ID, value = [Dist.CLIENT], bus = EventBusSubscriber.Bus.MOD)
object PlayerLocatorPlusClient {
    private val EXPERIENCE_BAR_BACKGROUND_TEXTURE = ResourceLocation.fromNamespaceAndPath(PlayerLocatorPlus.RESOURCE_NAMESPACE, "hud/empty_bar")
    private val PLAYER_MARK_TEXTURE = ResourceLocation.fromNamespaceAndPath(PlayerLocatorPlus.RESOURCE_NAMESPACE, "hud/player_mark")
    private val PLAYER_MARK_UP_TEXTURE = ResourceLocation.fromNamespaceAndPath(PlayerLocatorPlus.RESOURCE_NAMESPACE, "hud/player_mark_up")
    private val PLAYER_MARK_DOWN_TEXTURE = ResourceLocation.fromNamespaceAndPath(PlayerLocatorPlus.RESOURCE_NAMESPACE, "hud/player_mark_down")
    private val PLAYER_MARK_WHITE_OUTLINE_TEXTURE = ResourceLocation.fromNamespaceAndPath(PlayerLocatorPlus.RESOURCE_NAMESPACE, "hud/player_mark_white_outline")

    private val PLAYER_MARK_TEXTURES = arrayOf(
        ResourceLocation.fromNamespaceAndPath(PlayerLocatorPlus.RESOURCE_NAMESPACE, "hud/player_mark_0"),
        ResourceLocation.fromNamespaceAndPath(PlayerLocatorPlus.RESOURCE_NAMESPACE, "hud/player_mark_1"),
        ResourceLocation.fromNamespaceAndPath(PlayerLocatorPlus.RESOURCE_NAMESPACE, "hud/player_mark_2"),
        ResourceLocation.fromNamespaceAndPath(PlayerLocatorPlus.RESOURCE_NAMESPACE, "hud/player_mark_3"),
        ResourceLocation.fromNamespaceAndPath(PlayerLocatorPlus.RESOURCE_NAMESPACE, "hud/player_mark_4"),
        ResourceLocation.fromNamespaceAndPath(PlayerLocatorPlus.RESOURCE_NAMESPACE, "hud/player_mark_5"),
        ResourceLocation.fromNamespaceAndPath(PlayerLocatorPlus.RESOURCE_NAMESPACE, "hud/player_mark_6"),
    )

    private const val NAME_PLAQUE_PADDING_X = 4
    private const val NAME_PLAQUE_PADDING_Y = 2
    private const val NAME_PLAQUE_MARGIN = 2
    private const val NAME_PLAQUE_OVERLAP_THRESHOLD = 2

    private const val HUD_OFFSET_TOTAL = 16f
    private var hudOffset = Animatable(0f)

    // for mixin
    val currentHudOffset get() = hudOffset.currentValue

    private val relativePositionsLock = ReentrantLock()
    private var lastUpdatePosition = Vec3.ZERO
    private val relativePositions = mutableMapOf<UUID, RelativePlayerLocation>()

    private data class NamePlaque(
        val x: Int,
        val playerName: String,
        val progress: Double
    )

    @SubscribeEvent
    @JvmStatic
    fun registerGuiLayers(event: RegisterGuiLayersEvent) {
        event.registerAbove(
            VanillaGuiLayers.EXPERIENCE_LEVEL,
            ResourceLocation.fromNamespaceAndPath(PlayerLocatorPlus.RESOURCE_NAMESPACE, "player_locator"),
            ::render,
        )
    }

    fun handlePlayerLocations(payload: PlayerLocationsS2CPayload) {
        relativePositionsLock.lock()
        try {
            if (payload.fullReset) {
                relativePositions.clear()
            } else {
                payload.removeUuids.forEach(relativePositions::remove)
            }

            payload.locationUpdates.forEach { update -> relativePositions[update.playerUuid] = update }
            lastUpdatePosition = Minecraft.getInstance().player?.position() ?: Vec3.ZERO
        } finally {
            relativePositionsLock.unlock()
        }
    }

    fun handleServerConfig(payload: ModConfigS2CPayload) {
        ConfigManager.configOverride = payload.config
    }

    fun clearClientState() {
        relativePositionsLock.lock()
        try {
            relativePositions.clear()
            ConfigManager.configOverride = null
        } finally {
            relativePositionsLock.unlock()
        }
    }

    fun isBarVisible(): Boolean {
        val client = Minecraft.getInstance()

        val player = client.player ?: return false
        val interactionManager = client.gameMode ?: return false
        val networkHandler = client.connection

        // hide when disabled
        if (!config.visible) {
            return false
        }
        // hide in F1
        if (client.options.hideGui) {
            return false
        }
        // hide when there are no other players online and relativePositions is empty
        if (
            !config.visibleEmpty &&
            relativePositions.isEmpty() &&
            networkHandler?.onlinePlayers?.any { it.profile.id != player.uuid } != true
        ) {
            return false
        }
        // hide in spectator mode when the spectator menu is not open
        if (
            interactionManager.playerMode == GameType.SPECTATOR &&
            !config.alwaysVisibleInSpectator
        ) {
            return false
        }

        return true
    }

    fun render(context: GuiGraphics, tickCounter: DeltaTracker) {
        if (!config.visible) return

        if (!isBarVisible()) return

        val client = Minecraft.getInstance()
        val player = client.player ?: return
        val interactionManager = client.gameMode ?: return

        val barWidth = 182
        val x = context.guiWidth() / 2 - 91
        val y = context.guiHeight() - 32 + 3

        val barRendered = player.jumpableVehicle() != null || interactionManager.hasExperience()
        if (!barRendered) {
            context.blitSprite(EXPERIENCE_BAR_BACKGROUND_TEXTURE, x, y, barWidth, 5)
        }

        relativePositionsLock.lock()

        val namePlaques = mutableListOf<NamePlaque>()

        val isTabPressed = client.options.keyPlayerList.isDown

        for (position in relativePositions.values) {
            val playerMarker = player.level().getPlayerByUUID(position.playerUuid)
            val actualPosition = playerMarker
                ?.getPosition(tickCounter.getGameTimeDeltaPartialTick(false))
            val direction = if (actualPosition != null) {
                actualPosition.subtract(player.getPosition(tickCounter.getGameTimeDeltaPartialTick(false)))
            } else if (position.distance == 0f) {
                Vec3(position.direction)
            } else {
                val projectedPosition = lastUpdatePosition
                    .add(Vec3(position.direction).scale(position.distance.toDouble()))
                projectedPosition.subtract(player.getPosition(tickCounter.getGameTimeDeltaPartialTick(false)))
            }

            val direction2d = Vector2d(direction.x, direction.z)
            if (!direction2d.isFinite) {
                continue
            }
            val rotationVec = player.getViewVector(tickCounter.getGameTimeDeltaPartialTick(false))
            var relativeAngle = -direction2d.angle(Vector2d(rotationVec.x, rotationVec.z)) * 180.0 / Math.PI
            if (relativeAngle.isNaN()) {
                relativeAngle = 0.0
            }

            val horizontalFov = MathUtils.calculateHorizontalFov(
                verticalFov = client.options.fov().get(),
                width = context.guiWidth(),
                height = context.guiHeight()
            )
            val progress = (relativeAngle + horizontalFov / 2) / horizontalFov
            if (progress !in 0.0..1.0) {
                continue
            }

            val markX = x + (progress * barWidth.toFloat()).roundToInt() - 4

            val showHeadIcon = config.alwaysShowHeads || (config.showHeadsOnTab && isTabPressed)

            val playerList = client.connection?.onlinePlayers ?: emptyList()
            val playerListEntry = playerList.find { it.profile.id == position.playerUuid }

            val opacity = if (config.fadeMarkers) {
                val dist = position.distance.coerceIn(config.fadeStart.toFloat(), config.fadeEnd.toFloat())
                val fadeProgress = 1 - (dist - config.fadeStart) / (config.fadeEnd - config.fadeStart)
                (((1 - config.fadeEndOpacity) * fadeProgress + config.fadeEndOpacity) * 255).roundToInt()
            } else {
                255
            }
            val color = (opacity shl 24) or (position.color and 0xFFFFFF)

            // store marker information for name plaque rendering later
            if (playerListEntry != null && config.showNamesOnTab) {
                namePlaques.add(
                    NamePlaque(
                        x = markX,
                        playerName = playerListEntry.profile.name,
                        progress = progress
                    )
                )
            }

            if (playerListEntry == null || !showHeadIcon) {
                val texture = if (config.shrinkMarkers) {
                    val dist = position.distance.coerceIn(config.shrinkStart.toFloat(), config.shrinkEnd.toFloat())
                    val shrinkProgress = (dist - config.shrinkStart) / (config.shrinkEnd - config.shrinkStart)
                    val textureIdx = (shrinkProgress * PLAYER_MARK_TEXTURES.size).toInt()
                        .coerceAtMost(PLAYER_MARK_TEXTURES.size - 1)
                    PLAYER_MARK_TEXTURES[textureIdx]
                } else {
                    PLAYER_MARK_TEXTURE
                }

                drawSprite(context, texture, markX, y - 1, 7, 7, color)
            } else {
                drawSprite(context, PLAYER_MARK_WHITE_OUTLINE_TEXTURE, markX, y - 1, 7, 7, color)

                PlayerFaceRenderer.draw(
                    context,
                    playerListEntry.skin,
                    markX + 1,
                    y,
                    5,
                )
            }

            if (config.showHeight) {
                val heightDiffNormalized = direction.normalize().y
                if (heightDiffNormalized > 0.5) { // about 45 deg
                    context.blitSprite(PLAYER_MARK_UP_TEXTURE, markX + 1, y - 5, 5, 4)
                } else if (heightDiffNormalized < -0.5) {
                    context.blitSprite(PLAYER_MARK_DOWN_TEXTURE, markX + 1, y + 7, 5, 4)
                }
            }
        }

        hudOffset.targetValue = if (isTabPressed && config.showNamesOnTab && namePlaques.isNotEmpty()) {
            HUD_OFFSET_TOTAL
        } else {
            0f
        }
        hudOffset.updateValues(tickCounter.getGameTimeDeltaPartialTick(false) * 50f)

        val fadeProgress = round(hudOffset.currentValue / HUD_OFFSET_TOTAL * 255f) / 255f

        if (namePlaques.isNotEmpty() && fadeProgress > 0) {
            renderPlayerNamePlaques(context, namePlaques, y, fadeProgress)
        }

        relativePositionsLock.unlock()
    }

    private fun renderPlayerNamePlaques(
        context: GuiGraphics,
        markers: List<NamePlaque>,
        barY: Int,
        fadeProgress: Float = 1f
    ) {
        val textRenderer = Minecraft.getInstance().font

        // sort markers by their distance from the center (closest first)
        val sortedMarkers = markers.sortedBy {
            abs(it.progress - 0.5)
        }

        // determine which markers should be visible
        val visibleMarkers = mutableListOf<Pair<NamePlaque, IntRange>>()

        for (marker in sortedMarkers) {
            val textWidth = textRenderer.width(marker.playerName)
            val plaqueWidth = textWidth + NAME_PLAQUE_PADDING_X * 2

            val plaqueX = marker.x - plaqueWidth / 2 + 4
            val plaqueXRange = plaqueX..(plaqueX + plaqueWidth)

            val overlap = visibleMarkers.any { (_, range) ->
                range.first - NAME_PLAQUE_OVERLAP_THRESHOLD <= plaqueXRange.last &&
                range.last + NAME_PLAQUE_OVERLAP_THRESHOLD >= plaqueXRange.first
            }

            if (!overlap) {
                visibleMarkers.add(marker to plaqueXRange)
            }
        }

        // render markers in visibleMarkers
        for ((marker, _) in visibleMarkers) {
            val textWidth = textRenderer.width(marker.playerName)
            val plaqueWidth = textWidth + NAME_PLAQUE_PADDING_X * 2
            val plaqueHeight = textRenderer.lineHeight + NAME_PLAQUE_PADDING_Y * 2

            val plaqueX = marker.x - plaqueWidth / 2 + 4
            val plaqueY = barY - plaqueHeight - NAME_PLAQUE_MARGIN

            val bgAlpha = (192 * fadeProgress).roundToInt()
            val textAlpha = (255 * fadeProgress).roundToInt()

            if (bgAlpha > 0) context.fill(
                plaqueX,
                plaqueY,
                plaqueX + plaqueWidth,
                plaqueY + plaqueHeight,
                (bgAlpha shl 24)
            )

            // for some reason, if the opacity is under 4, drawText just assumes the color does not include alpha
            if (textAlpha > 3) context.drawString(
                textRenderer,
                marker.playerName,
                plaqueX + NAME_PLAQUE_PADDING_X,
                plaqueY + NAME_PLAQUE_PADDING_Y,
                (textAlpha shl 24) or 0xFFFFFF,
                false
            )
        }
    }

    private fun drawSprite(context: GuiGraphics, texture: ResourceLocation, x: Int, y: Int, width: Int, height: Int, color: Int) {
        context.setColor(
            ((color shr 16) and 0xFF) / 255f,
            ((color shr 8) and 0xFF) / 255f,
            (color and 0xFF) / 255f,
            ((color ushr 24) and 0xFF) / 255f,
        )
        context.blitSprite(texture, x, y, width, height)
        context.setColor(1f, 1f, 1f, 1f)
    }
}

@EventBusSubscriber(modid = PlayerLocatorPlus.MOD_ID, value = [Dist.CLIENT])
object PlayerLocatorPlusClientGameEvents {
    @SubscribeEvent
    @JvmStatic
    fun onDisconnect(event: ClientPlayerNetworkEvent.LoggingOut) {
        PlayerLocatorPlusClient.clearClientState()
    }
}
