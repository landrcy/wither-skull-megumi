package com.landrcy.witherscullmegumi.network;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ClientPayloadBridge {
    private ClientPayloadBridge() {}

    public static void handleRitualSync(RitualSyncPayload payload, IPayloadContext context) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            context.enqueueWork(() -> ClientOnlyInvoker.invoke(payload));
        }
    }

    private static final class ClientOnlyInvoker {
        private static void invoke(RitualSyncPayload payload) {
            com.landrcy.witherscullmegumi.client.ClientRitualManager.handleSyncPayload(payload);
        }
    }
}
