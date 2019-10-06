package com.Ben12345rocks.VotingPlugin.bungee;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;

public class VotingPluginBungeeCommand extends Command {
	private Bungee bungee;

	public VotingPluginBungeeCommand(Bungee bungee) {
		super("votingpluginbungee");
		this.bungee = bungee;
	}

	@Override
	public void execute(CommandSender sender, String[] args) {
		if (args.length > 0) {
			if (args[0].equalsIgnoreCase("reload")) {
				bungee.reload();
				sender.sendMessage(new TextComponent("Reloading VotingPluginBungee"));
			}
			if (args[0].equalsIgnoreCase("vote")) {
				if (args.length >= 2) {
					String user = args[1];
					String site = args[2];
					bungee.saveVote(user, site);
				}
			}
		}
	}
}
