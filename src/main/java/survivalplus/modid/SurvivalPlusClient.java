package survivalplus.modid;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import survivalplus.modid.entity.ModEntities;
import survivalplus.modid.entity.client.*;


public class SurvivalPlusClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {

        EntityRendererRegistry.register(ModEntities.REEPER, ReeperRenderer::new);
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.REEPER, ReeperModel::getTexturedModelData);

        EntityRendererRegistry.register(ModEntities.BUILDERZOMBIE, BuilderZombieRenderer::new);
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.BUILDERZOMBIE, ModZombieModel::getTexturedModelData);

        EntityRendererRegistry.register(ModEntities.MINERZOMBIE, MinerZombieRenderer::new);
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.MINERZOMBIE, ModZombieModel::getTexturedModelData);

        EntityRendererRegistry.register(ModEntities.LUMBERJACKZOMBIE, LumberjackZombieRenderer::new);
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.LUMBERJACKZOMBIE, ModZombieModel::getTexturedModelData);

        EntityRendererRegistry.register(ModEntities.DIGGINGZOMBIE, DiggingZombieRenderer::new);
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.DIGGINGZOMBIE, ModZombieModel::getTexturedModelData);

        EntityRendererRegistry.register(ModEntities.SCORCHEDSKELETON, ScorchedSkeletonRenderer::new);
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.SCORCHEDSKELETON, ScorchedSkeletonModel::getTexturedModelData);

        EntityRendererRegistry.register(ModEntities.LEAPINGSPIDER, LeapingSpiderRenderer::new);
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.LEAPINGSPIDER, LeapingSpiderModel::getTexturedModelData);
    }
}
