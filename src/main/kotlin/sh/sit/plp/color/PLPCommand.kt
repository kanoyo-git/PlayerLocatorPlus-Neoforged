package sh.sit.plp.color

import com.mojang.authlib.GameProfile
import com.mojang.brigadier.Command
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import net.minecraft.ChatFormatting
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.arguments.GameProfileArgument
import net.minecraft.network.chat.Component
import sh.sit.plp.BarUpdater
import sh.sit.plp.PlayerLocatorPlus
import sh.sit.plp.config.ConfigManager
import sh.sit.plp.config.ModConfig

object PLPCommand {
    private val WRONG_COLOR_MODE = SimpleCommandExceptionType(Component.translatable("commands.player-locator-plus.color.wrong-color-mode"))
    private val NON_SINGLE_PLAYER = SimpleCommandExceptionType(Component.translatable("commands.player-locator-plus.color.non-single-player"))
    private val INVALID_COLOR = SimpleCommandExceptionType(Component.translatable("commands.player-locator-plus.color.invalid-color"))

    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(
            Commands.literal("plp")
                .then(
                    Commands.literal("reload")
                        .requires { it.hasPermission(2) }
                        .executes { context ->
                            context.source.sendSuccess({ Component.literal("Player Locator Plus config reloaded") }, false)
                            ConfigManager.reload(fromDisk = true, minecraftServer = context.source.server)
                            BarUpdater.fullResend(context.source.server)
                            Command.SINGLE_SUCCESS
                        },
                )
                .then(
                    Commands.literal("random")
                        .requires { it.entity != null && it.hasPermission(2) }
                        .executes { context ->
                            context.source.player?.let(BarUpdater::sendFakePlayers)
                            Command.SINGLE_SUCCESS
                        },
                )
                .then(
                    Commands.literal("color")
                        .then(
                            Commands.argument("color", StringArgumentType.word())
                                .suggests { _, builder ->
                                    ChatFormatting.values().filter(ChatFormatting::isColor).forEach { builder.suggest(it.name) }
                                    builder.suggest("#")
                                    builder.buildFuture()
                                }
                                .executes { context -> runChangeColor(context, true) }
                                .then(
                                    Commands.argument("player", GameProfileArgument.gameProfile())
                                        .requires { it.hasPermission(3) }
                                        .executes { context -> runChangeColor(context, false) },
                                ),
                        ),
                ),
        )
    }

    private fun runChangeColor(context: CommandContext<CommandSourceStack>, self: Boolean): Int {
        if (PlayerLocatorPlus.config.colorMode != ModConfig.ColorMode.CUSTOM) {
            throw WRONG_COLOR_MODE.create()
        }

        val player: GameProfile = if (self) {
            context.source.playerOrException.gameProfile
        } else {
            GameProfileArgument.getGameProfiles(context, "player").singleOrNull() ?: throw NON_SINGLE_PLAYER.create()
        }

        val color = parseColor(StringArgumentType.getString(context, "color"))
            ?: throw INVALID_COLOR.create()

        PlayerDataState.of(context.source.server).run {
            getPlayer(player.id).customColor = color
            setDirty()
        }
        context.source.sendSuccess(
            if (self) {
                { Component.translatable("commands.player-locator-plus.color.self", formatColor(color)) }
            } else {
                {
                    Component.translatable(
                        "commands.player-locator-plus.color.other",
                        Component.nullToEmpty(player.name),
                        formatColor(color),
                    )
                }
            },
            false,
        )

        return Command.SINGLE_SUCCESS
    }

    private fun parseColor(value: String): Int? {
        ChatFormatting.getByName(value)?.color?.let { return it }
        if (!value.startsWith('#') || value.length != 7) return null
        return value.substring(1).toIntOrNull(16)
    }

    private fun formatColor(color: Int): Component =
        Component.literal("#" + color.toString(16).padStart(6, '0')).withColor(color)
}
