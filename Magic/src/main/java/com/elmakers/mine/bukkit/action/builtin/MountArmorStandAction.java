package com.elmakers.mine.bukkit.action.builtin;

import java.util.Arrays;
import java.util.Collection;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.elmakers.mine.bukkit.api.action.CastContext;
import com.elmakers.mine.bukkit.api.item.ItemData;
import com.elmakers.mine.bukkit.api.magic.Mage;
import com.elmakers.mine.bukkit.api.magic.MageController;
import com.elmakers.mine.bukkit.api.spell.Spell;
import com.elmakers.mine.bukkit.api.spell.SpellResult;
import com.elmakers.mine.bukkit.api.wand.Wand;
import com.elmakers.mine.bukkit.spell.BaseSpell;
import com.elmakers.mine.bukkit.utility.CompatibilityLib;
import com.elmakers.mine.bukkit.utility.ConfigurationUtils;

import de.slikey.effectlib.util.VectorUtils;

public class MountArmorStandAction extends RideEntityAction
{
    // Skip transformation updates smaller than this (radians), to save packets
    private static final double DISPLAY_UPDATE_THRESHOLD = Math.toRadians(0.5);
    // Vanilla measurements used to place the display relative to the stand's feet
    private static final double STAND_HEIGHT = 1.975;
    private static final double RIDER_HEIGHT = 1.8;
    private static final double RIDER_SEAT_DROP = 0.6;

    private boolean mountDisplay;
    private double mountDisplayScale = 1;
    private Vector mountDisplayOffset;
    private int mountDisplayInterpolation;
    private double mountDisplayYawOffset;
    private ItemDisplay.ItemDisplayTransform mountDisplayTransform = ItemDisplay.ItemDisplayTransform.HEAD;
    private ItemDisplay display;
    private float lastDisplayYaw;
    private float lastDisplayPitch;
    private float lastDisplayRoll;

    private boolean armorStandInvisible;
    private boolean armorStandInvulnerable;
    private boolean armorStandSmall;
    private boolean armorStandMarker;
    private boolean armorStandGravity;
    private boolean mountWand;
    private boolean findWand;
    private double armorStandPitch = 0;
    private double armorStandRoll = 0;
    private double armorStandHealth = 0;
    private double armorStandScale = 1;
    private ItemStack helmetItem;
    private CreatureSpawnEvent.SpawnReason armorStandSpawnReason = CreatureSpawnEvent.SpawnReason.CUSTOM;

    private ItemStack item;
    private ItemStack replacementItem;
    private int slotNumber;
    private boolean mountTarget = false;
    private String mountName;

    @Override
    public void reset(CastContext context)
    {
        Entity mount = this.mount;
        removeMountDisplay();
        super.reset(context);
        item = null;
        if (mount != null && !mountTarget) {
            mount.remove();
        }
    }

    @Override
    public void prepare(CastContext context, ConfigurationSection parameters)
    {
        super.prepare(context, parameters);
        MageController controller = context.getController();
        noTarget = parameters.getBoolean("mount_untargetable", true);
        mountTarget = parameters.getBoolean("mount_target", false);
        armorStandInvisible = parameters.getBoolean("armor_stand_invisible", true);
        armorStandInvulnerable = parameters.getBoolean("armor_stand_invulnerable", true);
        armorStandSmall = parameters.getBoolean("armor_stand_small", false);
        armorStandMarker = parameters.getBoolean("armor_stand_marker", true);
        armorStandGravity = parameters.getBoolean("armor_stand_gravity", true);
        armorStandPitch = parameters.getDouble("armor_stand_pitch", 0.0);
        armorStandRoll = parameters.getDouble("armor_stand_roll", 0.0);
        armorStandHealth = parameters.getDouble("armor_stand_health", 0.1);
        armorStandScale = parameters.getDouble("armor_stand_scale", 1.0);
        mountWand = parameters.getBoolean("mount_wand", false);
        findWand = parameters.getBoolean("find_wand", false);
        mountName = parameters.getString("mount_name", null);
        mountDisplay = parameters.getBoolean("mount_display", false);
        mountDisplayScale = parameters.getDouble("mount_display_scale", 1.0);
        mountDisplayOffset = ConfigurationUtils.getVector(parameters, "mount_display_offset", new Vector());
        if (mountDisplayOffset == null) {
            mountDisplayOffset = new Vector();
        }
        mountDisplayInterpolation = parameters.getInt("mount_display_interpolation", 2);
        mountDisplayYawOffset = parameters.getDouble("mount_display_yaw_offset", 0);
        mountDisplayTransform = ItemDisplay.ItemDisplayTransform.HEAD;
        String transformName = parameters.getString("mount_display_transform");
        if (transformName != null && !transformName.isEmpty()) {
            try {
                mountDisplayTransform = ItemDisplay.ItemDisplayTransform.valueOf(transformName.toUpperCase());
            } catch (Exception ex) {
                context.getLogger().warning("Unknown mount_display_transform: " + transformName);
            }
        }
        ItemData replacementType = controller.getOrCreateItem(parameters.getString("replacement_item"));
        if (replacementType != null) {
            replacementItem = replacementType.getItemStack(1);
        }
        if (parameters.contains("armor_stand_reason")) {
            String reasonText = parameters.getString("armor_stand_reason").toUpperCase();
            try {
                armorStandSpawnReason = CreatureSpawnEvent.SpawnReason.valueOf(reasonText);
            } catch (Exception ex) {
                context.getMage().sendMessage("Unknown spawn reason: " + reasonText);
            }
        }

        ItemData itemType = controller.getOrCreateItem(parameters.getString("helmet_item"));
        if (itemType != null) {
            helmetItem = itemType.getItemStack(1);
            if (helmetItem != null) {
                CompatibilityLib.getItemUtils().makeUnbreakable(CompatibilityLib.getItemUtils().makeReal(helmetItem));
            }
        }
    }

    @Override
    protected void remount(CastContext context) {
        if (mountTarget) {
            return;
        }

        // This seems to happen occasionally... guess we'll work around it for now.
        // TODO: Remove this, was an issue with how I was spawning armor stands and should no
        // longer be a problem.
        if (mount != null) {
            mount.remove();
        }
        // adjustHeading spawns a new one once the rider is back on the new stand
        removeMountDisplay();
        if (mountNewArmorStand(context)) {
            mount = context.getTargetEntity();
        }
    }

    @Override
    protected void adjustHeading(CastContext context) {
        super.adjustHeading(context);

        float targetPitch = targetLocation.getPitch();
        double pitch = 0;
        double roll = 0;
        if (armorStandPitch != 0 || armorStandRoll != 0) {
            pitch = armorStandPitch * targetPitch / 180 * Math.PI;
            if (armorStandRoll != 0) {
                double strafeDirection = context.getMage().getVehicleStrafeDirection();
                roll = armorStandRoll * strafeDirection;
            }
            if (!mountDisplay) {
                ArmorStand armorStand = (ArmorStand)mount;
                armorStand.setHeadPose(new EulerAngle(pitch, 0, roll));
            }
        }
        if (mountDisplay) {
            updateMountDisplay(context, targetLocation.getYaw(), pitch, roll);
        }
    }

    /**
     * Spawns the item display riding the rider if it is missing or got unmounted.
     * Riding the player lets the client move it along with the player and mount,
     * so it never lags behind. Returns true if it was just created.
     */
    private boolean ensureMountDisplay(CastContext context) {
        Entity rider = context.getEntity();
        if (rider == null) {
            return false;
        }
        if (display != null && display.isValid() && rider.equals(display.getVehicle())) {
            return false;
        }
        removeMountDisplay();
        display = spawnMountDisplay(context);
        if (display == null) {
            return false;
        }
        rider.addPassenger(display);
        return true;
    }

    /**
     * Points the display along the flight direction. All rotation goes through the display's
     * transformation, which the client interpolates, rather than through its own yaw.
     */
    private void updateMountDisplay(CastContext context, float yawDegrees, double pitch, double roll) {
        if (mount == null || !mount.isValid()) {
            return;
        }
        boolean created = ensureMountDisplay(context);
        if (display == null) {
            return;
        }

        float yaw = (float)Math.toRadians(yawDegrees + mountDisplayYawOffset);
        if (!created
                && Math.abs(yaw - lastDisplayYaw) < DISPLAY_UPDATE_THRESHOLD
                && Math.abs(pitch - lastDisplayPitch) < DISPLAY_UPDATE_THRESHOLD
                && Math.abs(roll - lastDisplayRoll) < DISPLAY_UPDATE_THRESHOLD) {
            return;
        }
        lastDisplayYaw = yaw;
        lastDisplayPitch = (float)pitch;
        lastDisplayRoll = (float)roll;

        // The display rides on top of the rider's head; shift it down to the stand's feet
        // so mount_display_offset is measured from there, like the crash hitbox
        double standHeight = armorStandMarker ? 0 : STAND_HEIGHT * (armorStandSmall ? 0.5 : 1) * armorStandScale;
        double feetToStandFeet = standHeight - RIDER_SEAT_DROP;
        double drop = RIDER_HEIGHT + feetToStandFeet;
        Vector offset = VectorUtils.rotateVector(mountDisplayOffset.clone(), yawDegrees, 0);
        Vector3f translation = new Vector3f((float)offset.getX(), (float)(offset.getY() - drop), (float)offset.getZ());
        Quaternionf rotation = new Quaternionf().rotateY(-yaw).rotateX((float)pitch).rotateZ((float)roll);
        float scale = (float)mountDisplayScale;
        Transformation transformation = new Transformation(translation, rotation, new Vector3f(scale, scale, scale), new Quaternionf());

        display.setInterpolationDelay(0);
        display.setInterpolationDuration(created ? 0 : mountDisplayInterpolation);
        display.setTransformation(transformation);
    }

    private ItemDisplay spawnMountDisplay(CastContext context) {
        ItemStack displayItem = mountWand ? item : helmetItem;
        if (displayItem == null) {
            return null;
        }
        Location location = mount.getLocation();
        location.setYaw(0);
        location.setPitch(0);
        MageController controller = context.getController();
        controller.setForceSpawn(true);
        ItemDisplay spawned;
        try {
            spawned = mount.getWorld().spawn(location, ItemDisplay.class, newDisplay -> {
                newDisplay.setItemStack(displayItem);
                newDisplay.setItemDisplayTransform(mountDisplayTransform);
                newDisplay.setBillboard(Display.Billboard.FIXED);
                newDisplay.setPersistent(false);
            });
        } catch (Exception ex) {
            context.getLogger().warning("Could not spawn broom display: " + ex.getMessage());
            return null;
        } finally {
            controller.setForceSpawn(false);
        }
        return spawned;
    }

    private void removeMountDisplay() {
        if (display != null) {
            display.remove();
            display = null;
        }
    }

    @Override
    protected SpellResult mount(CastContext context) {
        Mage mage = context.getMage();
        Player player = mage.getPlayer();
        if (player == null && mountWand)
        {
            return SpellResult.PLAYER_REQUIRED;
        }

        item = null;
        if (mountWand) {
            Wand wand = context.getWand();
            if (findWand) {
                wand = null;
                MageController controller = context.getController();
                ItemStack[] items = player.getInventory().getContents();
                for (int i = 0; i < items.length; i++) {
                    ItemStack candidateItem = items[i];
                    if (controller.isWand(candidateItem)) {
                        Wand candidateWand = controller.getWand(candidateItem);
                        String spellKey = candidateWand.getActiveSpellKey();
                        if (spellKey != null && spellKey.equals(context.getSpell().getSpellKey().getBaseKey())) {
                            wand = candidateWand;
                            wand.setHeldSlot(i);
                            break;
                        }
                    }
                }
            }
            if (wand == null) {
                return SpellResult.NO_TARGET;
            }
            wand.deactivate();

            item = wand.getItem();
            if (item == null || item.getType() == Material.AIR)
            {
                return SpellResult.FAIL;
            }
            slotNumber = wand.getHeldSlot();
        }

        if (!mountTarget && !mountNewArmorStand(context)) {
            return SpellResult.FAIL;
        }
        if (mountWand) {
            ItemStack replacement = replacementItem == null ? new ItemStack(Material.AIR) : replacementItem;
            player.getInventory().setItem(slotNumber, replacement);
        }

        SpellResult result = super.mount(context);;
        if (mount == null || !(mount instanceof ArmorStand)) {
            result = SpellResult.FAIL;
        }
        return result;
    }

    protected boolean mountNewArmorStand(CastContext context) {
        Mage mage = context.getMage();
        Entity entity = context.getEntity();
        ArmorStand armorStand = CompatibilityLib.getCompatibilityUtils().createArmorStand(mage.getLocation());

        armorStand.setHealth(armorStandHealth);
        CompatibilityLib.getCompatibilityUtils().setMaxHealth(armorStand, armorStandHealth);
        if (armorStandInvisible) {
            CompatibilityLib.getCompatibilityUtils().setInvisible(armorStand, true);
        }
        if (armorStandInvulnerable) {
            CompatibilityLib.getCompatibilityUtils().setInvulnerable(armorStand, true);
        }
        if (armorStandMarker) {
            armorStand.setMarker(true);
        }
        if (!armorStandGravity) {
            armorStand.setGravity(false);
        }
        CompatibilityLib.getCompatibilityUtils().setDisabledSlots(armorStand, 2039552);
        if (armorStandSmall) {
            armorStand.setSmall(true);
        }
        CompatibilityLib.getCompatibilityUtils().setPersist(armorStand, false);

        MageController controller = context.getController();
        controller.setForceSpawn(true);
        try {
            CompatibilityLib.getCompatibilityUtils().addToWorld(entity.getWorld(), armorStand, armorStandSpawnReason);
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
        controller.setForceSpawn(false);

        // Scales the collision box, the rider's seat height and the rendered helmet together
        if (armorStandScale != 1) {
            AttributeInstance scaleAttribute = armorStand.getAttribute(Attribute.GENERIC_SCALE);
            if (scaleAttribute != null) {
                scaleAttribute.setBaseValue(armorStandScale);
            }
        }

        EntityEquipment equipment = armorStand.getEquipment();
        if (equipment != null && !mountDisplay) {
            if (mountWand) {
                equipment.setHelmet(item);
            } else if (helmetItem != null) {
                equipment.setHelmet(helmetItem);
            }
        }
        if (mountName != null && !mountName.isEmpty()) {
            armorStand.setCustomName(mountName);
        }
        context.setTargetEntity(armorStand);

        return true;
    }

    @Override
    public void finish(CastContext context) {
        removeMountDisplay();
        if (!mountTarget && mount != null) {
            mount.remove();
        } else if (mount instanceof ArmorStand && (armorStandPitch != 0 || armorStandRoll != 0)) {
            ArmorStand armorStand = (ArmorStand)mount;
            armorStand.setHeadPose(new EulerAngle(0, 0, 0));
        }

        super.finish(context);

        Mage mage = context.getMage();
        Player player = mage.getPlayer();
        if (player == null || item == null) return;

        ItemStack currentItem = mage.getItem(slotNumber);
        if (replacementItem != null && context.getController().itemsAreEqual(currentItem, replacementItem)) {
            mage.setItem(slotNumber, item);
        } else if (currentItem != null || mage.hasStoredInventory() || player.isDead()) {
            mage.giveItem(item);
        } else {
            player.getInventory().setItem(slotNumber, item);
        }
        context.checkWand();

        item = null;
    }

    @Override
    public void getParameterNames(Spell spell, Collection<String> parameters)
    {
        super.getParameterNames(spell, parameters);
        parameters.add("armor_stand_invisible");
        parameters.add("armor_stand_invulnerable");
        parameters.add("armor_stand_small");
        parameters.add("armor_stand_marker");
        parameters.add("armor_stand_gravity");
        parameters.add("armor_stand_reason");
        parameters.add("armor_stand_pitch");
        parameters.add("armor_stand_scale");
        parameters.add("mount_wand");
        parameters.add("mount_target");
        parameters.add("replacement_item");
        parameters.add("mount_display");
        parameters.add("mount_display_scale");
        parameters.add("mount_display_offset");
        parameters.add("mount_display_interpolation");
        parameters.add("mount_display_yaw_offset");
        parameters.add("mount_display_transform");
    }

    @Override
    public void getParameterOptions(Spell spell, String parameterKey, Collection<String> examples)
    {
        if (parameterKey.equals("armor_stand_invisible")
                || parameterKey.equals("armor_stand_marker")
                || parameterKey.equals("armor_stand_small")
                || parameterKey.equals("armor_stand_gravity")
                || parameterKey.equals("armor_stand_invulnerable")
                || parameterKey.equals("armor_stand_invisible")
                || parameterKey.equals("mount_target")
                || parameterKey.equals("mount_wand")
                || parameterKey.equals("mount_display")) {
            examples.addAll(Arrays.asList(BaseSpell.EXAMPLE_BOOLEANS));
        } else if (parameterKey.equals("mount_display_transform")) {
            for (ItemDisplay.ItemDisplayTransform transform : ItemDisplay.ItemDisplayTransform.values()) {
                examples.add(transform.name().toLowerCase());
            }
        } else if (parameterKey.equals("armor_stand_pitch")) {
            examples.addAll(Arrays.asList(BaseSpell.EXAMPLE_VECTOR_COMPONENTS));
        } else if (parameterKey.equals("armor_stand_scale")) {
            examples.addAll(Arrays.asList(BaseSpell.EXAMPLE_SIZES));
        } else if (parameterKey.equals("armor_stand_reason")) {
            for (CreatureSpawnEvent.SpawnReason reason : CreatureSpawnEvent.SpawnReason.values()) {
                examples.add(reason.name().toLowerCase());
            }
        } else {
            super.getParameterOptions(spell, parameterKey, examples);
        }
    }
}
