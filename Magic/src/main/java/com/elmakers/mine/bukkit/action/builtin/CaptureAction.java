package com.elmakers.mine.bukkit.action.builtin;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.elmakers.mine.bukkit.action.BaseSpellAction;
import com.elmakers.mine.bukkit.api.action.CastContext;
import com.elmakers.mine.bukkit.api.spell.SpellResult;
import com.elmakers.mine.bukkit.utility.platform.CompatibilityUtils;
import com.elmakers.mine.bukkit.utility.platform.ItemUtils;
import com.elmakers.mine.bukkit.utility.platform.NBTUtils;

public class CaptureAction extends BaseSpellAction {
    private double minHealth;

    @Override
    public void prepare(CastContext context, ConfigurationSection parameters)
    {
        super.prepare(context, parameters);
        minHealth = parameters.getDouble("min_health", -1);
    }

    @Override
    public SpellResult perform(CastContext context) {

        Entity targetEntity = context.getTargetEntity();
        if (targetEntity instanceof Player) {
            return SpellResult.NO_TARGET;
        }
        if (minHealth >= 0) {
            if (!(targetEntity instanceof LivingEntity)) {
                return SpellResult.NO_TARGET;
            }
            LivingEntity li = (LivingEntity)targetEntity;
            if (li.getHealth() > minHealth) {
                return SpellResult.NO_TARGET;
            }
        }
        Material eggMaterial = context.getController().getMobEgg(targetEntity.getType());
        if (eggMaterial == null) {
            return SpellResult.NO_TARGET;
        }
        String entityTypeString = CompatibilityUtils.getEntityType(targetEntity);
        if (entityTypeString == null) {
            return SpellResult.FAIL;
        }
        Object savedEntity = CompatibilityUtils.getEntityData(targetEntity);
        if (savedEntity == null) {
            return SpellResult.FAIL;
        }
        if (targetEntity instanceof LivingEntity) {
            LivingEntity li = (LivingEntity)targetEntity;
            li.setHealth(CompatibilityUtils.getMaxHealth(li));
        }
        targetEntity.remove();
        ItemStack spawnEgg = new ItemStack(eggMaterial);

        String entityName = targetEntity.getCustomName();
        if (entityName != null && !entityName.isEmpty()) {
            ItemMeta meta = spawnEgg.getItemMeta();
            String title = context.getController().getMessages().get("general.spawn_egg_title");
            title = title.replace("$entity", entityName);
            meta.setDisplayName(title);
            spawnEgg.setItemMeta(meta);
        }
        spawnEgg = ItemUtils.makeReal(spawnEgg);

        // Add entity type attribute
        NBTUtils.setMeta(savedEntity, "id", entityTypeString);

        // Remove instance-specific attributes
        NBTUtils.removeMeta(savedEntity, "Pos");
        NBTUtils.removeMeta(savedEntity, "Motion");
        NBTUtils.removeMeta(savedEntity, "Rotation");
        NBTUtils.removeMeta(savedEntity, "FallDistance");
        NBTUtils.removeMeta(savedEntity, "Dimension");
        NBTUtils.removeMeta(savedEntity, "UUID");
        NBTUtils.removeMeta(savedEntity, "PortalCooldown");

        if (!NBTUtils.setMetaNode(spawnEgg, "EntityTag", savedEntity)) {
            return SpellResult.FAIL;
        }
        targetEntity.getWorld().dropItemNaturally(targetEntity.getLocation(), spawnEgg);

        return SpellResult.CAST;
    }

    @Override
    public boolean isUndoable() {
        return false;
    }

    @Override
    public boolean requiresTargetEntity() {
        return true;
    }

    @Override
    public boolean requiresTarget() {
        return true;
    }
}
