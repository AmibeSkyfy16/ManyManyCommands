package ch.skyfy.manymanycommands.commands

import ch.skyfy.json5configlib.ConfigManager
import ch.skyfy.manymanycommands.ManyManyCommandsMod
import ch.skyfy.manymanycommands.api.config.Configs
import com.mojang.brigadier.Command
import com.mojang.brigadier.Command.SINGLE_SUCCESS
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import net.minecraft.command.CommandSource
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text
import java.util.concurrent.CompletableFuture

class ReloadFilesCmd : Command<ServerCommandSource> {

    companion object {
        fun register(dispatcher: CommandDispatcher<ServerCommandSource>) {
            val cmd = literal("manymanycommands").requires { source -> source.hasPermissionLevel(4) }
                .then(
                    literal("reload").then(
                        argument("fileName", StringArgumentType.string()).suggests(::getConfigFiles).executes(ReloadFilesCmd())
                    )
                )
            dispatcher.register(cmd)
        }

        @Suppress("UNUSED_PARAMETER")
        private fun <S : ServerCommandSource> getConfigFiles(commandContext: CommandContext<S>, suggestionsBuilder: SuggestionsBuilder): CompletableFuture<Suggestions> {
            val list = mutableListOf<String>()
            list.add(Configs.PLAYERS.relativePath.fileName.toString())
            list.add(Configs.RULES.relativePath.fileName.toString())
            list.add("ALL")
            return CommandSource.suggestMatching(list, suggestionsBuilder)
        }
    }

    override fun run(context: CommandContext<ServerCommandSource>): Int {
        val fileName = StringArgumentType.getString(context, "fileName")
        val list = mutableListOf<Boolean>()
        if (fileName == "ALL") {
            list.add(ConfigManager.reloadConfig(Configs.PLAYERS))
            list.add(ConfigManager.reloadConfig(Configs.WARPS))
            list.add(ConfigManager.reloadConfig(Configs.RULES))
        } else {
            // Reflection will not work cause of inlined reified fun
            when (fileName) {
                "players.json5" -> list.add(ConfigManager.reloadConfig(Configs.PLAYERS))
                "warps.json5" -> list.add(ConfigManager.reloadConfig(Configs.WARPS))
                "rules.json5" -> list.add(ConfigManager.reloadConfig(Configs.RULES))
            }
        }

        if (list.contains(false)) {
            context.source.sendFeedback(Text.literal("Configuration could not be reloaded"), false)
            ManyManyCommandsMod.LOGGER.warn("Configuration could not be reloaded")
        } else {
            context.source.sendFeedback(Text.literal("The configuration was successfully reloaded"), false)
            ManyManyCommandsMod.LOGGER.info("The configuration was successfully reloaded")
        }

        return SINGLE_SUCCESS
    }

}