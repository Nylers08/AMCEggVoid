package ariolmc.online.aMCEggVoid.core.nether;

import lombok.Getter;

@Getter
public class BlazeZone {

    private final int minX;
    private final int maxX;
    private final int minZ;
    private final int maxZ;
    private final int centerX;
    private final int centerZ;
    private final int blocksOnEdge;

    public BlazeZone(int centerX, int centerZ, int blocksOnEdge) {
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.blocksOnEdge = blocksOnEdge;

        // Количество блоков с каждой стороны от центра
        int halfBlocks = (blocksOnEdge - 1) / 2;
        int halfSpan = halfBlocks * 2;

        this.minX = centerX - halfSpan;
        this.maxX = centerX + halfSpan;
        this.minZ = centerZ - halfSpan;
        this.maxZ = centerZ + halfSpan;
    }

    public boolean contains(int x, int z) {
        return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
    }

    public boolean isGridBlock(int x, int z) {
        return contains(x, z) && Math.floorMod(x - centerX, 2) == 0 && Math.floorMod(z - centerZ, 2) == 0;
    }

    public boolean overlaps(BlazeZone other, int padding) {
        return this.minX - padding <= other.maxX && this.maxX + padding >= other.minX
                && this.minZ - padding <= other.maxZ && this.maxZ + padding >= other.minZ;
    }
}
