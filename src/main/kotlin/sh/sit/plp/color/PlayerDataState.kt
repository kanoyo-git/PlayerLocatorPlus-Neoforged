package sh.sit.plp.color

import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.server.MinecraftServer
import net.minecraft.util.datafix.DataFixTypes
import net.minecraft.world.level.saveddata.SavedData
import java.util.UUID

class PlayerDataState : SavedData() {
    private var players = hashMapOf<UUID, PlayerData>()

    fun getPlayer(uuid: UUID): PlayerData = players.getOrPut(uuid) {
        setDirty()
        PlayerData()
    }

    override fun save(tag: CompoundTag, registries: HolderLookup.Provider): CompoundTag {
        val playersTag = CompoundTag()
        players.forEach { (uuid, data) ->
            val playerTag = CompoundTag()
            data.customColor?.let { playerTag.putInt("customColor", it) }
            playersTag.put(uuid.toString(), playerTag)
        }
        tag.put("players", playersTag)
        return tag
    }

    companion object {
        private const val DATA_NAME = "player_locator_plus_player_data"

        private fun load(tag: CompoundTag, registries: HolderLookup.Provider): PlayerDataState {
            val state = PlayerDataState()
            val playersTag = tag.getCompound("players")
            playersTag.allKeys.forEach { serializedUuid ->
                val uuid = runCatching { UUID.fromString(serializedUuid) }.getOrNull() ?: return@forEach
                val playerTag = playersTag.getCompound(serializedUuid)
                state.players[uuid] = PlayerData(
                    customColor = if (playerTag.contains("customColor", Tag.TAG_INT.toInt())) {
                        playerTag.getInt("customColor")
                    } else {
                        null
                    },
                )
            }
            return state
        }

        private val FACTORY = SavedData.Factory(::PlayerDataState, ::load, DataFixTypes.SAVED_DATA_COMMAND_STORAGE)

        fun of(server: MinecraftServer): PlayerDataState =
            server.overworld().dataStorage.computeIfAbsent(FACTORY, DATA_NAME)
    }
}
