package survivalplus.modid;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.model.Dilation;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.util.Identifier;
import survivalplus.modid.entity.ModEntities;
import survivalplus.modid.entity.client.BuilderZombieRenderer;
import survivalplus.modid.entity.client.BuilderZombieModel;
import survivalplus.modid.entity.client.ModModelLayers;
import survivalplus.modid.entity.client.ReeperModel;
import survivalplus.modid.entity.client.ReeperRenderer;


public class SurvivalPlusClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ModEntities.REEPER, ReeperRenderer::new);
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.REEPER, ReeperModel::getTexturedModelData);

        EntityRendererRegistry.register(ModEntities.BUILDERZOMBIE, BuilderZombieRenderer::new);
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.BUILDERZOMBIE, BuilderZombieModel::getTexturedModelData);
    }
}
