package dev.itsmeow.betteranimalsplus.common.tileentity;

import java.util.Random;

import dev.itsmeow.betteranimalsplus.init.ModBlocks;
import dev.itsmeow.betteranimalsplus.init.ModResources;
import dev.itsmeow.betteranimalsplus.init.ModTileEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalBlock;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class TileEntityTrillium extends TileEntity {

    private int typeNum;
    private final String keyType = "trilliumType";

    private int modelNum;
    private final String keyModel = "trilliumModel";

    public TileEntityTrillium() {
        super(ModTileEntities.TRILLIUM_TYPE.get());
        if (!this.getTileData().contains(this.keyType)) {
            this.setType(new Random().nextInt(5));
        }
        if (!this.getTileData().contains(this.keyModel)) {
            this.modelNum = new Random().nextInt(3);
            this.markDirty();
        }
    }

    public ResourceLocation getTexture() {
        return this.typeNum == 0 ? ModResources.trillium_yellow : ModResources.trillium_purple;
    }

    public void setType(int i) {
        this.typeNum = i;
        this.markDirty();
    }

    public int typeValue() {
        return this.typeNum;
    }

    @Override
    public void read(BlockState state, CompoundNBT compound) {
        super.read(state, compound);
        if (compound.contains(this.keyType)) {
            this.typeNum = compound.getInt(this.keyType);
        } else {
            this.setType(new Random().nextInt(5)); // 1/5 chance
        }
        if (compound.contains(this.keyModel)) {
            this.modelNum = compound.getInt(this.keyModel);
        } else {
            this.setModelNum(new Random().nextInt(3));
        }
    }

    @Override
    public CompoundNBT write(CompoundNBT compound) {
        super.write(compound);
        compound.putInt(this.keyType, this.typeNum);
        compound.putInt(this.keyModel, this.modelNum);
        return compound;
    }

    @Override
    public SUpdateTileEntityPacket getUpdatePacket() {
        CompoundNBT tag = new CompoundNBT();
        this.write(tag);
        return new SUpdateTileEntityPacket(this.pos, 1, tag);
    }

    @Override
    public void onDataPacket(NetworkManager net, SUpdateTileEntityPacket packet) {
        this.read(null, packet.getNbtCompound());
        this.world.getPendingBlockTicks().scheduleTick(this.pos, this.getBlockState().getBlock(), 100);
    }

    @Override
    public CompoundNBT getUpdateTag() {
        CompoundNBT tag = new CompoundNBT();
        this.write(tag);
        return tag;
    }

    public void setModelNum(int i) {
        this.modelNum = i;
        this.markDirty();
    }

    @Override
    public void handleUpdateTag(BlockState state, CompoundNBT tag) {
        this.read(state, tag);
    }

    @OnlyIn(Dist.CLIENT)
    public float getRotation() {
        BlockState state = this.world.getBlockState(this.pos);
        if (state.getBlock() == ModBlocks.TRILLIUM.get()) {
            Direction facing = state.get(HorizontalBlock.HORIZONTAL_FACING).getOpposite();
            if (facing == Direction.NORTH) {
                return 0F;
            }
            if (facing == Direction.EAST) {
                return 90F;
            }
            if (facing == Direction.SOUTH) {
                return 180F;
            }
            if (facing == Direction.WEST) {
                return 270F;
            }
        }
        return 0F;
    }

    public int getModelNum() {
        return this.modelNum;
    }

}
