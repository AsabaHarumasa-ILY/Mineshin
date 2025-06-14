package com.AsabaHarumasa.elementalreactions;

import com.AsabaHarumasa.elementalreactions.event.ElementalAttackHandler;
import com.AsabaHarumasa.elementalreactions.event.ElementalInteractionHandler;
import com.AsabaHarumasa.elementalreactions.event.EntityElementTracker;
import com.AsabaHarumasa.elementalreactions.event.PlayerElementTracker;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(ElementalReactionsMod.MODID)
public class ElementalReactionsMod {
    public static final String MODID = "elementalreactions";
    public static final Logger LOGGER = LogManager.getLogger(MODID);
    
    public static final ElementType[] ELEMENT_TYPES = {
        ElementType.WATER, ElementType.FIRE, ElementType.ELECTRO,
        ElementType.ICE, ElementType.WIND, ElementType.GRASS, ElementType.ROCK
    };

    public ElementalReactionsMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::clientSetup);
        
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC, "elementalreactions-common.toml");
        
        MinecraftForge.EVENT_BUS.register(new PlayerElementTracker());
        MinecraftForge.EVENT_BUS.register(new EntityElementTracker());
        MinecraftForge.EVENT_BUS.register(new ElementalInteractionHandler());
        MinecraftForge.EVENT_BUS.register(new ElementalAttackHandler());
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("==== Elemental Reactions Mod Initialization Started ====");
        ElementalParticles.registerParticleTypes();
        ElementRegistry.registerElementData();
        LOGGER.info("Registered {} element types", ELEMENT_TYPES.length);
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        LOGGER.info("Setting up client-side handlers");
        MinecraftForge.EVENT_BUS.register(new ElementHUD());
        MinecraftForge.EVENT_BUS.register(new EntityElementRenderer());
        MinecraftForge.EVENT_BUS.register(new ElementalParticles());
        LOGGER.info("Registered client-side renderers");
        
        // 初始化UI资源
        event.enqueueWork(() -> {
            ElementIcons.initialize();
            LOGGER.info("Loaded element icons");
        });
    }
}


