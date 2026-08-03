package sh.sit.plp.network

import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation
import sh.sit.plp.config.ModConfig
import sh.sit.plp.PlayerLocatorPlus

@JvmRecord
data class ModConfigS2CPayload(
    val config: ModConfig,
) : CustomPacketPayload {
    companion object {
        private val MOD_CONFIG_PAYLOAD_ID = ResourceLocation.fromNamespaceAndPath(PlayerLocatorPlus.RESOURCE_NAMESPACE, "mod_config")

        val TYPE = CustomPacketPayload.Type<ModConfigS2CPayload>(MOD_CONFIG_PAYLOAD_ID)
        val CODEC: StreamCodec<FriendlyByteBuf, ModConfigS2CPayload> = StreamCodec.composite(
            ModConfig.PACKET_CODEC,
            ModConfigS2CPayload::config,
            ::ModConfigS2CPayload
        )
    }

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE
}
