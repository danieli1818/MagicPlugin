package com.elmakers.mine.bukkit.utility.platform.v1_11.entity;

import java.util.logging.Logger;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Ocelot;

import com.elmakers.mine.bukkit.api.magic.MageController;
import com.elmakers.mine.bukkit.utility.platform.base.entity.EntityAnimalData;

public class EntityOcelotData extends EntityAnimalData {

    private Ocelot.Type type;

    public EntityOcelotData(ConfigurationSection parameters, MageController controller) {
        super(parameters, controller);

        Logger log = controller.getLogger();
        String typeName = parameters.getString("cat_type");
        if (typeName != null && !typeName.isEmpty()) {
            try {
                type = Ocelot.Type.valueOf(typeName.toUpperCase());
            } catch (Exception ex) {
                log.warning("Invalid cat type: " + typeName);
            }
        }
    }

    public EntityOcelotData(Entity entity) {
        super(entity);
        if (entity instanceof Ocelot) {
            Ocelot cat = (Ocelot) entity;
            type = cat.getCatType();
        }
    }

    @Override
    public void apply(Entity entity) {
        super.apply(entity);
        if (entity instanceof Ocelot) {
            Ocelot cat = (Ocelot) entity;
            if (type != null) {
                cat.setCatType(type);
            }
        }
    }

    @Override
    public boolean cycle(Entity entity) {
        if (!canCycle(entity)) {
            return false;
        }

        Ocelot cat = (Ocelot) entity;
        Ocelot.Type catType = cat.getCatType();
        Ocelot.Type[] typeValues = Ocelot.Type.values();
        catType = typeValues[(catType.ordinal() + 1) % typeValues.length];
        cat.setCatType(catType);
        return true;
    }


    @Override
    public boolean canCycle(Entity entity) {
        return entity instanceof Ocelot;
    }

}
