
package com.bencodez.votingplugin.voteparty;

import java.util.ArrayList;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import com.bencodez.advancedcore.api.messages.StringParser;
import com.bencodez.advancedcore.api.misc.ArrayUtils;
import com.bencodez.advancedcore.api.misc.MiscUtils;
import com.bencodez.advancedcore.api.rewards.RewardBuilder;
import com.bencodez.advancedcore.api.time.events.DayChangeEvent;
import com.bencodez.advancedcore.api.time.events.MonthChangeEvent;
import com.bencodez.advancedcore.api.user.UUID;
import com.bencodez.votingplugin.VotingPluginMain;
import com.bencodez.votingplugin.events.VotePartyEvent;
import com.bencodez.votingplugin.user.UserManager;
import com.bencodez.votingplugin.user.VotingPluginUser;

// TODO: Auto-generated Javadoc
/**
 * The Class VoteParty.
 */
public class VoteParty implements Listener {

	private VotingPluginMain plugin;

	public VoteParty(VotingPluginMain plugin) {
		this.plugin = plugin;
	}

	public void addTotal(VotingPluginUser user) {
		setTotalVotes(getTotalVotes() + 1);
		user.setVotePartyVotes(user.getVotePartyVotes() + 1);
	}

	/**
	 * Adds the vote player.
	 *
	 * @param user the user
	 */
	public void addVotePlayer(VotingPluginUser user) {
		String uuid = user.getUUID();
		ArrayList<String> voted = getVotedUsers();
		if (voted == null) {
			voted = new ArrayList<String>();
		}
		if (!voted.contains(uuid)) {
			voted.add(uuid);
			setVotedUsers(voted);
		}
	}

	/**
	 * Check.
	 */
	public void check() {
		if (getTotalVotes() >= getVotesRequired()) {
			setTotalVotes(getTotalVotes() - getVotesRequired());

			VotePartyEvent event = new VotePartyEvent();
			Bukkit.getPluginManager().callEvent(event);
			if (event.isCancelled()) {
				return;
			}
			MiscUtils.getInstance().broadcast(plugin.getSpecialRewardsConfig().getVotePartyBroadcast());
			giveRewards();

			if (plugin.getSpecialRewardsConfig().getVotePartyIncreaseVotesRquired() > 0) {
				plugin.getServerData().setVotePartyExtraRequired(plugin.getServerData().getVotePartyExtraRequired()
						+ plugin.getSpecialRewardsConfig().getVotePartyIncreaseVotesRquired());
			}
		}

	}

	/**
	 * Command vote party.
	 *
	 * @param sender the sender
	 */
	public void commandVoteParty(CommandSender sender) {
		if (plugin.getSpecialRewardsConfig().getVotePartyEnabled()) {
			ArrayList<String> msg = plugin.getConfigFile().getFormatCommandsVoteParty();
			int votesRequired = getVotesRequired();
			int votes = getTotalVotes();
			int neededVotes = votesRequired - votes;
			HashMap<String, String> placeholders = new HashMap<String, String>();
			placeholders.put("votesrequired", "" + votesRequired);
			placeholders.put("neededvotes", "" + neededVotes);
			placeholders.put("votes", "" + votes);
			msg = ArrayUtils.getInstance().colorize(ArrayUtils.getInstance().replacePlaceHolder(msg, placeholders));
			sender.sendMessage(ArrayUtils.getInstance().convert(msg));
		} else {
			sender.sendMessage(StringParser.getInstance().colorize("&cVoteParty not enabled"));
		}
	}

	/**
	 * Gets the needed votes.
	 *
	 * @return the needed votes
	 */
	public int getNeededVotes() {
		int votesRequired = getVotesRequired();
		int votes = getTotalVotes();
		int neededVotes = votesRequired - votes;
		return neededVotes;
	}

	/**
	 * Gets the total votes.
	 *
	 * @return the total votes
	 */
	public int getTotalVotes() {
		return plugin.getServerData().getData().getInt("VoteParty.Total");
	}

	/**
	 * Gets the voted users.
	 *
	 * @return the voted users
	 */
	@SuppressWarnings("unchecked")
	public ArrayList<String> getVotedUsers() {
		ArrayList<String> list = (ArrayList<String>) plugin.getServerData().getData().getList("VoteParty.Voted");
		if (list != null) {
			return list;
		}
		return new ArrayList<String>();
	}

	public int getVotesRequired() {
		int required = plugin.getSpecialRewardsConfig().getVotePartyVotesRequired();
		int extra = plugin.getServerData().getVotePartyExtraRequired();
		if (extra > 0) {
			return required + extra;
		}
		return required;
	}

	public void giveReward(VotingPluginUser user) {
		if (plugin.getSpecialRewardsConfig().getVotePartyUserVotesRequired() > 0) {
			if (user.getVotePartyVotes() < plugin.getSpecialRewardsConfig().getVotePartyUserVotesRequired()) {
				return;
			}
		}
		giveReward(user, user.isOnline());
	}

	public void giveReward(VotingPluginUser user, boolean online) {
		new RewardBuilder(plugin.getSpecialRewardsConfig().getData(),
				plugin.getSpecialRewardsConfig().getVotePartyRewardsPath()).setOnline(online)
						.withPlaceHolder("VotesRequired",
								"" + plugin.getSpecialRewardsConfig().getVotePartyVotesRequired())
						.send(user);
	}

	/**
	 * Give rewards.
	 */
	public void giveRewards() {
		for (final String cmd : plugin.getSpecialRewardsConfig().getVotePartyCommands()) {
			Bukkit.getScheduler().runTask(plugin, new Runnable() {

				@Override
				public void run() {
					Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), cmd);
				}

			});
		}

		if (plugin.getSpecialRewardsConfig().getVotePartyGiveAllPlayers()) {
			plugin.debug("Trying to give all players vote party");
			for (String uuid : UserManager.getInstance().getAllUUIDs()) {
				VotingPluginUser user = UserManager.getInstance().getVotingPluginUser(new UUID(uuid));
				giveReward(user);
			}
		} else {
			plugin.debug("Trying to give all online players vote party");
			plugin.debug(ArrayUtils.getInstance().makeStringList(getVotedUsers()));
			for (String uuid : getVotedUsers()) {
				VotingPluginUser user = UserManager.getInstance().getVotingPluginUser(new UUID(uuid));
				giveReward(user);
			}
		}
		reset();
	}

	@EventHandler
	public void onDayChange(DayChangeEvent event) {
		if (plugin.getSpecialRewardsConfig().getVotePartyResetEachDay()) {
			reset();
		}
	}

	@EventHandler
	public void onMonthChange(MonthChangeEvent event) {
		if (plugin.getSpecialRewardsConfig().getVotePartyResetMontly()) {
			reset();
		}

		if (plugin.getSpecialRewardsConfig().isVotePartyResetExtraVotesMonthly()) {
			plugin.getServerData().setVotePartyExtraRequired(0);
		}
	}

	public void register() {
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}

	public void reset() {
		setVotedUsers(new ArrayList<String>());
		setTotalVotes(0);
		for (String uuid : UserManager.getInstance().getAllUUIDs()) {
			VotingPluginUser user = UserManager.getInstance().getVotingPluginUser(new UUID(uuid));
			if (user.getVotePartyVotes() != 0) {
				user.setVotePartyVotes(0);
			}
		}

	}

	/**
	 * Sets the total votes.
	 *
	 * @param value the new total votes
	 */
	public void setTotalVotes(int value) {
		plugin.getServerData().getData().set("VoteParty.Total", value);
		plugin.getServerData().saveData();
	}

	/**
	 * Sets the voted users.
	 *
	 * @param value the new voted users
	 */
	public void setVotedUsers(ArrayList<String> value) {
		plugin.getServerData().getData().set("VoteParty.Voted", value);
		plugin.getServerData().saveData();
	}

	public synchronized void vote(VotingPluginUser user, boolean realVote) {
		if (plugin.getSpecialRewardsConfig().getVotePartyEnabled()) {
			if (plugin.getSpecialRewardsConfig().getVotePartyCountFakeVotes() || realVote) {
				if (plugin.getSpecialRewardsConfig().getVotePartyCountOfflineVotes() || user.isOnline()) {
					addTotal(user);
					addVotePlayer(user);
					check();
				}
			}
		}
	}

}
