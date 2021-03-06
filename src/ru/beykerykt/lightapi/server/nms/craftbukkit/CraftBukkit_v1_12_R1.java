package ru.beykerykt.lightapi.server.nms.craftbukkit;

import net.minecraft.server.v1_12_R1.*;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_12_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import ru.beykerykt.lightapi.chunks.ChunkInfo;
import ru.beykerykt.lightapi.server.nms.INMSHandler;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class CraftBukkit_v1_12_R1 implements INMSHandler {

	private static Field cachedChunkModified;

	@Override
	public void createLight(World world, int x, int y, int z, int light) {
		WorldServer worldServer = ((CraftWorld) world).getHandle();
		BlockPosition position = new BlockPosition(x, y, z);
		worldServer.a(EnumSkyBlock.BLOCK, position, light);
	}

	@Override
	public void deleteLight(World world, int x, int y, int z) {
		recalculateLight(world, x, y, z);
	}

	@Override
	public void recalculateLight(World world, int x, int y, int z) {
		WorldServer worldServer = ((CraftWorld) world).getHandle();
		BlockPosition position = new BlockPosition(x, y, z);
		worldServer.c(EnumSkyBlock.BLOCK, position);
	}

	@Override
	public List<ChunkInfo> collectChunks(World world, int x, int y, int z) {
		List<ChunkInfo> list = new CopyOnWriteArrayList<ChunkInfo>();
		int chunkX = x >> 4;
		int chunkZ = z >> 4;
		try {
			WorldServer nmsWorld = ((CraftWorld) world).getHandle();
			for (int dX = -1; dX <= 1; dX++) {
				for (int dZ = -1; dZ <= 1; dZ++) {
					if (nmsWorld.getChunkProviderServer().isLoaded(chunkX + dX, chunkZ + dZ)) {
						Chunk chunk = nmsWorld.getChunkAt(chunkX + dX, chunkZ + dZ);
						Field isModified = getChunkField(chunk);
						if (isModified.getBoolean(chunk)) {
							ChunkInfo cCoord = new ChunkInfo(world, chunk.locX, y, chunk.locZ, world.getPlayers());
							list.add(cCoord);
							chunk.f(false);
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return list;
	}

	@Override
	public void sendChunkUpdate(World world, int chunkX, int chunkZ, Collection<? extends Player> players) {
		for (Player player : players) {
			sendChunkUpdate(world, chunkX, chunkZ, player);
		}
	}

	@Override
	public void sendChunkUpdate(World world, int chunkX, int chunkZ, Player player) {
		Chunk chunk = ((CraftWorld) world).getHandle().getChunkAt(chunkX, chunkZ);
		EntityPlayer human = ((CraftPlayer) player).getHandle();
		Chunk pChunk = human.world.getChunkAtWorldCoords(human.getChunkCoordinates());
		if (distanceToSquared(pChunk, chunk) < 5 * 5) {
			PacketPlayOutMapChunk packet = new PacketPlayOutMapChunk(chunk, 65534); // ?
			human.playerConnection.sendPacket(packet);
		}
	}

	@Override
	public void sendChunkUpdate(World world, int chunkX, int y, int chunkZ, Collection<? extends Player> players) {
		for (Player player : players) {
			sendChunkUpdate(world, chunkX, y, chunkZ, player);
		}
	}

	@Override
	public void sendChunkUpdate(World world, int chunkX, int y, int chunkZ, Player player) {
		Chunk chunk = ((CraftWorld) world).getHandle().getChunkAt(chunkX, chunkZ);
		EntityPlayer human = ((CraftPlayer) player).getHandle();
		Chunk pChunk = human.world.getChunkAtWorldCoords(human.getChunkCoordinates());
		if (distanceToSquared(pChunk, chunk) < 5 * 5) {
			PacketPlayOutMapChunk packet = new PacketPlayOutMapChunk(chunk, (16 * 16 * y) - 1);
			human.playerConnection.sendPacket(packet);
		}
	}

	private static Field getChunkField(Object chunk) throws NoSuchFieldException, SecurityException {
		if (cachedChunkModified == null) {
			cachedChunkModified = chunk.getClass().getDeclaredField("s");
			cachedChunkModified.setAccessible(true);
		}
		return cachedChunkModified;
	}

	private int distanceToSquared(Chunk from, Chunk to) {
		if (!from.world.getWorldData().getName().equals(to.world.getWorldData().getName()))
			return 100;
		double var2 = to.locX - from.locX;
		double var4 = to.locZ - from.locZ;
		return (int) (var2 * var2 + var4 * var4);
	}
}
