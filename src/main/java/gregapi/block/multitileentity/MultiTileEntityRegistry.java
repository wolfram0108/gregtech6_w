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

package gregapi.block.multitileentity;

import gregapi.block.multitileentity.IMultiTileEntity.*;
import gregapi.code.ArrayListNoNulls;
import gregapi.code.HashSetNoNulls;
import gregapi.code.ItemNBT;
import gregapi.data.LH;
import gregapi.item.CreativeTab;
import gregapi.recipes.Recipe.RecipeMap;
import gregapi.render.RendererBlockTextured;
import gregapi.util.CR;
import gregapi.util.ST;
import gregapi.util.UT;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.Level;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static gregapi.data.CS.*;

/**
 * @author Gregorius Techneticies
 * 
 * Create yourself a new MultiTileEntity Registry in order to have your very own 32767 Sub IDs.
 * You can do with those IDs whatever you want since this automatically creates your personal Item and DOESN'T use any Items or Blocks of GregTech itself.
 * 
 * Whatever you do, DO NOT GET THE UTTERLY RETARDED IDEA OF ADDING YOUR MULTITILEENTITIES TO MY REGISTRY!!! INSTANCIATE YOUR OWN REGISTRY!!!
 * 
 * ================================================================================================================================================
 * The way this whole System works is very simple. The setTileEntity call can set the TileEntity of your choice at every Location you want.
 * If now the BlockContainer doesn't return a TileEntity, but instead the ItemBlock manually sets the TileEntity, you can have every single
 * TileEntity being placed at every Block you want. If that Block then is compatible with your TileEntity (via Interfaces and such) it can
 * easily make use of the TileEntity no matter which one it is.
 * 
 * "But what is with the Loading of those TileEntities? Don't they get deleted on startup?" You think? No they don't get deleted. Minecraft
 * can load every TileEntity just by a Name->Class Map (you know about that when you have ever created a TileEntity yourself), and the remaining
 * Stats can be saved inside the NBT of the TileEntity.
 * 
 * In the end I have a dynamic collection of Blocks to get the vanilla Materials and Sound Effects right, a Registry of TileEntities to be
 * attached to those Blocks via additional custom ItemBlocks to enable everything, and an automatic Network Handler.
 * 
 * The only thing needed to be done manually is something that transmits the Data from the Server to the Client to set the proper TileEntity there.
 * 
 * In order to do that, just send one of the 5 Packets (PacketSyncDataByteAndIDs, PacketSyncDataShortAndIDs, PacketSyncDataIntegerAndIDs, 
 * PacketSyncDataLongAndIDs or PacketSyncDataByteArrayAndIDs) for transmitting the ID to the Client with aID1 = getMultiTileEntityRegistryID() and
 * aID2 = getMultiTileEntityID(). The Byte/Short/Integer/Long/ByteArray can be used for transmitting other Data, such as a Facing to the Client.
 * ================================================================================================================================================
 */
public class MultiTileEntityRegistry {
	private static final HashMap<String, MultiTileEntityRegistry> NAMED_REGISTRIES = new HashMap<>();
	private static final HashSetNoNulls<Class<?>> sRegisteredTileEntities = new HashSetNoNulls<>();
	private static final HashSetNoNulls<String> sRegisteredTileEntityClassNames = new HashSetNoNulls<>();
	private final HashSetNoNulls<Class<?>> mRegisteredTileEntities = new HashSetNoNulls<>();
	
	public HashMap<Short, CreativeTab> mCreativeTabs = new HashMap<>();
	public Map<Short, MultiTileEntityClassContainer> mRegistry = new HashMap<>();
	public List<MultiTileEntityClassContainer> mRegistrations = new ArrayListNoNulls<>();
	
	public final String mNameInternal;
	public final MultiTileEntityBlockInternal mBlock;
	
	private static final MultiTileEntityBlockInternal regblock(String aNameInternal, MultiTileEntityBlockInternal aBlock, Class<? extends BlockItem> aItemClass) {
		ST.register(aBlock, aNameInternal, aItemClass);
		return aBlock;
	}
	
	/** @param aNameInternal the internal Name of the Item. DO NOT START YOUR UNLOCALISED NAME WITH "gt."!!! */
	public MultiTileEntityRegistry(String aNameInternal) {this(aNameInternal, new MultiTileEntityBlockInternal(aNameInternal), MultiTileEntityItemInternal.class);}
	/** @param aNameInternal the internal Name of the Item. DO NOT START YOUR UNLOCALISED NAME WITH "gt."!!! */
	public MultiTileEntityRegistry(String aNameInternal, MultiTileEntityBlockInternal aBlock, Class<? extends BlockItem> aItemClass) {this(aNameInternal, aBlock, aItemClass, RendererBlockTextured.INSTANCE);}
	/** @param aNameInternal the internal Name of the Item. DO NOT START YOUR UNLOCALISED NAME WITH "gt."!!! */
	public MultiTileEntityRegistry(String aNameInternal, MultiTileEntityBlockInternal aBlock, Class<? extends BlockItem> aItemClass, Object aItemRenderer) {
		this(aNameInternal, regblock(aNameInternal, aBlock, aItemClass));
		// F3 superseded-render (GT6BlockModel/ItemModel пайплайн; старый getIcon/immediate-mode мёртв, 0 вызовов neo): было MinecraftForgeClient.registerItemRenderer(...) (net.minecraftforge.client
		// удалён целиком в 26.1.2, RendererBlockTextured больше не implements IItemRenderer — decisions/F3-render.md §2.1/§3
		// "IItemRenderer"). Реальная регистрация item-модели — RegisterBlockStateModels/ModelEvent.RegisterStandalone (Фаза C).
	}
	/** @param aNameInternal the internal Name of the Item. DO NOT START YOUR UNLOCALISED NAME WITH "gt."!!! */
	public MultiTileEntityRegistry(String aNameInternal, MultiTileEntityBlockInternal aBlock) {
		if (!GAPI.mStartedPreInit) throw new IllegalStateException("The MultiTileEntity Registry must be initialised at the Preload Phase and not before, because it relies on an ItemBlock being created!");
		if (GAPI.mStartedInit) throw new IllegalStateException("The MultiTileEntity Registry must be initialised at the Preload Phase and not later, because it relies on an ItemBlock being created!");
		mNameInternal = aNameInternal;
		mBlock = aBlock;
		mBlock.mMultiTileEntityRegistry = this;
		NAMED_REGISTRIES.put(mNameInternal, this);
	}
	
	/** Whatever you do, DO NOT GET THE UTTERLY RETARDED IDEA OF ADDING YOUR MULTITILEENTITIES TO MY OWN REGISTRY!!! Create your own instance! */
	// F-registry-id (КОРЕНЬ regFound=false): прежний REGISTRIES.get(new ItemStackContainer(Item.byId(reg))) промахивался, ПОТОМУ ЧТО
	// REGISTRIES (ItemStackMap) заполнялся в конструкторе на PreInit — когда блок-итем ЕЩЁ НЕ зарегистрирован (neo DeferredRegister
	// создаёт объект на RegisterEvent) → ключ mItem = Item.byBlock(mBlock) = AIR (воздух, id 0). А get по реальному Item.byId(reg) ≠ AIR →
	// null ДАЖЕ с верным item-id (GameTest: regID=1646 валиден, regFound=false). Резолвим по СВЕЖЕМУ ключу на CALL-time (рантайм/load —
	// итемы заморожены, Item.byBlock(mBlock) резолвится): item-id блок-итема каждого реестра. Реестров единицы (NAMED_REGISTRIES), перебор дёшев.
	public static MultiTileEntityRegistry getRegistry(int aRegistryID) {
		for (MultiTileEntityRegistry tRegistry : NAMED_REGISTRIES.values()) if (ST.id(ST.item(tRegistry.mBlock)) == aRegistryID) return tRegistry;
		return null;
	}
	
	/** Whatever you do, DO NOT GET THE UTTERLY RETARDED IDEA OF ADDING YOUR MULTITILEENTITIES TO MY OWN REGISTRY!!! Create your own instance! */
	public static MultiTileEntityRegistry getRegistry(String aRegistryName) {
		return NAMED_REGISTRIES.get(aRegistryName);
	}
	
	public static BlockEntity getCanonicalTileEntity(int aRegistryID, int aMultiTileEntityID) {
		MultiTileEntityRegistry tRegistry = getRegistry(aRegistryID);
		if (tRegistry == null) return null;
		MultiTileEntityClassContainer tClassContainer = tRegistry.getClassContainer(aMultiTileEntityID);
		if (tClassContainer == null) return null;
		return tClassContainer.mCanonicalTileEntity;
	}
	
	public static BlockEntity getCanonicalTileEntity(String aRegistryName, int aMultiTileEntityID) {
		MultiTileEntityRegistry tRegistry = getRegistry(aRegistryName);
		if (tRegistry == null) return null;
		MultiTileEntityClassContainer tClassContainer = tRegistry.getClassContainer(aMultiTileEntityID);
		if (tClassContainer == null) return null;
		return tClassContainer.mCanonicalTileEntity;
	}
	
	/** Returns the MultiTileEntityRegistry ID that is currently used by this World. */
	// F-registry-id: item-id блок-итема (Item.byId-резолвимый в getRegistry), НЕ block-id (см. коммент в getNewTileEntityContainer).
	public int currentID() {return ST.id(ST.item(mBlock));}
	
	/** Adds a new MultiTileEntity. It is highly recommended to do this in either the PreInit or the Init Phase. PostInit might not work well.*/
	public ItemStack add(String aLocalised, String aCategoricalName, int aID, int aCreativeTabID, Class<? extends BlockEntity> aClass, int aBlockMetaData, int aStackSize, MultiTileEntityBlock aBlock, CompoundTag aParameters, Object... aRecipe) {
		return add(aLocalised, aCategoricalName, new MultiTileEntityClassContainer(aID, aCreativeTabID, aClass, aBlockMetaData, aStackSize, aBlock, aParameters), aRecipe);
	}
	
	/** Adds a new MultiTileEntity. It is highly recommended to do this in either the PreInit or the Init Phase. PostInit might not work well.*/
	public ItemStack add(String aLocalised, String aCategoricalName, MultiTileEntityClassContainer aClassContainer, Object... aRecipe) {
		boolean tFailed = F;
		if (UT.Code.stringInvalid(aLocalised)) {
			ERR.println("MTE REGISTRY ERROR: Localisation Missing!");
			tFailed = T;
		}
		if (aClassContainer == null) {
			ERR.println("MTE REGISTRY ERROR: Class Container is null!");
			tFailed = T;
		} else {
			if (aClassContainer.mClass == null) {
				ERR.println("MTE REGISTRY ERROR: Class inside Class Container is null!");
				tFailed = T;
			}
			if (aClassContainer.mID == W) {
				ERR.println("MTE REGISTRY ERROR: Class Container uses Wildcard MetaData!");
				tFailed = T;
			}
			if (aClassContainer.mID < 0) {
				ERR.println("MTE REGISTRY ERROR: Class Container uses negative MetaData!");
				tFailed = T;
			}
			if (mRegistry.containsKey(aClassContainer.mID)) {
				ERR.println("MTE REGISTRY ERROR: Class Container uses occupied MetaData!");
				tFailed = T;
			}
		}
		if (tFailed) {
			ERR.println("MTE REGISTRY ERROR: STACKTRACE START");
			int i = 0; for (StackTraceElement tElement : new Exception().getStackTrace()) if (i++<5 && !tElement.getClassName().startsWith("sun")) ERR.println("\tat " + tElement); else break;
			ERR.println("MTE REGISTRY ERROR: STACKTRACE END");
			return null;
		}
		assert aClassContainer != null;
		LH.add(mNameInternal+"."+aClassContainer.mID, aLocalised);
		mRegistry.put(aClassContainer.mID, aClassContainer);
		mLastRegisteredID = aClassContainer.mID;
		mRegistrations.add(aClassContainer);
		if (!mCreativeTabs.containsKey(aClassContainer.mCreativeTabID)) mCreativeTabs.put(aClassContainer.mCreativeTabID, new CreativeTab(mNameInternal+"."+aClassContainer.mCreativeTabID, aCategoricalName, Item.byBlock(mBlock), aClassContainer.mCreativeTabID));
		// F3-render (КРИТ, гейт②-находка): *Client-хуки регистрации — это TESR/immediate-mode рендер-инициализация (напр.
		// MultiTileEntityChest.onRegistrationFirstClient → ClientRegistry.bindTileEntitySpecialRenderer, 1.7.10-API удалён в neo →
		// NoSuchFieldException). БЕЗ try-catch необработанное исключение ОБРЫВАЛО Loader_MultiTileEntities → в runClient регистрировался
		// лишь 1 MTE из сотен (рок/палка/машины отсутствовали → placeBlock=false, worldgen пуст). Рендер MTE = Фаза C: сбой client-хука
		// = лог+продолжить (тот же приём, что вся отложенная F3-render-инициализация). Серверные хуки НЕ глушим (их сбой = реальный баг).
		if (sRegisteredTileEntityClassNames.add(aClassContainer.mCanonicalTileEntity.getClass().getName()) && sRegisteredTileEntities.add(aClassContainer.mCanonicalTileEntity.getClass())) {
			if (aClassContainer.mCanonicalTileEntity instanceof IMTE_OnRegistrationFirst) ((IMTE_OnRegistrationFirst)aClassContainer.mCanonicalTileEntity).onRegistrationFirst(this, aClassContainer.mID);
			if (CODE_CLIENT && aClassContainer.mCanonicalTileEntity instanceof IMTE_OnRegistrationFirstClient) try {((IMTE_OnRegistrationFirstClient)aClassContainer.mCanonicalTileEntity).onRegistrationFirstClient(this, aClassContainer.mID);} catch (Throwable e) {e.printStackTrace(ERR);}
		}
		if (mRegisteredTileEntities.add(aClassContainer.mCanonicalTileEntity.getClass())) {
			if (aClassContainer.mCanonicalTileEntity instanceof IMTE_OnRegistrationFirstOfRegister) ((IMTE_OnRegistrationFirstOfRegister)aClassContainer.mCanonicalTileEntity).onRegistrationFirstOfRegister(this, aClassContainer.mID);
			if (CODE_CLIENT && aClassContainer.mCanonicalTileEntity instanceof IMTE_OnRegistrationFirstOfRegisterClient) try {((IMTE_OnRegistrationFirstOfRegisterClient)aClassContainer.mCanonicalTileEntity).onRegistrationFirstOfRegisterClient(this, aClassContainer.mID);} catch (Throwable e) {e.printStackTrace(ERR);}
		}
		if (aClassContainer.mCanonicalTileEntity instanceof IMTE_OnRegistration) {
			((IMTE_OnRegistration)aClassContainer.mCanonicalTileEntity).onRegistration(this, aClassContainer.mID);
		}
		if (CODE_CLIENT && aClassContainer.mCanonicalTileEntity instanceof IMTE_OnRegistrationClient) {
			try {((IMTE_OnRegistrationClient)aClassContainer.mCanonicalTileEntity).onRegistrationClient(this, aClassContainer.mID);} catch (Throwable e) {e.printStackTrace(ERR);}
		}
		if (aRecipe != null && aRecipe.length > 1) {
			if (aRecipe[0] instanceof Object[]) aRecipe = (Object[])aRecipe[0];
			if (aRecipe.length > 2) CR.shaped(getItem(aClassContainer.mID), CR.DEF_REV_NCC, aRecipe);
		}
		// A simple special case to make it easier to add a Machine to Recipe Lists without having to worry about anything.
		String
		tRecipeMapName = aClassContainer.mParameters.getString(NBT_RECIPEMAP);
		if (UT.Code.stringValid(tRecipeMapName)) {RecipeMap tMap = RecipeMap.RECIPE_MAPS.get(tRecipeMapName); if (tMap != null) tMap.mRecipeMachineList.add(getItem(aClassContainer.mID));}
		tRecipeMapName = aClassContainer.mParameters.getString(NBT_FUELMAP);
		if (UT.Code.stringValid(tRecipeMapName)) {RecipeMap tMap = RecipeMap.RECIPE_MAPS.get(tRecipeMapName); if (tMap != null) tMap.mRecipeMachineList.add(getItem(aClassContainer.mID));}
		return getItem(aClassContainer.mID);
	}
	
	public short mLastRegisteredID = W;
	
	public ItemStack getItem() {return getItem(mLastRegisteredID, 1, null);}
	public ItemStack getItem(CompoundTag aNBT) {return getItem(mLastRegisteredID, 1, aNBT);}
	public ItemStack getItem(int aID) {return getItem(aID, 1, null);}
	public ItemStack getItem(int aID, CompoundTag aNBT) {return getItem(aID, 1, aNBT);}
	public ItemStack getItem(int aID, long aAmount) {return getItem(aID, aAmount, null);}
	
	public ItemStack getItem(int aID, long aAmount, CompoundTag aNBT) {
		ItemStack rStack = ST.make(mBlock, (int)aAmount, aID);
		if (aNBT == null) aNBT = UT.NBT.make();
		if (aNBT.isEmpty()) {
			MultiTileEntityContainer tTileEntityContainer = getNewTileEntityContainer(aID, aNBT);
			if (tTileEntityContainer != null) ((IMultiTileEntity)tTileEntityContainer.mTileEntity).writeItemNBT(aNBT);
		}
		UT.NBT.set(rStack, aNBT);
		return rStack;
	}
	
	public String getLocal(int aID) {return LH.get(mNameInternal+"."+aID);}
	
	public MultiTileEntityClassContainer getClassContainer(int aID) {return mRegistry.get((short)aID);}
	public MultiTileEntityClassContainer getClassContainer(ItemStack aStack) {return mRegistry.get(ST.meta_(aStack));}
	
	public BlockEntity getNewTileEntity(int aID)                                                 {MultiTileEntityContainer tContainer =  getNewTileEntityContainer(null  ,  0,  0,  0, aID, null); return tContainer == null ? null : tContainer.mTileEntity;}
	public BlockEntity getNewTileEntity(Level aWorld, int aX, int aY, int aZ, int aID)           {MultiTileEntityContainer tContainer =  getNewTileEntityContainer(aWorld, aX, aY, aZ, aID, null); return tContainer == null ? null : tContainer.mTileEntity;}
	
	/**
	 * BUG-074/078 — ЕДИНСТВЕННАЯ точка компенсации item-facing для detached-TE (форма предмета, мира нет).
	 *
	 * <p>Путей рождения такого TE ровно два, и оба зовут этот метод: обычный item-рендер
	 * ({@code MultiTileEntityBlockInternal.passRenderingToObject}) и BER-ветка для предметов со своим
	 * рендерером ({@code MultiTileEntityBER.extractArgument} — сундук, масстораж). Величину задаёт сам TE
	 * ({@code IMTE_ItemFacing.getItemFacing}), поэтому семья с иной раскладкой граней меняет
	 * ОДНО переопределение, а не строку в каждом {@code getTexture2}.</p>
	 *
	 * <p>Получатели отбираются по КОНТРАКТУ {@code IMTE_ItemFacing}, а не по месту в иерархии: сундук
	 * носит собственную грань вне {@code TileEntityBase09FacingSingle} (как и в оригинале), и отбор по
	 * классу базы проходил мимо него — приём приходилось повторять руками в его рендер-состоянии.</p>
	 */
	public static BlockEntity applyItemFacing(BlockEntity aTileEntity) {
		if (aTileEntity instanceof IMultiTileEntity.IMTE_ItemFacing tFacingTE) tFacingTE.setItemFacing(tFacingTE.getItemFacing());
		return aTileEntity;
	}

	public BlockEntity getNewTileEntity(ItemStack aStack)                                        {MultiTileEntityContainer tContainer =  getNewTileEntityContainer(null  ,  0,  0,  0, ST.meta_(aStack), ItemNBT.get(aStack)); return tContainer == null ? null : tContainer.mTileEntity;}
	public BlockEntity getNewTileEntity(Level aWorld, int aX, int aY, int aZ, ItemStack aStack)  {MultiTileEntityContainer tContainer =  getNewTileEntityContainer(aWorld, aX, aY, aZ, ST.meta_(aStack), ItemNBT.get(aStack)); return tContainer == null ? null : tContainer.mTileEntity;}
	
	public MultiTileEntityContainer getNewTileEntityContainer(ItemStack aStack)                                                 {return getNewTileEntityContainer(null  ,  0,  0,  0, ST.meta_(aStack), ItemNBT.get(aStack));}
	public MultiTileEntityContainer getNewTileEntityContainer(Level aWorld, int aX, int aY, int aZ, ItemStack aStack)           {return getNewTileEntityContainer(aWorld, aX, aY, aZ, ST.meta_(aStack), ItemNBT.get(aStack));}
	
	public MultiTileEntityContainer getNewTileEntityContainer(int aID, CompoundTag aNBT) {return getNewTileEntityContainer(null, 0, 0, 0, aID, aNBT);}
	public MultiTileEntityContainer getNewTileEntityContainer(net.minecraft.world.level.LevelAccessor aWorld, int aX, int aY, int aZ, int aID, CompoundTag aNBT) {
		MultiTileEntityClassContainer tClass = mRegistry.get((short)aID);
		if (tClass == null || tClass.mBlock == null) return null;
		// F-tileentity-construction (ADR, placement-pos): при МИРОВОЙ постановке (aWorld!=null) передаём реальную (aX,aY,aZ) в
		// no-arg-конструктор MTE через центральный канал PENDING_WORLD_POS (worldPosition в neo immutable — BlockEntity.java:48-59;
		// вся MTE-иерархия на no-arg-ctor, см. TileEntityBase01Root). Иначе TE садится на BlockPos.ZERO, а WD.te крепит его по
		// te.getBlockPos()=(0,0,0), не на своё место → камни/палки/источники/машины остаются без TE (region-scan gt.multitileentity=0).
		// aWorld==null (item-form/getDrops/detached) → канал пуст → ZERO (data-контейнер, позиция не нужна). Тот же канал даёт pos и
		// load-реконструкции MTE (getNewTileEntity с реальными координатами стаба на ChunkEvent.Load).
		BlockEntity tTileEntity;
		if (aWorld != null) gregapi.tileentity.base.TileEntityBase01Root.PENDING_WORLD_POS.set(new net.minecraft.core.BlockPos(aX, aY, aZ));
		try {tTileEntity = (BlockEntity)UT.Reflection.callConstructor(tClass.mClass, -1, null, T);}
		finally {gregapi.tileentity.base.TileEntityBase01Root.PENDING_WORLD_POS.remove();}
		MultiTileEntityContainer rContainer = new MultiTileEntityContainer(tTileEntity, tClass.mBlock, tClass.mBlockMetaData);
		if (rContainer.mTileEntity == null) return null;
		// было TileEntity.setWorldObj(World) (1.7.10, recompSrc TileEntity.java:70) -> BlockEntity.setLevel(Level) [BlockEntity.java:93].
		// F6-worldgen: приёмник расширен до LevelAccessor; BE.setLevel хочет полный Level → на worldgen (WorldGenLevel/
		// ServerLevelAccessor) берём итоговый ServerLevel через getLevel() (BE к нему и привяжется при финализации чанка);
		// на gameplay это сам Level; экзотический LevelAccessor без обоих → null (как aWorld==null: контейнер без мира).
		rContainer.mTileEntity.setLevel(aWorld instanceof Level tLvl ? tLvl : aWorld instanceof net.minecraft.world.level.ServerLevelAccessor tSLA ? tSLA.getLevel() : null);
		// F-registry-id (КОРНЕВОЙ фикс load-реконструкции): registry-ID тайла = ITEM-id блок-итема, т.к. getRegistry(int) матчит его как
		// `ST.id(ST.item(reg.mBlock)) == id` (см. getRegistry). В 1.7.10 block-id==item-id (общее id-пространство, ItemBlock делил id блока) —
		// там ST.id(mBlock) годился. В neo BLOCK и ITEM — РАЗНЫЕ реестры с независимыми id, а `ST.id(mBlock)` резолвится в overload id(Block)=
		// BLOCK.getId → block-id (напр. 1168) ≠ item-id → getRegistry(block-id) промахивался. Правильно: ST.id(ST.item(mBlock)) = item-id
		// (ST.item(Block)=Item.byBlock). getMultiTileEntityRegistryID() теперь = item-id, network-safe (item-id синхронизирован клиент/сервер).
		((IMultiTileEntity)rContainer.mTileEntity).initFromNBT(aNBT == null || aNBT.isEmpty() ? tClass.mParameters : UT.NBT.fuse(aNBT, tClass.mParameters), (short)aID, (short)ST.id(ST.item(mBlock)));
		return rContainer;
	}
	
	public static void onServerStart() {for (Class<?> tClass : sRegisteredTileEntities) if (IMTE_OnServerStart.class.isAssignableFrom(tClass)) try {((IMTE_OnServerStart)tClass.newInstance()).onServerStart();} catch (Throwable e) {e.printStackTrace(ERR);}}
	public static void onServerStop () {for (Class<?> tClass : sRegisteredTileEntities) if (IMTE_OnServerStop .class.isAssignableFrom(tClass)) try {((IMTE_OnServerStop )tClass.newInstance()).onServerStop ();} catch (Throwable e) {e.printStackTrace(ERR);}}
	
	public static void onServerLoad(File aSaveLocation) {for (Class<?> tClass : sRegisteredTileEntities) if (IMTE_OnServerLoad.class.isAssignableFrom(tClass)) try {((IMTE_OnServerLoad)tClass.newInstance()).onServerLoad(aSaveLocation);} catch (Throwable e) {e.printStackTrace(ERR);}}
	public static void onServerSave(File aSaveLocation) {for (Class<?> tClass : sRegisteredTileEntities) if (IMTE_OnServerSave.class.isAssignableFrom(tClass)) try {((IMTE_OnServerSave)tClass.newInstance()).onServerSave(aSaveLocation);} catch (Throwable e) {e.printStackTrace(ERR);}}
}
