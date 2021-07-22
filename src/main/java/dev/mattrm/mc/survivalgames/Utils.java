package dev.mattrm.mc.survivalgames;

import org.bukkit.util.BlockVector;

import java.util.Arrays;

public class Utils {
    public static int getRandomNumber(int min, int max) {
        return (int) ((Math.random() * (max - min)) + min);
    }

    public static BlockVector stringToBlockVector(String str) {
        int[] coords = Arrays.stream(str.split(" ")).mapToInt(Integer::parseInt).toArray();
        return (new BlockVector(coords[0], coords[1], coords[2]));
    }

    public static String blockVectorToString(BlockVector vec) {
        return vec.getBlockX() + " " + vec.getBlockY() + " " + vec.getBlockZ();
    }

    public static com.sk89q.worldedit.BlockVector toWorldEditBlockVector(BlockVector vec) {
        return new com.sk89q.worldedit.BlockVector(vec.getBlockX(), vec.getBlockY(), vec.getBlockZ());
    }
}
