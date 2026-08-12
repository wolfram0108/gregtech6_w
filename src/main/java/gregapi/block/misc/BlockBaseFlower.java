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
 *
 * Modified in 2026 for the GregTech 6 NeoForge port
 * (https://github.com/wolfram0108/gregtech6_w): ported from Minecraft 1.7.10 / Forge
 * to Minecraft 26.1.2 / NeoForge.
 */

package gregapi.block.misc;

import net.minecraft.core.BlockPos;

import gregapi.api.Optional;
import gregapi.block.IBlockBase;
import gregapi.block.ItemBlockBase;
import gregapi.block.Material;
import gregapi.compat.galacticraft.IBlockSealable;
import gregapi.data.MD;
import gregapi.render.IIconContainer;
import gregapi.util.ST;
import gregapi.util.UT;
import gregapi.util.WD;
import micdoodle8.mods.galacticraft.api.block.IOxygenReliantBlock;
import mods.railcraft.common.carts.EntityTunnelBore;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.EnumPlantType;
import net.minecraft.core.Direction;

import java.util.List;
import java.util.Random;

import static gregapi.data.CS.*;

/**
 * @author Gregorius Techneticies
 */
@Optional.InterfaceList(value = {
	@Optional.Interface(iface = "micdoodle8.mods.galacticraft.api.block.IOxygenReliantBlock", modid = ModIDs.GC)
})
public abstract class BlockBaseFlower extends FlowerBlock implements IBlockBase, IBlockSealable, IOxygenReliantBlock, BonemealableBlock, gregapi.render.IRenderedCross, gregapi.block.IBlockExtendedMetaData {
	public final String mNameInternal;
	public IIconContainer[] mIcons;
	/** For Creative Subsets, not actually important. */
	private final byte mMaxMeta;

	/** F3-render/meta: вариант цветка (per-ore индикатор бедрок-руд) — в blockstate-property (синкается с чанком, без TE):
	 *  WD.set→setExtendedMetaData ставит, WD.meta→getExtendedMetaData читает, GT6BlockModel рисует cross по нему (IRenderedCross). */
	public static final net.minecraft.world.level.block.state.properties.IntegerProperty META = net.minecraft.world.level.block.state.properties.IntegerProperty.create("meta", 0, 15);
	@Override protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block, BlockState> aBuilder) {super.createBlockStateDefinition(aBuilder); aBuilder.add(META);}
	/** F9: было super(Material.plants) — BlockFlower(1.7.10, recompSrc Block.java:26) — переходник не
	 *  распространён на классы вне BlockBase (F9 4-bis, тот же приём переиспользован: собственное mMaterial/
	 *  getMaterial(), не новая абстракция). */
	protected final Material mMaterial = Material.plants;
	public Material getMaterial() {return mMaterial;}

	/** @param aSpeed is usually 0.4F */
	public BlockBaseFlower(Class<? extends ItemBlockBase> aItemClass, String aNameInternal, long aMaxMeta, IIconContainer[] aIcons) {
		// F16/F9 форс движка: 1.7.10 BlockFlower(int) отбирал группу суб-типов (не эффект) - концепт исчез; neo
		// FlowerBlock(SuspiciousStewEffects,Properties) [FlowerBlock.java:36] требует эффект похлёбки - GT6-цветы
		// декоративные (без спец-эффекта) -> SuspiciousStewEffects.EMPTY [SuspiciousStewEffects.java:25], тот же
		// Properties.of()-дефолт, что и остальные BlockBase-наследники (F9-мост твёрдости отложен туда же).
		// F12-followup (block-split): setId в Properties (иначе «Block id not set»); namespace=GAPI (совпадает с реестром/call-site).
		// F16: golden setStepSound(soundTypeGrass) — runtime-мутатор в neo невозможен, задаём в Properties.sound(GRASS) при ctor (1:1).
		// F16 noCollision (репорт игрока: трава под цветком → земля, цветок затемнён): 1.7.10 BlockFlower — без
		// коллизии и isOpaqueCube=false; neo-эквивалент — Properties.noCollision() (hasCollision=false И canOcclude=false,
		// BlockBehaviour:1078-1082, так собран vanilla DANDELION). Без него canOcclude=true → occlusion-форма непуста →
		// faceShapeOccludes с полной верхней гранью травы = «свет заблокирован» → SpreadingSnowyDirtBlock убивал траву.
		// MODCOMPAT-002: цвет на карте. 1.7.10 BlockFlower наследовал Material.plants (BlockFlower.java:26), а тот
		// несёт foliageColor — то есть цветы на карте были цвета листвы. В neo дефолт «нет цвета», задаём явно тем
		// же мостом и из того же материала, что остальные иерархии (см. BlockBase.mapColorOf).
		super(net.minecraft.world.item.component.SuspiciousStewEffects.EMPTY, gregapi.block.BlockBase.mapColorOf(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().noCollision().sound(net.minecraft.world.level.block.SoundType.GRASS), gregapi.block.Material.plants));
		registerDefaultState(getStateDefinition().any().setValue(META, 0)); // F3-render/meta: дефолт META=0
		mMaxMeta = (byte)(UT.Code.bind4(aMaxMeta-1)+1);
		mIcons = aIcons;
		mNameInternal = aNameInternal;
		gregapi.item.CreativeTabsGT.assign(this, gregapi.item.CreativeTabsGT.DECORATIONS);
		// F12-followup (block-split): блок регистрирует registerBlockLazy на call-site (Loader_Blocks); ЗДЕСЬ — только BlockItem.
		final Class<? extends net.minecraft.world.item.BlockItem> tItemClass = aItemClass==null?gregapi.block.ItemBlockBase.class:aItemClass;
		gregapi.GT_API.registerItemLazy(gregapi.data.CS.ModIDs.GT, mNameInternal, () -> (net.minecraft.world.item.BlockItem)gregapi.util.UT.Reflection.callConstructor(tItemClass, 0, null, gregapi.data.CS.T, this));
		if (MD.RC.mLoaded) try {EntityTunnelBore.addMineableBlock(this);} catch(Throwable e) {e.printStackTrace(ERR);}
		if (COMPAT_FR != null) gregapi.GT_API.deferItemInit(() -> COMPAT_FR.addToBackpacks("forester", ST.make(this, 1, W)));
	}
	
	public final String getUnlocalizedName() {return mNameInternal;}
	@Override public void setBlockBounds(float aMinX, float aMinY, float aMinZ, float aMaxX, float aMaxY, float aMaxZ) {/* IBlock-хук; F-shape отложена core-wide, FlowerBlock несёт свой neo SHAPE */}
	@Override public float[] getRenderBounds() {return null;/* цветы — cross-рендер (IRenderedCross), bounds не хранят */}
	// neo BonemealableBlock: GT6-цветы декоративны — костная мука неприменима (как ванильные одиночные цветы).
	@Override public boolean isValidBonemealTarget(net.minecraft.world.level.LevelReader aWorld, BlockPos aPos, BlockState aState) {return F;}
	@Override public boolean isBonemealSuccess(net.minecraft.world.level.Level aWorld, net.minecraft.util.RandomSource aRandom, BlockPos aPos, BlockState aState) {return F;}
	@Override public void performBonemeal(net.minecraft.server.level.ServerLevel aWorld, net.minecraft.util.RandomSource aRandom, BlockPos aPos, BlockState aState) {/**/}
	@Override public String name(byte aMeta) {return mNameInternal + "." + aMeta;}
	public String getLocalizedName() {return gregapi.lang.LanguageHandler.get(mNameInternal);}
	public float getBlockHardness(Level aWorld, int aX, int aY, int aZ) {return 0;}
	// было getExplosionResistance(Entity,World,x,y,z,eX,eY,eZ) -> IBlockExtension.getExplosionResistance
	// (BlockState,BlockGetter,BlockPos,Explosion) [IBlockExtension.java:333]; исходное тело игнорировало все параметры (константа 0).
	@Override public float getExplosionResistance(net.minecraft.world.level.block.state.BlockState aState, BlockGetter aWorld, BlockPos aPos, net.minecraft.world.level.Explosion aExplosion) {return 0;}
	public float getExplosionResistance(Entity aEntity) {return 0;}
	public String getHarvestTool(int aMeta) {return TOOL_sword;}
	public int getHarvestLevel(int aMeta) {return 0;}
	public boolean canSilkHarvest() {return canSilkHarvest((byte)0);}
	public boolean canSilkHarvest(Level aWorld, Player aPlayer, int aX, int aY, int aZ, int aMeta) {return canSilkHarvest(UT.Code.bind4(aMeta));}
	public boolean isToolEffective(String aType, int aMeta) {return T;}
	public boolean canBeReplacedByLeaves(BlockGetter aWorld, int aX, int aY, int aZ) {return T;}
	public boolean isNormalCube(BlockGetter aWorld, int aX, int aY, int aZ)  {return F;}
	public boolean isSideSolid(BlockGetter aWorld, int aX, int aY, int aZ, Direction aDirection) {return F;}
	public int damageDropped(int aMeta) {return aMeta;}
	public int quantityDropped(Random par1Random) {return 1;}
	public int getDamageValue(Level aWorld, int aX, int aY, int aZ) {return WD.meta(aWorld, aX, aY, aZ);}
	public int getLightOpacity() {return LIGHT_OPACITY_NONE;}

	// F3 light-opacity МОСТ (цветы наследуют ванильный FlowerBlock, а не BlockBase — свой мост, см. разбор там).
	@Override public int getLightBlock(net.minecraft.world.level.block.state.BlockState aState, net.minecraft.world.level.BlockGetter aWorld, net.minecraft.core.BlockPos aPos) {return gregapi.data.CS.lightDampening(getLightOpacity());}

	// F3 shade МОСТ (цветы наследуют ванильный FlowerBlock, а не BlockBase — свой мост, см. разбор там).
	@Override public float getShadeBrightness(net.minecraft.world.level.block.state.BlockState aState, BlockGetter aWorld, net.minecraft.core.BlockPos aPos) {return gregapi.data.CS.shadeBrightness(isBlockNormalCube());}

	/** 1.7.10 {@code Block.isBlockNormalCube()} ({@code Block.java:502-504}) — тело 1:1, см. {@code BlockBase}. */
	public boolean isBlockNormalCube() {return mMaterial.blocksMovement() && renderAsNormalBlock();}

	/** 1.7.10-значение приходило от ванильного предка {@code BlockBush.renderAsNormalBlock()} = false
	 *  ({@code BlockBush.java:108-111}); в neo этого метода у предка нет — переносим явно, 1:1. */
	public boolean renderAsNormalBlock() {return F;}
	public Item getItemDropped(int par1, Random aRandom, int par3) {return Item.byBlock(this);}
	public Item getItem(Level aWorld, int aX, int aY, int aZ) {return Item.byBlock(this);}
	public void registerBlockIcons(Object aIconRegister) {/**/}
	public boolean canCreatureSpawn(MobCategory type, BlockGetter aWorld, int aX, int aY, int aZ) {return F;}
	@SuppressWarnings("unchecked") public void getSubBlocks(Item aItem, CreativeModeTab aTab, @SuppressWarnings("rawtypes") List aList) {for (int i = 0; i < maxMeta(); i++) aList.add(ST.make(aItem, 1, i));}
	public boolean isSealed(Level aWorld, int aX, int aY, int aZ, Direction aDirection) {return F;}
	@Override public Block getBlock() {return this;}
	@Override public byte maxMeta() {return mMaxMeta;}
	public ResourceLocation getIcon(int aSide, int aMeta) {return mIcons[aMeta % mIcons.length].getIcon(0);}

	// F3-render/meta (IBlockExtendedMetaData): вариант цветка в blockstate-property META; get/setExtendedMetaData —
	// дефолты интерфейса (консолидация захода #39: зеркало удалено; прежний локальный сеттер гейтился на Level —
	// дефолт шире и вернее 1:1: пишет и LevelAccessor-регион, и ChunkAccess ворлдгена, как остальная семья).
	// F3-render (IRenderedCross): текстура cross-модели per-мета (getIcon уже per-мета из mIcons); GT6BlockModel рисует X-форму.
	// aWorld==null = item-рендер, aX несёт МЕТУ СТЕКА (контракт IRenderedCross; прежде item всегда рисовал мету 0).
	@Override public ResourceLocation getCrossIcon(BlockGetter aWorld, int aX, int aY, int aZ) {
		if (mIcons == null || mIcons.length == 0) return null;
		IIconContainer tIcon = mIcons[UT.Code.bind4(aWorld == null ? aX : WD.meta(aWorld, aX, aY, aZ)) % mIcons.length];
		return tIcon == null ? null : tIcon.getIcon(0);
	}
	public void onOxygenAdded(Level aWorld, int aX, int aY, int aZ) {/**/}
	public void onOxygenRemoved(Level aWorld, int aX, int aY, int aZ) {if (!aWorld.isClientSide() && !WD.oxygen(aWorld, aX, aY, aZ)) {WD.set(aWorld, aX, aY, aZ, NB, 0, 3); return;}}
	
	@Override public void addInformation(ItemStack aStack, byte aMeta, Player aPlayer, List<String> aList, boolean aF3_H) {/**/}
	@Override public float getExplosionResistance(byte aMeta) {return 0;}
	@Override public boolean useGravity(byte aMeta) {return F;}
	@Override public boolean doesWalkSpeed(byte aMeta) {return F;}
	@Override public boolean doesPistonPush(byte aMeta) {return F;}
	@Override public boolean canSilkHarvest(byte aMeta) {return T;}
	@Override public boolean canCreatureSpawn(byte aMeta) {return F;}
	@Override public boolean isSealable(byte aMeta, byte aSide) {return F;}
	@Override public boolean isFlammable(byte aMeta) {return getFlammability(aMeta) > 0;}
	@Override public boolean isFireSource(byte aMeta) {return F;}
	@Override public int getFlammability(byte aMeta) {return 0;}
	@Override public int getFireSpreadSpeed(byte aMeta) {return 0;}
	@Override public int getItemStackLimit(ItemStack aStack) {return 64;}
	@Override public ItemStack onItemRightClick(ItemStack aStack, Level aWorld, Player aPlayer) {return aStack;}
	
	public EnumPlantType getPlantType(BlockGetter aWorld, int aX, int aY, int aZ) {return EnumPlantType.Plains;}
	public Block getPlant(BlockGetter aWorld, int aX, int aY, int aZ) {return this;}
	public int getPlantMetadata(BlockGetter aWorld, int aX, int aY, int aZ) {return WD.meta(aWorld, aX, aY, aZ);}
	// 1:1 оригинала (:131): кислород + canSustainPlant почвы через ЦЕНТР WD.canSustainPlant — он несёт таблицу
	// почв 1.7.10 для TriState.DEFAULT. ⛔ Прежняя копия здесь сворачивала toBoolean(T): цветок «стоял» на камне
	// и в воздухе — снос не работал вовсе (замер gt6flowerprobe, 2026-07-30).
	public boolean canBlockStay(Level aWorld, int aX, int aY, int aZ) {return WD.oxygen(aWorld, aX, aY, aZ) && WD.canSustainPlant(aWorld, aX, aY - 1, aZ, Direction.UP, Blocks.DANDELION);}
	public boolean func_149851_a(Level aWorld, int aX, int aY, int aZ, boolean aIsRemote) {return T;}
	public boolean func_149852_a(Level aWorld, Random aRandom, int aX, int aY, int aZ) {return T;}
	public void func_149853_b(Level aWorld, Random aRandom, int aX, int aY, int aZ) {ST.drop(aWorld, aX+0.5, aY+0.5, aZ+0.5, this, 1, WD.meta(aWorld, aX, aY, aZ));}
	// было Block.onBlockPlaced(World,x,y,z,side,hitX,hitY,hitZ,meta) (1.7.10 vanilla override-точка, дефолт identity
	// return meta [recompSrc Block.java:1067-1069]) - удалено из neo целиком; GT6-own reintroduced generic-hook (тот
	// же приём, что BlockBaseSpike/BlockBaseLog/BlockBaseBeam уже переопределяют), дефолт-идентичность как в оригинале.
	public int onBlockPlaced(Level aWorld, int aX, int aY, int aZ, int aSide, float aHitX, float aHitY, float aHitZ, int aMeta) {return aMeta;}
	
	// BUG-006-приём для иерархии ВНЕ BlockBase (цветок стоит на ванильном FlowerBlock, мостов BlockBase:312 /
	// PrefixBlock / BlockBaseRail у него нет): loot-таблиц у GT6 нет, neo-дефолт getDrops(loot) отдавал ПУСТО —
	// снос тиком/опорой и добыча не роняли НИЧЕГО (приёмка 2026-07-30: «цветок исчезает без лута»; судья
	// gt6flowerprobe показывал «дроп-сущностей 0», но судил только снос — слепота исправлена). Формула дропа —
	// дефолт 1.7.10 Block.getDrops: quantityDropped копий ST(getItemDropped, 1, damageDropped) из СОБСТВЕННЫХ
	// методов ниже (:132-149, 1:1 оригинала :97-101). Мета — из СНИМКА aState (BUG-016/026).
	@Override public java.util.List<ItemStack> getDrops(net.minecraft.world.level.block.state.BlockState aState, net.minecraft.world.level.storage.loot.LootParams.Builder aParams) {
		net.minecraft.world.phys.Vec3 tOrigin = aParams.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN);
		if (tOrigin == null) return super.getDrops(aState, aParams);
		if (WD.explosionDropDenied(aParams)) return java.util.Collections.emptyList();
		java.util.ArrayList<ItemStack> rDrops = ST.arraylist();
		int tMeta = WD.meta(aState);
		Item tItem = getItemDropped(tMeta, RNGSUS, 0);
		if (tItem != null) for (int i = 0, j = quantityDropped(RNGSUS); i < j; i++) rDrops.add(ST.make(tItem, 1, damageDropped(tMeta)));
		return rDrops;
	}

	// КАНАЛ ПОДКЛЮЧЁН (2026-07-30, реестр мёртвых каналов): 1.7.10 checkAndDropBlock звался из
	// updateTick/onNeighborBlockChange ванильного BlockBush (recompSrc :53-56, :62-64; setTickRandomly(true) :22)
	// и сносил цветок, когда canBlockStay:193 говорило «нельзя» (кислород WD.oxygen + почва). Neo-эквиваленты
	// ниже — оба канала ванильной базы, к которым цветок и в 1.7.10 был прикреплён:
	//  (1) canSurvive → canBlockStay: его читает унаследованный VegetationBlock.updateShape (:28-40 референса)
	//      — снос при обновлении соседа, роль onNeighborBlockChange; и он же гейт постановки. Правило ЗАМЕНЯЕТ
	//      ванильное целиком, как @Override canBlockStay в 1.7.10 (:131 оригинала). LevelReader без Level
	//      (регион генерации) → ванильное правило почвы, приём как isFireSource у PrefixBlock;
	//  (2) isRandomlyTicking + randomTick → checkAndDropBlock: random-плечо 1:1 (BlockBush:22,:62-64) —
	//      кислород меняется и без обновления соседей.
	@Override public boolean canSurvive(net.minecraft.world.level.block.state.BlockState aState, net.minecraft.world.level.LevelReader aWorld, BlockPos aPos) {
		return aWorld instanceof Level tLevel ? canBlockStay(tLevel, aPos.getX(), aPos.getY(), aPos.getZ()) : super.canSurvive(aState, aWorld, aPos);
	}
	@Override public boolean isRandomlyTicking(net.minecraft.world.level.block.state.BlockState aState) {return T;}
	@Override public void randomTick(net.minecraft.world.level.block.state.BlockState aState, net.minecraft.server.level.ServerLevel aWorld, BlockPos aPos, net.minecraft.util.RandomSource aRandom) {
		checkAndDropBlock(aWorld, aPos.getX(), aPos.getY(), aPos.getZ());
	}
	public void checkAndDropBlock(Level aWorld, int aX, int aY, int aZ) {
		if (canBlockStay(aWorld, aX, aY, aZ)) return;
		WD.dropBlockAsItem(aWorld, aX, aY, aZ, WD.meta(aWorld, aX, aY, aZ), 0);
		WD.set(aWorld, aX, aY, aZ, NB, 0, 2);
	}
	
	@Override public boolean onItemUseFirst(ItemBlockBase aItem, ItemStack aStack, Player aPlayer, Level aWorld, int aX, int aY, int aZ, int aSide, float aHitX, float aHitY, float aHitZ) {return F;}
	
	@Override
	public boolean onItemUse(ItemBlockBase aItem, ItemStack aStack, Player aPlayer, Level aWorld, int aX, int aY, int aZ, int aSide, float aHitX, float aHitY, float aHitZ) {
		if (aStack.getCount() == 0) return F;
		
		Block tBlock = WD.block(aWorld, aX, aY, aZ);
		BlockEntity tTileEntity = WD.te(aWorld, aX, aY, aZ, T);
		
		// F16 flower-pot ЗАКРЫТ РЕШЕНИЕМ (BUG-039 v4, FORCED-ADAPTATION): 1.7.10-ветка «посадить GT6-цветок в горшок»
		// работала через TileEntityFlowerPot-BE (mirror-класс, в рантайме NCDFE — был краш ПКМ 2026-07-19). В neo
		// наполненный горшок = отдельный POTTED_*-блок: для GT6-цветов потребовалась бы регистрация N собственных
		// potted-блоков + моделей — несоразмерно декоративной фиче. Деградация принята: GT6-цветы в горшок не
		// сажаются (no-op, ваниль сажается ванилью); данжен-горшки наполняются центром BlocksGT.potted.
		if (tBlock == Blocks.FLOWER_POT) return F;

		if (tBlock == Blocks.SNOW && (WD.meta(aWorld, aX, aY, aZ) & 7) < 1) {
			aSide = SIDE_UP;
		// было tBlock != Blocks.tallgrass (1.7.10 единый BlockTallGrass, meta grass/fern) -> neo раздвоил на
		// Blocks.GRASS/Blocks.FERN, оба instanceof TallGrassBlock [TallGrassBlock.java:15, Blocks.java:707-732] -
		// instanceof как 1:1-эквивалент identity-проверки единого класса (второй tBlock!=DEAD_BUSH дубль-баг порта устранён).
		} else if (tBlock != Blocks.VINE && !(tBlock instanceof net.minecraft.world.level.block.TallGrassBlock) && tBlock != Blocks.DEAD_BUSH && !WD.replaceable(tBlock, aWorld, aX, aY, aZ)) {
			aX += OFFX[aSide]; aY += OFFY[aSide]; aZ += OFFZ[aSide];
		}

		// World.canPlaceEntityOnSide восстановлен 1:1 через ЦЕНТР WD.canPlaceEntityOnSide (Forge-хук удалён по ИМЕНИ,
		// способность есть — коллизия формы с исключением размещающего + заменяемость цели; централизован в WD.java).
		if (!(aPlayer).mayUseItemAt(new BlockPos(aX, aY, aZ), FORGE_DIR[aSide], aStack) || (aY == WD.maxY(aWorld) && getMaterial().isSolid()) /* BUG-089: было aY == 255 — верх мира через центр F6-Y-scale */ || !WD.canPlaceEntityOnSide(aWorld, this, aX, aY, aZ, F, aSide, aPlayer, aStack)) return F;

		if (aItem.placeBlockAt(aStack, aPlayer, aWorld, aX, aY, aZ, aSide, aHitX, aHitY, aHitZ, onBlockPlaced(aWorld, aX, aY, aZ, aSide, aHitX, aHitY, aHitZ, aItem.getMetadata(aStack.getDamageValue())))) {
			WD.playStepSound(aWorld, aX+0.5F, aY+0.5F, aZ+0.5F, this);
			if (!UT.Entities.hasInfiniteItems(aPlayer)) aStack.setCount(aStack.getCount()-1);
		}
		return T;
	}
}
