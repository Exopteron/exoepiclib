package com.exopteron.exoepiclib.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class ClientNetworking {
    protected static void registerClientNetHandler(ExoNetworkManager netManager) {
        ClientPlayNetworking.registerGlobalReceiver(netManager.channel, (client, handler, buf, responseSender) -> {
            int packetID = buf.readVarInt();
            Class<? extends ExoPacket> packet = netManager.idToPackets.get(packetID);
            if (packet != null) {
                try {
                    ExoPacket instance = packet.getConstructor().newInstance();
                    instance.read(buf);
                    client.execute(() -> {
                            instance.handle(client.player, Side.LOGICAL_CLIENT);
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    // TODO handle gracefully
                }
            }
        });
    }
}
