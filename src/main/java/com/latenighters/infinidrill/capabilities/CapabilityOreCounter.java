package com.latenighters.infinidrill.capabilities;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.energy.IEnergyStorage;

public class CapabilityOreCounter {
    public static final Capability<IOreCountHandler> COUNTER = CapabilityManager.get(new CapabilityToken<>(){});;
    public static void register(RegisterCapabilitiesEvent event)
    {
        event.register(IOreCountHandler.class);
    }
}
