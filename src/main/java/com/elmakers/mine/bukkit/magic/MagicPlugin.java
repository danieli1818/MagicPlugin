package com.elmakers.mine.bukkit.magic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;

import com.elmakers.mine.bukkit.api.block.UndoList;
import com.elmakers.mine.bukkit.api.magic.*;
import com.elmakers.mine.bukkit.api.spell.SpellCategory;
import com.elmakers.mine.bukkit.block.MaterialAndData;
import com.elmakers.mine.bukkit.citizens.CitizensController;
import com.elmakers.mine.bukkit.magic.command.*;
import com.elmakers.mine.bukkit.utility.ConfigurationUtils;
import com.elmakers.mine.bukkit.utility.InventoryUtils;
import com.elmakers.mine.bukkit.utility.NMSUtils;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.MemorySection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import com.elmakers.mine.bukkit.api.spell.SpellTemplate;
import com.elmakers.mine.bukkit.api.wand.LostWand;
import com.elmakers.mine.bukkit.wand.Wand;

/*! \mainpage Magic Bukkit Plugin
*
* \section intro_sec Introduction
*
* This is the documentation for the MagicPlugin. If you are looking to
* integrate with Magic (but not extend it), see the MagicAPI:
* 
* http://jenkins.elmakers.com/job/MagicAPI/doxygen/
* 
* Building against MagicPlugin directly is only necessary if you want
* to extend Magic, such as adding a new Spell or EffectPlayer.
* 
* \section issues_sec Issues
* 
* For issues, bugs, feature requests, spell ideas, use our issue tracker:
* 
* http://jira.elmakers.com/browse/MAGIC/
* 
* \section start_sec Getting Started
* 
* If you haven't done so already, get started with Bukkit by getting a basic
* shell of a plugin working. You should at least have a working Plugin that
* loads in Bukkit (add a debug print to onEnable to be sure!) before you
* start trying to integrate with other Plugins. See here for general help:
* 
* http://wiki.bukkit.org/Plugin_Tutorial
* 
* \section maven_sec Building with Maven
* 
* Once you have a project set up, it is easy to build against the Magic API
* with Maven. Simply add the elmakers repository to your repository list,
* and then add a dependency for MagicAPI. A typical setup would look like:
* 
* <pre>
* &lt;dependencies&gt;
* &lt;dependency&gt;
* 	&lt;groupId&gt;org.bukkit&lt;/groupId&gt;
* 	&lt;artifactId&gt;bukkit&lt;/artifactId&gt;
* 	&lt;version&gt;1.6.4-R2.0&lt;/version&gt;
* 	&lt;scope&gt;provided&lt;/scope&gt;
* &lt;/dependency&gt;
* &lt;dependency&gt;
* 	&lt;groupId&gt;com.elmakers.mine.bukkit&lt;/groupId&gt;
* 	&lt;artifactId&gt;MagicAPI&lt;/artifactId&gt;
* 	&lt;version&gt;1.0&lt;/version&gt;
* 	&lt;scope&gt;provided&lt;/scope&gt;
* &lt;/dependency&gt;
* &lt;dependency&gt;
* 	&lt;groupId&gt;com.elmakers.mine.bukkit.plugins&lt;/groupId&gt;
* 	&lt;artifactId&gt;Magic&lt;/artifactId&gt;
* 	&lt;version&gt;3.0-RC1&lt;/version&gt;
* 	&lt;scope&gt;provided&lt;/scope&gt;
* &lt;/dependency&gt;
* &lt;/dependencies&gt;
* &lt;repositories&gt;
* &lt;repository&gt;
*     &lt;id&gt;elmakers-repo&lt;/id&gt;
*     &lt;url&gt;http://maven.elmakers.com/repository/ &lt;/url&gt;
* &lt;/repository&gt;
* &lt;repository&gt;
*     &lt;id&gt;bukkit-repo&lt;/id&gt;
*     &lt;url&gt;http://repo.bukkit.org/content/groups/public/ &lt;/url&gt;
* &lt;/repository&gt;
* &lt;/repositories&gt;
* </pre>
* 
* \section example_sec Examples
*
* \subsection casting Casting Spells
* 
* A plugin may cast spells directly, or on behalf of logged in players.
* 
* \subsection wands Creating Wands
* 
* A plugin may create or modify Wand items.
*/

/**
 * This is the main Plugin class for Magic.
 * 
 * An integrating Plugin should generally cast this to MagicAPI and
 * use the API interface when interacting with Magic.
 *
 */
public class MagicPlugin extends JavaPlugin implements MagicAPI
{
    /*
     * Singleton Plugin instance
     */
    private static MagicPlugin instance;

	/*
	 * Private data
	 */	
	private MagicController controller = null;

	/*
	 * Plugin interface
	 */

    public MagicPlugin()
    {
        instance = this;
    }

	public void onEnable() 
	{
        if (NMSUtils.isLegacy()) {
            getLogger().info("Enabling 1.7 support - Magic may stop supporting 1.7 in version 5!");
        }
		if (controller == null) {
			controller = new MagicController(this);
		}

        if (NMSUtils.getFailed()) {
            getLogger().warning("Something went wrong with some Deep Magic, plugin will not load.");
            getLogger().warning("Please make sure you are running a compatible version of CraftBukkit or Spigot!");
        } else {
            initialize();
        }
	}

	protected void initialize()
	{
		controller.initialize();
		
		TabExecutor magicCommand = new MagicCommandExecutor(this);
		getCommand("magic").setExecutor(magicCommand);
		getCommand("magic").setTabCompleter(magicCommand);
        TabExecutor magicGiveCommand = new MagicGiveCommandExecutor(this);
        getCommand("mgive").setExecutor(magicGiveCommand);
        getCommand("mgive").setTabCompleter(magicGiveCommand);
        TabExecutor magicSkillsCommand = new MagicSkillsCommandExecutor(this);
        getCommand("mskills").setExecutor(magicSkillsCommand);
        getCommand("mskills").setTabCompleter(magicSkillsCommand);
		TabExecutor castCommand = new CastCommandExecutor(this);
		getCommand("cast").setExecutor(castCommand);
		getCommand("cast").setTabCompleter(castCommand);
		getCommand("castp").setExecutor(castCommand);
		getCommand("castp").setTabCompleter(castCommand);
		TabExecutor wandCommand = new WandCommandExecutor(this);
		getCommand("wand").setExecutor(wandCommand);
		getCommand("wand").setTabCompleter(wandCommand);
		getCommand("wandp").setExecutor(wandCommand);
		getCommand("wandp").setTabCompleter(wandCommand);
		TabExecutor spellsCommand = new SpellsCommandExecutor(this);
		getCommand("spells").setExecutor(spellsCommand);
        CitizensController citizens = controller.getCitizens();
        if (citizens != null)
        {
            TabExecutor magicTraitCommand = new MagicTraitCommandExecutor(this, citizens);
            getCommand("mtrait").setExecutor(magicTraitCommand);
            getCommand("mtrait").setTabCompleter(magicTraitCommand);
        }
	}

	/* 
	 * Help commands
	 */

	public void onDisable() 
	{
        if (controller != null) {
            controller.clear();
            controller.save();
        }
	}

	/*
	 * API Implementation
	 */
	
	@Override
	public Plugin getPlugin() {
		return this;
	}

	@Override
	public boolean hasPermission(CommandSender sender, String pNode) {
		return controller.hasPermission(sender, pNode);
	}

	@Override
	public boolean hasPermission(CommandSender sender, String pNode, boolean defaultPermission) {
		return controller.hasPermission(sender, pNode, defaultPermission);
	}

	@Override
	public void save() {
		controller.save();
	}

	@Override
	public void reload() {
		controller.loadConfiguration();
	}

	@Override
	public void clearCache() {
		controller.clearCache();
	}
	
	@Override
	public boolean commit() {
		return controller.commitAll();
	}
	
	@Override
	public Collection<com.elmakers.mine.bukkit.api.magic.Mage> getMages() {
		return controller.getMages();
	}
	
	@Override
	public Collection<com.elmakers.mine.bukkit.api.magic.Mage> getMagesWithPendingBatches() {
		Collection<com.elmakers.mine.bukkit.api.magic.Mage> mages = new ArrayList<com.elmakers.mine.bukkit.api.magic.Mage>();
		Collection<com.elmakers.mine.bukkit.api.magic.Mage> internal = controller.getPending();
		mages.addAll(internal);
		return mages;
	}

    @Override
    public Collection<UndoList> getPendingUndo() {
        Collection<UndoList> undo = new ArrayList<UndoList>();
        undo.addAll(controller.getPendingUndo());
        return undo;
    }
	
	@Override
	public Collection<LostWand> getLostWands() {
		Collection<LostWand> lostWands = new ArrayList<LostWand>();
		lostWands.addAll(controller.getLostWands());
		return lostWands;
	}
	
	@Override
	public Collection<Automaton> getAutomata() {
		Collection<Automaton> automata = new ArrayList<Automaton>();
		automata.addAll(controller.getAutomata());
		return automata;
	}

	@Override
	public void removeLostWand(String id) {
		controller.removeLostWand(id);
	}

	@Override
	public com.elmakers.mine.bukkit.api.wand.Wand getWand(ItemStack itemStack) {
		return new Wand(controller, itemStack);
	}

    @Override
	public boolean isWand(ItemStack item) {
		return Wand.isWand(item);
	}

    @Override
    public String getSpell(ItemStack item) {
        return Wand.getSpell(item);
    }

    @Override
    public boolean isBrush(ItemStack item) {
        return Wand.isBrush(item);
    }

    @Override
    public boolean isSpell(ItemStack item) {
        return Wand.isSpell(item);
    }

    @Override
    public String getBrush(ItemStack item) {
        return Wand.getBrush(item);
    }

    @Override
    public boolean isUpgrade(ItemStack item) {
        return Wand.isUpgrade(item);
    }

	@Override
	public void giveItemToPlayer(Player player, ItemStack itemStack) {
		controller.giveItemToPlayer(player, itemStack);
	}

    @Override
    public void giveExperienceToPlayer(Player player, int xp) {
        if (controller.isMage(player)) {
            com.elmakers.mine.bukkit.api.magic.Mage mage = controller.getMage(player);
            mage.giveExperience(xp);
        } else {
            player.giveExp(xp);
        }
    }

	@Override
	public com.elmakers.mine.bukkit.api.magic.Mage getMage(CommandSender sender) {
		return controller.getMage(sender);
	}

	@Override
	public com.elmakers.mine.bukkit.api.magic.Mage getMage(Entity entity, CommandSender sender) {
		return controller.getMage(entity);
	}

    @Override
    public String describeItem(ItemStack item) {
        return controller.describeItem(item);
    }

    @Override
    public ItemStack createItem(String magicKey) {
        return createItem(magicKey, null);
    }

    @Override
    public ItemStack createItem(String magicKey, com.elmakers.mine.bukkit.api.magic.Mage mage) {
        ItemStack itemStack = null;
        if (controller == null) {
            getLogger().log(Level.WARNING, "Calling API before plugin is initialized");
            return null;
        }

        // Handle : or | as delimiter
        String magicItemKey = magicKey.replace("|", ":");

        try {
            if (magicItemKey.contains("skull:") || magicItemKey.contains("skull_item:")) {
                magicItemKey = magicItemKey.replace("skull:", "skull_item:");
                MaterialAndData skullData = new MaterialAndData(magicItemKey);
                itemStack = skullData.getItemStack(1);
            } else if (magicItemKey.contains("book:")) {
                String bookCategory = magicItemKey.substring(5);
                SpellCategory category = null;

                if (!bookCategory.isEmpty() && !bookCategory.equalsIgnoreCase("all")) {
                    category = controller.getCategory(bookCategory);
                    if (category == null) {
                        return null;
                    }
                }
                itemStack = getSpellBook(category, 1);
            } else if (magicItemKey.contains("spell:")) {
                String spellKey = magicKey.substring(6);
                itemStack = createSpellItem(spellKey);
            } else if (magicItemKey.contains("skill:")) {
                String spellKey = magicKey.substring(6);
                itemStack =  Wand.createSpellItem(spellKey, controller, mage, null, false);
                InventoryUtils.setMeta(itemStack, "skill", "true");
            } else if (magicItemKey.contains("wand:")) {
                String wandKey = magicItemKey.substring(5);
                com.elmakers.mine.bukkit.api.wand.Wand wand = createWand(wandKey);
                if (wand != null) {
                    itemStack = wand.getItem();
                }
            } else if (magicItemKey.contains("upgrade:")) {
                String wandKey = magicItemKey.substring(8);
                com.elmakers.mine.bukkit.api.wand.Wand wand = createWand(wandKey);
                if (wand != null) {
                    wand.makeUpgrade();
                    itemStack = wand.getItem();
                }
            } else if (magicItemKey.contains("brush:")) {
                String brushKey = magicItemKey.substring(6);
                itemStack = createBrushItem(brushKey);
            } else if (magicItemKey.contains("item:")) {
                String itemKey = magicItemKey.substring(5);
                itemStack = createGenericItem(itemKey);
            } else {
                MaterialAndData item = new MaterialAndData(magicItemKey);
                if (item.isValid()) {
                    return item.getItemStack(1);
                }
                com.elmakers.mine.bukkit.api.wand.Wand wand = createWand(magicKey);
                if (wand != null) {
                    return wand.getItem();
                }
                itemStack = createSpellItem(magicKey);
                if (itemStack != null) {
                    return itemStack;
                }
                itemStack = createBrushItem(magicItemKey);
            }

        } catch (Exception ex) {
            getLogger().log(Level.WARNING, "Error creating item: " + magicItemKey, ex);
        }

        return itemStack;
    }

    @Override
    public ItemStack createGenericItem(String itemKey) {
        return controller.createItem(itemKey);
    }

    @Override
	public com.elmakers.mine.bukkit.api.wand.Wand createWand(String wandKey) {
		return Wand.createWand(controller, wandKey);
	}

    @Override
    public com.elmakers.mine.bukkit.api.wand.Wand createUpgrade(String wandKey) {
        Wand wand = Wand.createWand(controller, wandKey);
        if (!wand.isUpgrade()) {
            wand.makeUpgrade();
        }
        return wand;
    }

	@Override
	public ItemStack createSpellItem(String spellKey) {
		return Wand.createSpellItem(spellKey, controller, null, true);
	}

	@Override
	public ItemStack createBrushItem(String brushKey) {
		return Wand.createBrushItem(brushKey, controller, null, true);
	}

	@Override
	public boolean cast(String spellName, String[] parameters) {
		return cast(spellName, parameters, null, null);
	}

	@Override
	public Collection<SpellTemplate> getSpellTemplates() {
		return controller.getSpellTemplates();
	}

	@Override
	public Collection<String> getWandKeys() {
		return Wand.getWandKeys();
	}

	@Override
	public boolean cast(String spellName, String[] parameters, CommandSender sender, Entity entity) {
		ConfigurationSection config = null;
        if (parameters != null && parameters.length > 0) {
            config = new MemoryConfiguration();
            ConfigurationUtils.addParameters(parameters, config);
        }
        return controller.cast(null, spellName, config, sender, entity);
	}

    @Override
    public boolean cast(String spellName, ConfigurationSection parameters, CommandSender sender, Entity entity) {
        return controller.cast(null, spellName, parameters, sender, entity);
    }

    @Override
	public Collection<String> getPlayerNames() {
		return controller.getPlayerNames();
	}

	@Override
	public com.elmakers.mine.bukkit.api.wand.Wand createWand(Material iconMaterial, short iconData) {
		return new Wand(controller, iconMaterial, iconData);
	}

    @Override
    public com.elmakers.mine.bukkit.api.wand.Wand createWand(ItemStack item) {
        return Wand.createWand(controller, item);
    }

	@Override
	public SpellTemplate getSpellTemplate(String key) {
		return controller.getSpellTemplate(key);
	}

	@Override
	public Collection<String> getSchematicNames() {
		return controller.getSchematicNames();
	}

	@Override
	public Collection<String> getBrushes() {
		return controller.getBrushKeys();
	}
	
	@Override
	public MageController getController() {
		return controller;
	}

    @Override
    public ItemStack getSpellBook(SpellCategory category, int count) {
        return controller.getSpellBook(category, count);
    }

    @Override
    public Messages getMessages() {
        return controller.getMessages();
    }

    public static MagicAPI getAPI() {
        return instance;
    }
}
