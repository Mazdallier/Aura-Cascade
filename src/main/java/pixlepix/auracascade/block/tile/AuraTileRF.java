package pixlepix.auracascade.block.tile;

import cofh.api.energy.IEnergyReceiver;
import cofh.api.transport.IEnderEnergyHandler;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;
import pixlepix.auracascade.AuraCascade;
import pixlepix.auracascade.data.CoordTuple;
import pixlepix.auracascade.data.EnumAura;
import pixlepix.auracascade.main.Config;
import pixlepix.auracascade.main.ParticleEffects;

import java.util.*;

/**
 * Created by localmacaccount on 2/24/15.
 */
public class AuraTileRF extends AuraTile {

    public ArrayList<CoordTuple> foundTiles = new ArrayList<CoordTuple>();

    public HashSet<CoordTuple> particleTiles = new HashSet<CoordTuple>();
    
    public int lastPower = 0;

    public boolean disabled = false;

    @Override
    public void updateEntity() {
        super.updateEntity();
        if (worldObj.getTotalWorldTime() % 100 == 0) {
            foundTiles.clear();
            LinkedList<CoordTuple> nextTiles = new LinkedList<CoordTuple>();
            nextTiles.add(new CoordTuple(this));
            while (nextTiles.size() > 0) {
                CoordTuple target = nextTiles.removeFirst();
                for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
                    CoordTuple adjacent = target.add(direction);
                    TileEntity entity = adjacent.getTile(worldObj);
                    if (entity instanceof IEnergyReceiver) {
                        if (!nextTiles.contains(adjacent) && !foundTiles.contains(adjacent)) {
                            nextTiles.add(adjacent);
                            foundTiles.add(adjacent);
                        }
                    }
                }
            }

            if (!worldObj.isRemote) {
                particleTiles.clear();
                //First, find all things near tiles
                for (CoordTuple tuple : foundTiles) {
                    for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
                        particleTiles.add(tuple.add(direction));
                    }
                }

                //Remove things that are 'inside' the bubble
                Iterator iterator = particleTiles.iterator();
                while (iterator.hasNext()) {
                    CoordTuple tuple = (CoordTuple) iterator.next();
                    if (foundTiles.contains(tuple)) {
                        iterator.remove();
                    }
                }
            }

            disabled = foundTiles.size() > 8;

            for (CoordTuple tuple : foundTiles) {
                TileEntity te = tuple.getTile(worldObj);
                if (te instanceof IEnderEnergyHandler) {
                    disabled = false;
                }
            }
        }

        if (!worldObj.isRemote && worldObj.getTotalWorldTime() % 3 == 0) {
            for (CoordTuple tuple : particleTiles) {
                Random random = new Random();
                double x = tuple.getX() + random.nextDouble();
                double y = tuple.getY() + random.nextDouble();
                double z = tuple.getZ() + random.nextDouble();
                ParticleEffects.spawnParticle("witchMagic", x, y, z, 0, 0, 0, 255, 0, !disabled ? 50 : 0);
            }

        }

        if (!disabled) {
            for (CoordTuple tuple : foundTiles) {
                TileEntity entity = tuple.getTile(worldObj);
                if (entity instanceof IEnergyReceiver) {
                    ((IEnergyReceiver) entity).receiveEnergy(ForgeDirection.UNKNOWN, (int) (lastPower * Config.powerFactor / foundTiles.size()), false);
                }

            }
        }

        //Just before aura moves
        if (worldObj.getTotalWorldTime() % 20 == 0) {
            lastPower = 0;
        }


    }

    @Override
    public void receivePower(int power, EnumAura type) {
        super.receivePower(power, type);
        lastPower += power;
    }
}