/**
 * Copyright (c) 2023 GregTech-6 Team
 *
 * This file is part of GregTech.
 *
 * GregTech is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GregTech is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GregTech. If not, see <http://www.gnu.org/licenses/>.
 */

package gregapi.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Explosion;

import gregapi.data.LH;
import gregapi.data.OP;
import gregapi.util.ST;
import gregapi.util.UT;
import gregapi.util.WD;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import gregapi.block.Material;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.resources.Identifier;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.IPlantable;
import net.minecraft.core.Direction;

import java.util.List;
import java.util.Random;

import static gregapi.data.CS.*;

/**
 * @author Gregorius Techneticies
 */
public abstract class BlockBase extends Block implements IBlockBase {
	public final String mNameInternal;
	/** F-bounds: последние заданные bounds (1.7.10 мутировал Block.mBoundingBox); рендер-использование
	 *  отложено на F3-клиент-проход. Хранит форму {minX,minY,minZ,maxX,maxY,maxZ}. */
	protected float[] mRenderBounds = {0, 0, 0, 1, 1, 1};
	@Override public void setBlockBounds(float aMinX, float aMinY, float aMinZ, float aMaxX, float aMaxY, float aMaxZ) {
		mRenderBounds = new float[] {aMinX, aMinY, aMinZ, aMaxX, aMaxY, aMaxZ};
	}
	/** F3-render: текущие render-bounds {minX,minY,minZ,maxX,maxY,maxZ} для GT6BlockModel (было RenderBlocks.setRenderBoundsFromBlock). */
	public float[] getRenderBounds() {return mRenderBounds;}
	/** F3-render диспетчер-канал 1.7.10 (RenderBlocks.renderBlockByRenderType): vanilla Block.getRenderType()==0 —
	 *  стандартный куб; PILLAR-классы (BlockBaseBeam/Log/Bale) возвращают PILLAR_RENDER=31 → GT6BlockModel
	 *  применяет поворот UV по оси укладки (1:1 renderBlockLog). Метод существовал на подклассах и до этого —
	 *  без базового дефолта модель не могла его диспетчеризовать. */
	public int getRenderType() {return 0;}
	/** F-light: 1.7.10 Block.setLightLevel(float) мутировал эмиссию. neo эмиссия — Properties.lightLevel(ToIntFunction<BlockState>),
	 *  выставляется при ctor, но ВЫЧИСЛЯЕТСЯ лениво (initCache после регистрации) → функция читает mLightLevel через
	 *  state.getBlock() уже ПОСЛЕ setLightLevel подкласса. Мост подключён (lightOf ниже в mkProps). setLightLevel хранит поле. */
	protected float mLightLevel = 0.0F;
	public void setLightLevel(float aLightLevel) {mLightLevel = aLightLevel;}
	/** F-light мост: neo lightLevel-функция; читает mLightLevel инстанса через state.getBlock() в момент initCache (после setLightLevel). */
	private static int lightOf(net.minecraft.world.level.block.state.BlockState aState) {return aState.getBlock() instanceof BlockBase b ? (int)(15.0F * b.mLightLevel) : 0;}

	/** F9: gregapi Material (портированная 1.7.10-модель) хранится блоком — neo `WD.getMaterial(Block)` удалён. */
	protected final Material mMaterial;
	public Material getMaterial() {return mMaterial;}
	public BlockBase(Class<? extends BlockItem> aItemClass, String aNameInternal, Material aMaterial, SoundType aSoundType) {
		// F16/F9 форс движка: neo `Block` immutable (данные в Properties ДО super). setStepSound встроен в Properties.sound;
		// setBlockName удалён (имя через реестр — ST.register ниже); setCreativeTab(tabBlock) → CreativeTabsGT.assign(BLOCK) ниже
		// (last-wins: subclass-ctor переопределит). Light ПОДКЛЮЧЁН (lightLevel(lightOf) — ленивая функция читает
		// mLightLevel). Твёрдость/mapColor per-meta варьируются → динамические override'ы (getDestroyProgress/getMapColor), не Properties.
		// F12-followup (block-split): setId в Properties (иначе «Block id not set»); namespace=GAPI (совпадает с реестром BLOCKS,
		// куда ST.register клал блок), ключ санитизирован. Конструкция — на RegisterEvent через registerBlockLazy на call-site.
		super(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().sound(aSoundType).lightLevel(BlockBase::lightOf).setId(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BLOCK, net.minecraft.resources.Identifier.fromNamespaceAndPath(gregapi.data.CS.ModIDs.GT, gregapi.GT_API.sanitizeRegName(aNameInternal)))));
		mMaterial = aMaterial;
		mNameInternal = aNameInternal;
		gregapi.item.CreativeTabsGT.assign(this, gregapi.item.CreativeTabsGT.BLOCK); // F16 1:1: 1.7.10 setCreativeTab(tabBlock); last-wins → subclass переопределит
		// F12-followup (block-split): блок регистрирует registerBlockLazy на call-site; ЗДЕСЬ (RegisterEvent<Block>, ITEMS открыт)
		// регистрируем ТОЛЬКО BlockItem через supplier. Было: ST.register(this,...) (регистрировало блок эагер→freeze + BlockItem).
		final Class<? extends BlockItem> tItemClass = aItemClass==null?gregapi.block.ItemBlockBase.class:aItemClass;
		gregapi.GT_API.registerItemLazy(gregapi.data.CS.ModIDs.GT, mNameInternal, () -> (BlockItem)gregapi.util.UT.Reflection.callConstructor(tItemClass, 0, null, T, this));
		LH.add(mNameInternal+"."+W, "Any Sub-Block of this one");
	}
	
	public final String getUnlocalizedName() {return mNameInternal;}
	public String getLocalizedName() {return gregapi.lang.LanguageHandler.get(mNameInternal);}
	public String getHarvestTool(int aMeta) {return TOOL_pickaxe;}
	public int getHarvestLevel(int aMeta) {return 0;}
	public boolean canSilkHarvest() {return canSilkHarvest((byte)0);}
	public boolean canSilkHarvest(Level aWorld, Player aPlayer, int aX, int aY, int aZ, int aMeta) {return canSilkHarvest(UT.Code.bind4(aMeta));}
	public boolean isToolEffective(String aType, int aMeta) {return getHarvestTool(aMeta).equals(aType);}
	public boolean canBeReplacedByLeaves(BlockGetter aWorld, int aX, int aY, int aZ) {return F;}
	public boolean isNormalCube(BlockGetter aWorld, int aX, int aY, int aZ)  {return T;}
	public boolean renderAsNormalBlock() {return T;}
	public boolean isOpaqueCube() {return T;}
	public boolean func_149730_j() {return isOpaqueCube();}
	// F-occlusion МОСТ (репорт игрока: кувшинка/слаб рядом с блоком делает его прозрачным): 1.7.10-канал
	// isOpaqueCube() портирован per-класс (LilyPad/Bars/Spike/Sapling/Leaves/Path/Glass=F, слабы BlockMetaType=
	// mBlock==this), но ОСИРОТЕЛ — neo вырезает грани соседей по occlusion-форме состояния (canOcclude +
	// getOcclusionShape; дефолт = ПОЛНЫЙ куб → не-полные блоки глушили рендер за собой). Мост: не-opaque →
	// occlusion-форма ПУСТА (сосед рисуется) и свет проходит (1.7.10 lightOpacity = isOpaqueCube?255:0).
	// Кэш состояний строится ПОСЛЕ ctor (initCache) → override и per-класс isOpaqueCube резолвятся корректно.
	@Override protected net.minecraft.world.phys.shapes.VoxelShape getOcclusionShape(BlockState aState) {
		return isOpaqueCube() ? super.getOcclusionShape(aState) : net.minecraft.world.phys.shapes.Shapes.empty();
	}
	@Override protected boolean propagatesSkylightDown(BlockState aState) {
		return !isOpaqueCube() || super.propagatesSkylightDown(aState);
	}
	public boolean isSideSolid(BlockGetter aWorld, int aX, int aY, int aZ, Direction aDirection) {return isSideSolid(WD.meta(aWorld, aX, aY, aZ), UT.Code.side(aDirection));}
	// было shouldSideBeRendered(IBlockAccess,x,y,z,side) -> BlockBehaviour.skipRendering(BlockState,BlockState,Direction)
	// [BlockBehaviour.java:160], семантика ИНВЕРТИРОВАНА (shouldRender -> skipRendering) И новая сигнатура не
	// передаёт World/BlockPos - для isOpaqueCube()==true ветка (константный результат от THIS-блока, позиция
	// не нужна) переносится напрямую с инверсией; для else-ветки используем ванильный дефолт (position-lost).
	@Override protected boolean skipRendering(BlockState aState, BlockState aNeighbor, Direction aDir) {return isOpaqueCube() ? WD.visOpq(aNeighbor.getBlock()) : super.skipRendering(aState, aNeighbor, aDir);}
	public int damageDropped(int aMeta) {return aMeta;}
	public int quantityDropped(int aMeta, int aFortune, Random aRandom) {return 1;}
	public ItemStack createStackedBlock(int aMeta) {return ST.make(this, 1, damageDropped(aMeta));}
	public int getDamageValue(Level aWorld, int aX, int aY, int aZ) {return WD.meta(aWorld, aX, aY, aZ);}
	public int getLightOpacity() {return LIGHT_OPACITY_MAX;}
	public Item getItemDropped(int aMeta, Random aRandom, int aFortune) {return Item.byBlock(this);}
	public Item getItem(Level aWorld, int aX, int aY, int aZ) {return Item.byBlock(this);}
	public void registerBlockIcons(Object aIconRegister) {/**/}
	public boolean canSustainPlant(BlockGetter aWorld, int aX, int aY, int aZ, Direction aSide, IPlantable aPlant) {return F;}
	public boolean canCreatureSpawn(MobCategory type, BlockGetter aWorld, int aX, int aY, int aZ) {byte aMeta = WD.meta(aWorld, aX, aY, aZ); return canCreatureSpawn(aMeta) && isSideSolid(aMeta, SIDE_TOP);}
	public boolean isFireSource(Level aWorld, int aX, int aY, int aZ, Direction aSide) {return isFireSource(WD.meta(aWorld, aX, aY, aZ));}
	public boolean isFlammable(BlockGetter aWorld, int aX, int aY, int aZ, Direction aSide) {return isFlammable(WD.meta(aWorld, aX, aY, aZ));}
	public int getFlammability(BlockGetter aWorld, int aX, int aY, int aZ, Direction aSide) {return getFlammability(WD.meta(aWorld, aX, aY, aZ));}
	public int getFireSpreadSpeed(BlockGetter aWorld, int aX, int aY, int aZ, Direction aSide) {return getFireSpreadSpeed(WD.meta(aWorld, aX, aY, aZ));}
	// было getExplosionResistance(Entity,World,x,y,z,eX,eY,eZ) -> IBlockExtension.getExplosionResistance
	// (BlockState,BlockGetter,BlockPos,Explosion) [IBlockExtension.java:333]
	@Override public float getExplosionResistance(BlockState aState, BlockGetter aWorld, BlockPos aPos, Explosion aExplosion) {return getExplosionResistance(WD.meta(aWorld, aPos.getX(), aPos.getY(), aPos.getZ()));}
	public float getExplosionResistance(Entity aEntity) {return getExplosionResistance((byte)0);}
	// F12/F9-hardness: getDestroySpeed(BlockGetter,BlockPos) возвращает лишь запечённый Properties.destroyTime (не зовёт Block,
	// neo Properties immutable → runtime setHardness невозможен), НО getDestroyProgress(state,player,world,pos) — overridable
	// динамический хук. Подключаем GT6-getBlockHardness (субклассы дают vanilla/GT6-значения) по vanilla-формуле — 1:1.
	@Override protected float getDestroyProgress(net.minecraft.world.level.block.state.BlockState aState, net.minecraft.world.entity.player.Player aPlayer, net.minecraft.world.level.BlockGetter aWorld, net.minecraft.core.BlockPos aPos) {
		if (!(aWorld instanceof Level tLevel)) return super.getDestroyProgress(aState, aPlayer, aWorld, aPos);
		float tHardness = getBlockHardness(tLevel, aPos.getX(), aPos.getY(), aPos.getZ());
		if (tHardness < 0) return 0.0F; // vanilla hardness < 0 = неразрушим
		int tHarvest = net.neoforged.neoforge.event.EventHooks.doPlayerHarvestCheck(aPlayer, aState, aWorld, aPos) ? 30 : 100;
		return aPlayer.getDestroySpeed(aState, aPos) / tHardness / (float)tHarvest;
	}
	public float getBlockHardness(Level aWorld, int aX, int aY, int aZ) {return 1;}
	@Override public Block getBlock() {return this;}
	@Override public byte maxMeta() {return 1;}
	public final void onNeighborBlockChange(Level aWorld, int aX, int aY, int aZ, Block aBlock) {if (useGravity(WD.meta(aWorld, aX, aY, aZ))) aWorld.scheduleTick(new BlockPos(aX, aY, aZ), this, 2); onNeighborBlockChange2(aWorld, aX, aY, aZ, aBlock);}
	// F-neighbor (канал сместился): 1.7.10 World.notifyBlocksOfNeighborChange звал Block.onNeighborBlockChange; neo-вход —
	// BlockBehaviour.neighborChanged. Мост по образцу BlockFluidBaseGT:154; GT6-канал (гравитация + onNeighborBlockChange2) цел.
	@Override protected void neighborChanged(BlockState aState, Level aWorld, BlockPos aPos, Block aBlock, net.minecraft.world.level.redstone.Orientation aOrientation, boolean aMovedByPiston) {
		onNeighborBlockChange(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), aBlock);
	}
	// было onBlockAdded(World,x,y,z) -> BlockBehaviour.onPlace(BlockState,Level,BlockPos,BlockState,boolean) [BlockBehaviour.java:167]
	@Override protected final void onPlace(BlockState aState, Level aWorld, BlockPos aPos, BlockState aOldState, boolean aMovedByPiston) {if (useGravity(WD.meta(aWorld, aPos.getX(), aPos.getY(), aPos.getZ()))) aWorld.scheduleTick(aPos, this, 2); onBlockAdded2(aWorld, aPos.getX(), aPos.getY(), aPos.getZ());}
	public Identifier getIcon(BlockGetter aWorld, int aX, int aY, int aZ, int aSide) {return getIcon(aSide, WD.meta(aWorld, aX, aY, aZ));}
	// F3 superseded-render (GT6BlockModel/ItemModel пайплайн; старый getIcon/immediate-mode мёртв, 0 вызовов neo): было наследуемое vanilla Block.getIcon(int,int) (1.7.10, удалено в 26.1.2
	// целиком вместе со всем IIcon-атласом) — GT6 полагался на полиморфную диспетчеризацию к этому методу движка.
	// Восстановлено локально (тот же приём, что уже принят в BlockBaseMeta.getIcon), чтобы вызов выше и переопределения
	// в наследниках (BlockBaseSpike/BlockBaseBars/...) имели общую точку. Держатель ссылки — Identifier (см. IIconContainer).
	public Identifier getIcon(int aSide, int aMeta) {throw new UnsupportedOperationException("F3 dead-interface: 1.7.10 Block.getIcon(side,meta) удалён из neo (НЕ @Override, движок его не зовёт; GT6-код зовёт лишь IIconContainer.getIcon(int)). Рендер — через GT6BlockModel (IRenderedBlock.getTexture). Defensive throw.");}
	
	@Override public String name(byte aMeta) {return aMeta == W ? mNameInternal : mNameInternal + "." + aMeta;}
	@Override public boolean useGravity(byte aMeta) {return F;}
	@Override public boolean doesWalkSpeed(byte aMeta) {return F;}
	@Override public boolean doesPistonPush(byte aMeta) {return F;}
	@Override public boolean canSilkHarvest(byte aMeta) {return T;}
	@Override public boolean canCreatureSpawn(byte aMeta) {return F;}
	@Override public boolean isSealable(byte aMeta, byte aSide) {return isSideSolid(aMeta, aSide);}
	@Override public boolean isFireSource(byte aMeta) {return F;}
	@Override public boolean isFlammable(byte aMeta) {return getFlammability(aMeta) > 0;}
	@Override public void addInformation(ItemStack aStack, byte aMeta, Player aPlayer, List<String> aList, boolean aF3_H) {/**/}
	@Override public float getExplosionResistance(byte aMeta) {return 10.0F;}
	@Override public int getFlammability(byte aMeta) {return 0;}
	@Override public int getFireSpreadSpeed(byte aMeta) {return 0;}
	@Override public int getItemStackLimit(ItemStack aStack) {return UT.Code.bindStack(OP.block.mDefaultStackSize);}
	@Override public ItemStack onItemRightClick(ItemStack aStack, Level aWorld, Player aPlayer) {return aStack;}
	
	public boolean checkNoEntityCollision(Level aWorld, int aX, int aY, int aZ, byte aMeta, Entity aExceptThisOne) {return WD.noEntityCollision(aWorld, new AABB(aX, aY, aZ, aX+1, aY+1, aZ+1), aExceptThisOne);}

	// F-block-placement: 1.7.10/Forge Block.canReplace(World,x,y,z,side,stack) и Block.onBlockPlaced(...,meta)
	// удалены из neo (размещение перестроено на BlockPlaceContext/getStateForPlacement). Воспроизводим Forge-дефолты
	// как GT6-хелперы: canReplace=T (реальная проверка заменяемости — WD.replaceable в onItemUse выше); onBlockPlaced
	// возвращает мету без изменений (facing-подклассы переопределяют). ItemBlock.placeBlockAt/World.canPlaceEntityOnSide
	// — заменены прямым WD.set + checkNoEntityCollision в onItemUse (см. ниже).
	public boolean canReplace(Level aWorld, int aX, int aY, int aZ, int aSide, ItemStack aStack) {return T;}
	public byte onBlockPlaced(Level aWorld, int aX, int aY, int aZ, int aSide, float aHitX, float aHitY, float aHitZ, byte aMeta) {return aMeta;}
	public boolean isSideSolid(int aMeta, byte aSide) {return T;}
	public void updateTick2(Level aWorld, int aX, int aY, int aZ, Random aRandom) {/**/}
	public void onNeighborBlockChange2(Level aWorld, int aX, int aY, int aZ, Block aBlock) {/**/}
	public void onBlockAdded2(Level aWorld, int aX, int aY, int aZ) {/**/}
	
	// @Override
	public final void updateTick(Level aWorld, int aX, int aY, int aZ, Random aRandom) {
		if (aWorld.isClientSide() || checkGravity(aWorld, aX, aY, aZ)) return;
		updateTick2(aWorld, aX, aY, aZ, aRandom);
	}
	
	public boolean checkGravity(Level aWorld, int aX, int aY, int aZ) {
		byte aMeta = WD.meta(aWorld, aX, aY, aZ);
		if (aY > 0 && useGravity(aMeta) && FallingBlock.isFree(WD.block(aWorld, aX, aY - 1, aZ).defaultBlockState())) {
			// было BlockFalling.fallInstantly (1.7.10 static-поле, дефолт false, не найден ни в одном из 3 корней
			// референса) -> дефолтное значение "T" (=!false), тот же эффект без движкового поля.
			// было World.checkChunksExist(x0,y0,z0,x1,y1,z1) (симметричный диапазон ±32) -> ILevelReaderExtension.
			// isAreaLoaded(BlockPos,int) [ILevelReaderExtension.java:19], тот же приём, что уже принят для
			// doChunksNearChunkExist в block-behavior 2-м проходе (decisions/DEFERRED-LEDGER.md §B2).
			if (T && aWorld.isAreaLoaded(new BlockPos(aX, aY, aZ), 32)) {
				// было new FallingBlockEntity(World,x,y,z,Block,meta) + addFreshEntity (1.7.10-форма, приватный
				// конструктор в neo) -> FallingBlockEntity.fall(Level,BlockPos,BlockState) [FallingBlockEntity.java:91],
				// единственный публичный neo-путь спавна (сам заменяет исходный блок на fluid-state и вызывает addFreshEntity).
				if (!aWorld.isClientSide()) FallingBlockEntity.fall(aWorld, new BlockPos(aX, aY, aZ), aWorld.getBlockState(new BlockPos(aX, aY, aZ)));
			} else {
				WD.set(aWorld, aX, aY, aZ, NB, 0, 3);
				while (FallingBlock.isFree(WD.block(aWorld, aX, aY-1, aZ).defaultBlockState()) && aY > 0) --aY;
				if (aY > 0) WD.set(aWorld, aX, aY, aZ, this, aMeta, 2);
			}
			return T;
		}
		return F;
	}
	
	@Override public boolean onItemUseFirst(ItemBlockBase aItem, ItemStack aStack, Player aPlayer, Level aWorld, int aX, int aY, int aZ, int aSide, float aHitX, float aHitY, float aHitZ) {return F;}
	
	@Override
	public boolean onItemUse(ItemBlockBase aItem, ItemStack aStack, Player aPlayer, Level aWorld, int aX, int aY, int aZ, int aSide, float aHitX, float aHitY, float aHitZ) {
		if (aStack.getCount() == 0) return F;
		
		Block tBlock = WD.block(aWorld, aX, aY, aZ);
		if (tBlock == Blocks.SNOW && (WD.meta(aWorld, aX, aY, aZ) & 7) < 1) {
			aSide = SIDE_UP;
		} else if (tBlock != Blocks.VINE && tBlock != Blocks.DEAD_BUSH && tBlock != Blocks.DEAD_BUSH && !WD.replaceable(tBlock, aWorld, aX, aY, aZ)) {
			aX += OFFX[aSide]; aY += OFFY[aSide]; aZ += OFFZ[aSide];
		}

		if (!WD.replaceable(WD.block(aWorld, aX, aY, aZ), aWorld, aX, aY, aZ)) return F;
		if (!canReplace(aWorld, aX, aY, aZ, aSide, aStack)) return F;
		byte aMeta = UT.Code.bind4(aItem.getMetadata(ST.meta(aStack)));
		if (!checkNoEntityCollision(aWorld, aX, aY, aZ, aMeta, null)) return F;
		// canPlaceEntityOnSide восстановлен 1:1 через ЦЕНТР WD.canPlaceEntityOnSide (Forge-хук удалён по ИМЕНИ, способность
		// адаптирована централизованно — коллизия формы с исключением размещающего + заменяемость цели, WD.java).
		if (!(aPlayer).mayUseItemAt(new BlockPos(aX, aY, aZ), FORGE_DIR[aSide], aStack) || (aY == 255 && getMaterial().isSolid()) || !WD.canPlaceEntityOnSide(aWorld, this, aX, aY, aZ, F, aSide, aPlayer, aStack)) return F;

		// ItemBlock.placeBlockAt (Forge, удалён; neo BlockItem.place перестроен на BlockPlaceContext) -> прямой WD.set
		// этого блока с вычисленной onBlockPlaced-метой (тот же итог: блок поставлен, звук, расход стека).
		if (WD.set(aWorld, aX, aY, aZ, this, onBlockPlaced(aWorld, aX, aY, aZ, aSide, aHitX, aHitY, aHitZ, aMeta), 3)) {
			WD.playStepSound(aWorld, aX+0.5F, aY+0.5F, aZ+0.5F, this);
			aStack.setCount(aStack.getCount()-1);
		}
		return T;
	}
	
	public final int quantityDropped(Random aRandom) {return quantityDropped(0, 0, aRandom);}
}
