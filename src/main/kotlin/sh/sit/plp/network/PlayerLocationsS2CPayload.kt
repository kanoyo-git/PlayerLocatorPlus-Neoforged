package sh.sit.plp.network

import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation
import net.minecraft.core.UUIDUtil
import sh.sit.plp.PlayerLocatorPlus
import java.util.*

@JvmRecord
data class PlayerLocationsS2CPayload(
    val locationUpdates: List<RelativePlayerLocation>,
    val removeUuids: List<UUID>,
    val fullReset: Boolean,
) : CustomPacketPayload {
    companion object {
        private val PLAYER_LOCATIONS_PAYLOAD_ID = ResourceLocation.fromNamespaceAndPath(PlayerLocatorPlus.RESOURCE_NAMESPACE, "player_locations_v2")

        val TYPE = CustomPacketPayload.Type<PlayerLocationsS2CPayload>(PLAYER_LOCATIONS_PAYLOAD_ID)
        val CODEC: StreamCodec<FriendlyByteBuf, PlayerLocationsS2CPayload> = StreamCodec.composite(
            ByteBufCodecs.collection(
                /* intFunction = */ { capacity -> ArrayList(capacity) },
                /* streamCodec = */ RelativePlayerLocation.CODEC
            ),
            PlayerLocationsS2CPayload::locationUpdates,
            ByteBufCodecs.collection(
                { capacity -> ArrayList(capacity) },
                UUIDUtil.STREAM_CODEC
            ),
            PlayerLocationsS2CPayload::removeUuids,
            ByteBufCodecs.BOOL,
            PlayerLocationsS2CPayload::fullReset,
            ::PlayerLocationsS2CPayload
        )
    }

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE
}
