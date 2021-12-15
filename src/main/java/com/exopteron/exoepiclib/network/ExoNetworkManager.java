/* 
Copyright (c) 2021 Exopteron 

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/
package com.exopteron.exoepiclib.network;

import java.util.HashMap;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class ExoNetworkManager {
    public HashMap<Integer, Class<? extends ExoPacket>> idToPackets;
    public HashMap<Class<? extends ExoPacket>, Integer> packetToId;
    public Identifier channel;
    public ExoNetworkManager(Identifier channel) {
        this.idToPackets = new HashMap<Integer, Class<? extends ExoPacket>>();
        this.packetToId = new HashMap<Class<? extends ExoPacket>, Integer>();
        this.channel = channel;
        this.registerServerHandler();
        ClientNetworking.registerClientNetHandler(this);
    }
    public void registerPacket(int id, Class<? extends ExoPacket> packet) {
        try {
            this.packetToId.put(packet, id);
            this.idToPackets.put(id, packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // TODO warn of unregistered packets
    public Packet<ClientPlayPacketListener> assemble(ExoPacket packet) {
        PacketByteBuf buf = PacketByteBufs.create();
        Integer packetID = this.packetToId.get(packet.getClass());
        if (packetID != null) {
            buf.writeVarInt(packetID);
            packet.write(buf);
            return (Packet<ClientPlayPacketListener>) ServerPlayNetworking.createS2CPacket(this.channel, buf);
        }
        return null;
    }
    public void sendPacketToClient(ExoPacket packet, ServerPlayerEntity player) {
        PacketByteBuf buf = PacketByteBufs.create();
        Integer packetID = this.packetToId.get(packet.getClass());
        if (packetID != null) {
            buf.writeVarInt(packetID);
            packet.write(buf);
            ServerPlayNetworking.send(player, this.channel, buf);
        }
    }
    @Environment(EnvType.CLIENT)
    public void sendPacketToServer(ExoPacket packet) {
        PacketByteBuf buf = PacketByteBufs.create();
        Integer packetID = this.packetToId.get(packet.getClass());
        if (packetID != null) {
            buf.writeVarInt(packetID);
            packet.write(buf);
            ClientPlayNetworking.send(this.channel, buf);
        }
    }
    private void registerServerHandler() {
        ServerPlayNetworking.registerGlobalReceiver(this.channel, (server, player, handler, buf, responseSender) -> {
            int packetID = buf.readVarInt();
            Class<? extends ExoPacket> packet = this.idToPackets.get(packetID);
            if (packet != null) {
                try {
                    ExoPacket instance = packet.getConstructor().newInstance();
                    instance.read(buf);
                    server.execute(() -> {
                            instance.handle(player, Side.LOGICAL_SERVER);
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    // TODO handle gracefully
                }
            }
        });
    }
}
