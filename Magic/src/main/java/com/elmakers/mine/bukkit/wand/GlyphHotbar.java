package com.elmakers.mine.bukkit.wand;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import com.elmakers.mine.bukkit.api.magic.Mage;
import com.elmakers.mine.bukkit.api.magic.MageController;
import com.elmakers.mine.bukkit.api.magic.Messages;
import com.elmakers.mine.bukkit.api.spell.CastingCost;
import com.elmakers.mine.bukkit.api.spell.MageSpell;
import com.elmakers.mine.bukkit.api.spell.Spell;
import com.elmakers.mine.bukkit.utility.ConfigurationUtils;

public class GlyphHotbar {
    private final Wand wand;
    private boolean skipEmpty;
    private int hotbarSlotWidth;
    private int hotbarActiveSlotWidth;
    private int iconWidth;
    private int slotSpacingWidth;
    private int manaBarWidth;
    private int flashTime;
    private int collapsedWidth;
    private int collapsedFinalSpacing;
    private WandDisplayMode barMode;
    private boolean showCooldown;

    protected String extraMessage;
    protected long lastExtraMessage;
    protected int extraMessageDelay;
    protected int extraAnimationTime;
    protected long lastInsufficientResource;
    protected long lastInsufficientCharges;
    protected long lastCooldown;
    protected Spell lastCooldownSpell;

    public GlyphHotbar(Wand wand) {
        this.wand = wand;
    }

    public void load(ConfigurationSection configuration) {
        if (configuration == null) {
            configuration = ConfigurationUtils.newConfigurationSection();
        }

        skipEmpty = configuration.getBoolean("skip_empty", true);
        hotbarSlotWidth = configuration.getInt("slot_width", 22);
        hotbarActiveSlotWidth = configuration.getInt("active_slot_width", 22);
        iconWidth = configuration.getInt("icon_width", 16);
        slotSpacingWidth = configuration.getInt("slot_spacing", -1);
        manaBarWidth = configuration.getInt("bar_width", 128);
        flashTime = configuration.getInt("flash_duration", 300);
        extraMessageDelay = configuration.getInt("extra_display_time", 2000);
        extraAnimationTime = configuration.getInt("extra_animate_time", 500);
        collapsedWidth = configuration.getInt("collapsed_slot_width", 6);
        collapsedFinalSpacing = configuration.getInt("collapsed_spacing", 12);
        showCooldown = configuration.getBoolean("show_cooldown", true);

        barMode = WandDisplayMode.parse(wand.getController(), configuration, "bar_mode", WandDisplayMode.MANA);
    }

    public String getGlyphs() {
        MageController controller = wand.getController();
        Messages messages = controller.getMessages();
        WandInventory hotbar = wand.getActiveHotbar();
        Mage mage = wand.getMage();
        if (hotbar == null || mage == null) return "";

        // Animation when showing extra message
        int hotbarActivePaddingLeft = (hotbarActiveSlotWidth - hotbarSlotWidth) / 2;
        int iconPaddingLeft = (hotbarSlotWidth - iconWidth) / 2;
        int iconPaddingRight = (hotbarSlotWidth - iconWidth) - iconPaddingLeft;
        int collapseSpace = 0;
        int finalSpace = 0;
        long now = System.currentTimeMillis();
        boolean hasExtraMessage = extraMessage != null && extraAnimationTime > 0;
        if (hasExtraMessage) {
            if (now < lastExtraMessage + extraAnimationTime) {
                collapseSpace = (int)Math.ceil((hotbarSlotWidth - collapsedWidth) * (now - lastExtraMessage) / extraAnimationTime);
                finalSpace = (int)Math.ceil(collapsedFinalSpacing * (now - lastExtraMessage) / extraAnimationTime);
            } else {
                collapseSpace = hotbarSlotWidth - collapsedWidth;
                finalSpace = collapsedFinalSpacing;
            }
        }
        String collapseReverse = messages.getSpace(-collapseSpace);
        String finalPadding = messages.getSpace(finalSpace);

        // Icon width + 1 pixel padding, to reverse back over the icon (for applying cooldown)
        String iconReverse = messages.getSpace(-(iconWidth + 1));

        // Hotbar slot border + 1 pixel padding, to reverse back over the hotbar slot
        String hotbarSlotReverse = messages.getSpace(-(hotbarSlotWidth + 1));

        // Padding between icon and the slot border on either side
        String hotbarIconPaddingLeft = messages.getSpace(iconPaddingLeft);
        String hotbarIconPaddingRight = messages.getSpace(iconPaddingRight);

        // Amount to reverse back past a hotbar slot background start before placing
        // The active slot overlay
        // Generally the active slot overlay is larger than the slot, so we have to back up
        // farther and then go forward farther as well
        // Also need to add in one extra pixel of space as usual
        String hotbarActiveSlotStart = messages.getSpace(-hotbarActivePaddingLeft);

        // How far to move back after adding the active overlay, to the beginner of the hotbar slot background
        String hotbarActiveSlotEnd = messages.getSpace(-(1 + hotbarActiveSlotWidth + hotbarActivePaddingLeft));

        // Configurable space between each slot
        String slotSpacing = messages.getSpace(slotSpacingWidth);
        String glyphs = "";

        // Create the hotbar
        int hotbarSlots = 0;
        String hotbarSlot = messages.get("gui.hotbar.hotbar_slot");
        String hotbarSlotActive = messages.get("gui.hotbar.hotbar_slot_active");
        String emptyIcon = messages.get("gui.icons.empty");
        for (ItemStack hotbarItem : hotbar.items) {
            String icon;
            Spell spell = wand.getSpell(Wand.getSpell(hotbarItem));
            String spellKey = null;
            if (spell == null) {
                if (skipEmpty) continue;
                icon = emptyIcon;
            } else {
                icon = spell.getGlyph();
                spellKey = spell.getSpellKey().getBaseKey();
            }

            // Add hotbar slot background
            glyphs += hotbarSlot;
            glyphs += hotbarSlotReverse;

            // Add active overlay
            String activeSpell = wand.getBaseActiveSpell();
            if (spellKey != null && activeSpell != null && spellKey.equals(activeSpell) && !hotbarSlotActive.isEmpty()) {
                glyphs += hotbarActiveSlotStart;
                glyphs += hotbarSlotActive;
                glyphs += hotbarActiveSlotEnd;
            }

            // Add icon with padding
            glyphs += hotbarIconPaddingLeft;
            glyphs += icon;

            // Add cooldown/disabled indicators
            if (showCooldown) {
                if (flashTime > 0 && now < lastCooldown + flashTime && lastCooldownSpell != null && lastCooldownSpell.getSpellKey().equals(spell.getSpellKey())) {
                    String cooldownIcon = messages.get("gui.cooldown.wait", "");
                    if (!cooldownIcon.isEmpty()) {
                        glyphs += iconReverse;
                        glyphs += cooldownIcon;
                    }
                } else {
                    int cooldownLevel = 0;
                    Long timeToCast = spell != null && spell instanceof MageSpell ? ((MageSpell)spell).getTimeToCast() : null;
                    Long maxTimeToCast = spell != null && spell instanceof MageSpell ? ((MageSpell)spell).getMaxTimeToCast() : null;

                    if (timeToCast == null || maxTimeToCast == null) {
                        cooldownLevel = 16;
                    } else if (maxTimeToCast > 0) {
                        cooldownLevel = (int)Math.ceil(16.0 * timeToCast / maxTimeToCast);
                    }
                    if (cooldownLevel > 0) {
                        String cooldownIcon = messages.get("gui.cooldown." + cooldownLevel, "");
                        if (!cooldownIcon.isEmpty()) {
                            glyphs += iconReverse;
                            glyphs += cooldownIcon;
                        }
                    }
                }
            }

            // Final icon padding to align to the slot frame
            glyphs += hotbarIconPaddingRight;

            // Animation if collapses
            if (collapseSpace != 0) {
                glyphs += collapseReverse;
            }

            // Add space in between each slot
            glyphs += slotSpacing;
            hotbarSlots++;
        }

        // Create the mana bar
        if (manaBarWidth > 0 && !hasExtraMessage && barMode != WandDisplayMode.NONE) {
            int manaSlots = 32;
            int hotbarWidth = hotbarSlots * (hotbarSlotWidth + slotSpacingWidth + 1);
            int manaBarPaddingLeft = (hotbarWidth - manaBarWidth) / 2;
            int manaBarReverseAmount = hotbarWidth - manaBarPaddingLeft;
            String manaReverse = messages.getSpace(-manaBarReverseAmount);
            glyphs += manaReverse;
            int manaWidth = (int)Math.floor(barMode.getProgress(wand) * manaSlots);
            glyphs += messages.get("gui.mana." + manaWidth);

            // Currently treating charges the same as mana
            if (flashTime > 0 && (now < lastInsufficientResource + flashTime || now < lastInsufficientCharges + flashTime)) {
                glyphs += messages.getSpace(-(manaBarWidth + 1));
                glyphs += messages.get("gui.mana.insufficient");
            }
        }

        if (finalSpace != 0) {
            glyphs += finalPadding;
        }
        return glyphs;
    }

    public String getExtraMessage() {
        if (extraMessage != null) {
            long now = System.currentTimeMillis();
            if (now < lastExtraMessage + extraMessageDelay) {
                // If animating, wait for animation but don't clear the message
                if (now < lastExtraMessage + extraAnimationTime) {
                    int length = (int)Math.floor(extraMessage.length() * (now - lastExtraMessage) / extraAnimationTime);
                    return extraMessage.substring(0, length);
                }
                return extraMessage;
            }
        }
        extraMessage = null;
        return "";
    }

    public boolean handleActionBar(String message) {
        if (extraMessage == null) {
            lastExtraMessage = System.currentTimeMillis();
        }
        extraMessage = message;
        return true;
    }

    public boolean handleInsufficientResources(Spell spell, CastingCost cost) {
        lastInsufficientResource = System.currentTimeMillis();
        return true;
    }

    public boolean handleCooldown(Spell spell) {
        lastCooldown = System.currentTimeMillis();
        lastCooldownSpell = spell;
        return true;
    }

    public boolean handleInsufficientCharges(Spell spell) {
        lastInsufficientCharges = System.currentTimeMillis();
        return true;
    }

    public boolean isAnimating() {
        return extraMessage != null && System.currentTimeMillis() <= lastExtraMessage + extraAnimationTime;
    }
}
