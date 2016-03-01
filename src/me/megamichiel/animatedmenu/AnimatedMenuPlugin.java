package me.megamichiel.animatedmenu;

import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.Getter;
import me.megamichiel.animatedmenu.command.BroadcastCommand;
import me.megamichiel.animatedmenu.command.Command;
import me.megamichiel.animatedmenu.command.CommandHandler;
import me.megamichiel.animatedmenu.command.ConsoleCommand;
import me.megamichiel.animatedmenu.command.MenuOpenCommand;
import me.megamichiel.animatedmenu.command.MessageCommand;
import me.megamichiel.animatedmenu.command.OpCommand;
import me.megamichiel.animatedmenu.command.ServerCommand;
import me.megamichiel.animatedmenu.command.TellRawCommand;
import me.megamichiel.animatedmenu.menu.AnimatedMenu;
import me.megamichiel.animatedmenu.util.FormulaPlaceholder;
import me.megamichiel.animatedmenu.util.Nagger;
import me.megamichiel.animatedmenu.util.RemoteConnections;
import me.megamichiel.animatedmenu.util.RemoteConnections.ServerInfo;
import me.megamichiel.animatedmenu.util.StringBundle;
import me.megamichiel.animatedmenu.util.StringUtil;
import net.milkbowl.vault.economy.Economy;

import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.NumberConversions;

public class AnimatedMenuPlugin extends JavaPlugin implements Listener, Nagger {
	
	@Getter private static AnimatedMenuPlugin instance;
	
	@Getter
	private final List<CommandHandler> commandHandlers = new ArrayList<CommandHandler>();
	@Getter
	private final MenuRegistry menuRegistry = new MenuRegistry(this);
	
	@Getter private final Map<String, FormulaPlaceholder> formulaPlaceholders = new HashMap<String, FormulaPlaceholder>();
	@Getter private final RemoteConnections connections = new RemoteConnections(this);
	private boolean warnOfflineServers = true;
	
	@Getter private final List<BukkitTask> asyncTasks = new ArrayList<BukkitTask>();
	
	private String update;
	@Getter
	private boolean vaultPresent = false, playerPointsPresent = false;
	public Economy economy;
	public PlayerPointsAPI playerPointsAPI;
	
	@Override
	public void onEnable()
	{
		instance = this;
		
		/* Listeners */
		getServer().getPluginManager().registerEvents(this, this);
		getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
		getCommand("animatedmenu").setExecutor(new AnimatedMenuCommand(this));
		
		/* Config / API */
		saveDefaultConfig();
		
		registerDefaultCommandHandlers();
		try
		{
			Class.forName("me.clip.placeholderapi.PlaceholderAPI");
			AnimatedMenuPlaceholders.register(this);
		}
		catch (Exception ex) {}
		try {
			Class.forName("net.milkbowl.vault.economy.Economy");
			economy = Bukkit.getServicesManager().getRegistration(Economy.class).getProvider();
			vaultPresent = true;
		} catch (Exception ex) {} //No Vault
		Plugin pp = Bukkit.getPluginManager().getPlugin("PlayerPoints");
		if(pp != null) {
			playerPointsAPI = ((PlayerPoints) pp).getAPI();
			playerPointsPresent = true;
		}
		new BukkitRunnable() {
			@Override
			public void run() {
				Bukkit.getPluginManager().callEvent(new AnimatedMenuPreLoadEvent(AnimatedMenuPlugin.this));
				menuRegistry.loadMenus();
				Bukkit.getPluginManager().callEvent(new AnimatedMenuPostLoadEvent(AnimatedMenuPlugin.this));
			}
		}.runTask(this);
		
		/* Other Stuff */
		checkForUpdate();
		loadConfig();
		
		asyncTasks.add(Bukkit.getScheduler().runTaskTimerAsynchronously(this, menuRegistry, 0, 0));
	}
	
	@Override
	public void onDisable()
	{
		for (Player p : new HashSet<Player>(menuRegistry.getOpenMenu().keySet())) { // InventoryCloseEvent could cause ConcurrentModificationException
			p.closeInventory();
		}
		for (BukkitTask task : asyncTasks)
			task.cancel();
		asyncTasks.clear();
<<<<<<< HEAD
		connections.cancel();
=======
>>>>>>> 41fa8baba6eb1ea2c9a35c62bff02fad1e1de5a3
	}
	
	private void checkForUpdate()
	{
		new BukkitRunnable() {
			@Override
			public void run() {
				String current = getDescription().getVersion();
				try
				{
					URLConnection connection = new URL("https://github.com/megamichiel/AnimatedMenu").openConnection();
					Scanner scanner = new Scanner(connection.getInputStream());
					scanner.useDelimiter("\\Z");
					String content = scanner.next();
					Matcher matcher = Pattern.compile("Current\\sVersion:\\s(<b>)?([0-9]\\.[0-9]\\.[0-9])(</b>)?").matcher(content);
					matcher.find();
					String version = matcher.group(2);
					update = current.equals(version) ? null : version;
					scanner.close();
				}
				catch (Exception ex)
				{
					nag("Failed to check for updates:");
					nag(ex);
				}
				if (update != null)
				{
					getLogger().info("A new version is available! (Current version: " + current + ", new version: " + update + ")");
				}
			}
		}.runTaskAsynchronously(this);
	}
	
	protected void registerDefaultCommandHandlers()
	{
		commandHandlers.clear();
		commandHandlers.add(new CommandHandler("console") {
			@Override
			public Command getCommand(Nagger nagger, String command) {
				return new ConsoleCommand(nagger, command);
			}
		});
		commandHandlers.add(new CommandHandler("message") {
			@Override
			public Command getCommand(Nagger nagger, String command) {
				return new MessageCommand(nagger, command);
			}
		});
		commandHandlers.add(new CommandHandler("op") {
			@Override
			public Command getCommand(Nagger nagger, String command) {
				return new OpCommand(nagger, command);
			}
		});
		commandHandlers.add(new CommandHandler("broadcast") {
			@Override
			public Command getCommand(Nagger nagger, String command) {
				return new BroadcastCommand(nagger, command);
			}
		});
		commandHandlers.add(new CommandHandler("server") {
			@Override
			public Command getCommand(Nagger nagger, String command) {
				return new ServerCommand(nagger, command);
			}
		});
		commandHandlers.add(new CommandHandler("menu") {
			@Override
			public Command getCommand(Nagger nagger, String command) {
				return new MenuOpenCommand(nagger, command);
			}
		});
		commandHandlers.add(new CommandHandler("tellraw") {
			@Override
			public Command getCommand(Nagger nagger, String command) {
				return new TellRawCommand(nagger, command);
			}
		});
	}
	
	public boolean warnOfflineServers()
	{
		return warnOfflineServers;
	}
	
	protected void loadConfig()
	{
		if (getConfig().isConfigurationSection("Formulas"))
		{
			ConfigurationSection section = getConfig().getConfigurationSection("Formulas");
			for (String key : section.getKeys(false))
			{
				String val = section.getString(key);
				if (val != null)
				{
					formulaPlaceholders.put(key.toLowerCase(), new FormulaPlaceholder(this, val));
				}
			}
		}
		if (getConfig().isConfigurationSection("Connections"))
		{
			ConfigurationSection section = getConfig().getConfigurationSection("Connections");
			for (String key : section.getKeys(false))
			{
				if (section.isConfigurationSection(key))
				{
					ConfigurationSection sec = section.getConfigurationSection(key);
					String ip = sec.getString("ip");
					if (ip != null)
					{
						int colonIndex = ip.indexOf(':');
						int port = colonIndex == -1 ? 25565 : NumberConversions.toInt(ip.substring(colonIndex + 1));
						if (colonIndex > -1)
							ip = ip.substring(0, colonIndex);
						ServerInfo serverInfo = connections.add(key.toLowerCase(), new InetSocketAddress(ip, port));
						Map<StringBundle, StringBundle> map = serverInfo.getValues();
						for (String key2 : sec.getKeys(false))
						{
							if (!key2.equals("ip"))
							{
								String val = sec.getString(key2);
								if (val != null)
									map.put(StringUtil.parseBundle(this, key2).colorAmpersands(),
											StringUtil.parseBundle(this, val).colorAmpersands());
							}
						}
					}
				}
			}
		}
		warnOfflineServers = getConfig().getBoolean("Warn-Offline-Servers");
		connections.schedule(getConfig().getLong("Connection-Refresh-Delay", 10 * 20L));
	}
	
	void reload()
	{
		reloadConfig();
		
		formulaPlaceholders.clear();
		connections.clear();
		connections.cancel();
		loadConfig();
		
		registerDefaultCommandHandlers();
		Bukkit.getPluginManager().callEvent(new AnimatedMenuPreLoadEvent(this));
		menuRegistry.loadMenus();
		Bukkit.getPluginManager().callEvent(new AnimatedMenuPostLoadEvent(this));
	}
	
	/* Listeners */
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerJoin(PlayerJoinEvent e)
	{
		final Player player = e.getPlayer();
		if (update != null && player.hasPermission("animatedmenu.seeupdate"))
		{
			player.sendMessage("�8[�6" + getDescription().getName() + "�8] �aA new version is available! (Current version: " + getDescription().getVersion() + ", new version: " + update + ")");
		}
		for (final AnimatedMenu menu : menuRegistry)
		{
			if (menu.getSettings().getOpener() != null && menu.getSettings().getOpenerJoinSlot() > -1)
			{
				player.getInventory().setItem(menu.getSettings().getOpenerJoinSlot(), menu.getSettings().getOpener());
			}
			if (menu.getSettings().shouldOpenOnJoin())
			{
				getServer().getScheduler().runTask(this, new Runnable() {
					@Override
					public void run() {
						menuRegistry.openMenu(player, menu);
					}
				});
			}
		}
	}
	
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent e) {
		if (!e.getPlayer().hasPermission("animatedmenu.open"))
			return;
		if(e.getItem() != null)
		{
			for(AnimatedMenu menu : menuRegistry)
			{
				ItemStack item = menu.getSettings().getOpener();
				if(item != null && item.getData().equals(e.getItem().getData()))
				{
					ItemMeta meta = e.getItem().getItemMeta();
					ItemMeta meta1 = item.getItemMeta();
					if(menu.getSettings().hasOpenerName() && !compare(meta.getDisplayName(), meta1.getDisplayName()))
						continue;
					if(menu.getSettings().hasOpenerLore() && !compare(meta.getLore(), meta1.getLore()))
						continue;
					menuRegistry.openMenu(e.getPlayer(), menu);
					e.setCancelled(true);
					return;
				}
			}
		}
	}
	
	private boolean compare(Object o1, Object o2)
	{
		return o1 == null ? o2 == null : o1.equals(o2);
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent e)
	{
		Player p = e.getPlayer();
		String cmd = e.getMessage().substring(1).split(" ")[0].toLowerCase();
		for(AnimatedMenu menu : menuRegistry)
		{
			if(cmd.equals(menu.getSettings().getOpenCommand()))
			{
				menuRegistry.openMenu(p, menu);
				e.setCancelled(true);
				return;
			}
		}
	}
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onInventoryClose(InventoryCloseEvent e)
	{
		AnimatedMenu menu = menuRegistry.getOpenMenu().remove(e.getPlayer());
		if (menu != null) menu.handleMenuClose(e.getPlayer());
	}
	
	@EventHandler
	public void onInventoryClick(InventoryClickEvent e)
	{
		if(!(e.getWhoClicked() instanceof Player)) // Not sure if they can't be a player, but just for safety
			return;
		Player p = (Player) e.getWhoClicked();
		AnimatedMenu open = menuRegistry.getOpenedMenu(p);
		if(open == null) return;
		e.setCancelled(true);
		int slot = e.getRawSlot();
		InventoryView view = e.getView();
		if ((view.getTopInventory() != null) && slot >= 0 && (slot < view.getTopInventory().getSize()))
		{
			open.getMenuGrid().click(p, e.getClick(), e.getSlot());
		}
	}
	
	@Override
	public void nag(String message)
	{
		getLogger().warning(message);
	}
	
	@Override
	public void nag(Throwable throwable)
	{
		getLogger().warning(throwable.getClass().getName() + ": " + throwable.getMessage());
	}
}
