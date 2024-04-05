package survivalplus.modid.entity.client;

import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.feature.ElytraFeatureRenderer;
import net.minecraft.client.render.entity.feature.HeadFeatureRenderer;
import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.ZombieEntityModel;
import net.minecraft.util.Identifier;
import survivalplus.modid.SurvivalPlus;
import survivalplus.modid.entity.custom.LumberjackZombieEntity;
import survivalplus.modid.entity.custom.MinerZombieEntity;

public class LumberjackZombieRenderer extends MobEntityRenderer<LumberjackZombieEntity, LumberjackZombieModel<LumberjackZombieEntity>> {
    public LumberjackZombieRenderer(EntityRendererFactory.Context context) {
        super(context, new LumberjackZombieModel<>(context.getPart(ModModelLayers.LUMBERJACKZOMBIE)), 0.5f);
        this.addFeature(new ArmorFeatureRenderer<>(this, new ZombieEntityModel<>(context.getPart(EntityModelLayers.ZOMBIE_INNER_ARMOR)), new ZombieEntityModel<>(context.getPart(EntityModelLayers.ZOMBIE_OUTER_ARMOR)), context.getModelManager()));
        this.addFeature(new HeadFeatureRenderer<>(this, context.getModelLoader(), 1.0f, 1.0f, 1.0f, context.getHeldItemRenderer()));
        this.addFeature(new ElytraFeatureRenderer<>(this, context.getModelLoader()));
        this.addFeature(new HeldItemFeatureRenderer<>(this, context.getHeldItemRenderer()));
    }

    @Override
    public Identifier getTexture(LumberjackZombieEntity entity) {
        return new Identifier(SurvivalPlus.MOD_ID,"textures/entity/lumberjackzombie.png");
    }


}
