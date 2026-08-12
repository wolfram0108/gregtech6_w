/**
 * Copyright (c) 2019 Gregorius Techneticies
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

package gregapi.block;

import net.minecraft.world.level.block.Block;


/**
 * @author Gregorius Techneticies
 */
public interface IBlock {
	public Block getBlock();
	/** F-bounds: 1.7.10 мутировал Block.mBoundingBox (рендер per-pass + коллизия). neo bounds immutable →
	 *  GT6-блок хранит последние заданные bounds сам; рендер-использование отложено на F3-клиент-проход. */
	public void setBlockBounds(float aMinX, float aMinY, float aMinZ, float aMaxX, float aMaxY, float aMaxZ);
	/** F-bounds (чтение, пара к setBlockBounds): последние заданные render-bounds {minX,minY,minZ,maxX,maxY,maxZ}.
	 *  Единый контракт для ВСЕХ Block-иерархий GT6 (BlockBase/флюиды/MTE/rail/prefix — общего предка нет);
	 *  читает GT6BlockModel.applyBounds (1.7.10 RenderBlocks.setRenderBoundsFromBlock) — под-боксы render-пассов. */
	public float[] getRenderBounds();
	/** F3 block-icon-data (читают ЦЕНТРЫ {@link gregapi.render.GT6QuadBuilder#resolveBlockFaceIcon} — лицо блока,
	 *  и {@code GT6BlockModel.particleMaterial} — крошка разрушения): 1.7.10 держал {@code Block.getIcon(side, meta)}
	 *  на САМОМ ванильном Block, поэтому канал был у каждого блока по определению движка и каждая GT6-иерархия просто
	 *  перекрывала его своим (BlockBaseMeta/Log/Beam/Leaves/Rail/Grass/Path — свои спрайты, PrefixBlock — по материалу,
	 *  MultiTileEntityBlock — CFOAM_HARDENED, жидкости — текстура жидкости). neo этот метод удалил вместе с {@code IIcon},
	 *  и контракт восстановлен здесь — по той же причине, что {@link #getRenderBounds()} и {@link #getHarvestTool(int)}:
	 *  общего Block-предка у иерархий GT6 нет.
	 *  <p>Дефолт — {@code null} = «канал иконки у этой иерархии не заведён» (в 1.7.10 ему соответствовал
	 *  унаследованный от ваниль-Block {@code blockIcon}, которого в neo нет); потребитель уходит на штатный baked-путь.
	 *  ⛔ Носитель канала обязан ОТВЕЧАТЬ, а не бросать: до 2026-07-29 шесть классов бросали здесь
	 *  {@code UnsupportedOperationException}, и оба центра-потребителя были обвешаны {@code catch (Throwable)} —
	 *  исключение в живом канале, замазанное на стороне читателя. */
	default net.minecraft.resources.ResourceLocation getIcon(int aSide, int aMeta) {return null;}
	/** F3 block-render-color (пара к {@link #getIcon}): 1.7.10 {@code Block.getRenderColor(meta)} — тинт блока,
	 *  и {@code colorMultiplier(world,x,y,z)} — тинт per-позиция. Оба удалены из neo Block. Дефолт — белый
	 *  ({@code 0x00ffffff} = «без собственного тинта», ровно как отдавал ванильный Block). */
	default int getRenderColor(int aMeta) {return 0x00ffffff;}
	default int colorMultiplier(net.minecraft.world.level.BlockGetter aWorld, int aX, int aY, int aZ) {return getRenderColor(gregapi.util.WD.meta(aWorld, aX, aY, aZ));}
	/** F-tool (читают ЦЕНТРЫ WD.harvestTool/WD.harvestLevel): 1.7.10 Forge держал getHarvestTool(int)/getHarvestLevel(int)
	 *  на САМОМ Block — каждая GT6-иерархия отвечала своими полями (BlockBase.mTool, MTE-Block.mTool, PrefixBlock.mTool,
	 *  Rail=crowbar). neo эту точку удалил; контракт восстанавливает её на едином IBlock (общего Block-предка у иерархий
	 *  нет — та же причина, что getRenderBounds выше). Носители перекрывают дефолт автоматически одноимёнными
	 *  существующими методами; не-носители (жидкости, Internal — как в 1.7.10 без override) — дефолт «без инструмента». */
	default String getHarvestTool(int aMeta) {return "";}
	/** Дефолт -1, а не 0: ровно так его держал 1.7.10 ({@code Block.java:2490} — массив уровней инициализирован
	 *  -1, «уровень не задан»). Носители, не перекрывающие метод (жидкости GT6, MTE-Internal), в оригинале
	 *  отдавали именно -1 — сверено набором {@code engine_block_passport.csv}. Потребители к этому готовы:
	 *  клампят тем же приёмом, что оригинал ({@code MultiItemTool.java:482} → {@code bind4} даёт 0), либо
	 *  сравнивают «инструмент сильнее» ({@code ForgeHooks.java:115}), где -1 верно означает «требования нет». */
	default int getHarvestLevel(int aMeta) {return -1;}
	/** F9 (читает ЦЕНТР {@link gregapi.util.WD#getMaterial}): 1.7.10 {@code Block.getMaterial()} — материал блока,
	 *  у GT6 это ДЕЙСТВУЮЩЕЕ правило инструментов ({@code GT_Tool_*.isMinableBlock} сверяет именно его), а не
	 *  украшение. Точка удалена из neo, и центр отбирал носителей ПЕРЕЧИСЛЕНИЕМ ИЕРАРХИЙ — из-за чего
	 *  {@code PrefixBlock} в список не попал и его 27 блоков падали в общий хвост {@code rock}: ящики отвечали
	 *  «камень» вместо {@code wood}, руды в песке/гравии/грязи — вместо {@code sand}/{@code ground}, корпуса
	 *  машин — вместо {@code iron} (замер против 1.7.10, набор {@code engine_block_passport.csv}). Отбор по
	 *  КОНТРАКТУ, как у harvestTool/harvestLevel выше. Дефолт {@code null} = «контракт величины не несёт»
	 *  (носители-предметы вроде {@code ItemBlockBase}), центр тогда идёт своим разбором дальше. */
	default Material getMaterial() {return null;}
	/** BUG-071 (пер-материальность уровня): в 1.7.10 движок звал {@code getHarvestLevel(мета блока)}, и мета несла
	 *  ОСМЫСЛЕННУЮ величину — у prefix-блока это {@code bind4(материал.mToolQuality)} (PrefixBlock:435 оригинала),
	 *  у MTE-блока {@code mBlockMetaData} класса (= {@code mToolQuality} материала машины, Loader_MultiTileEntities:895).
	 *  В порте канал меты занят ДРУГИМ (ID материала / подтип в BE), а на harvest-путях мета вырождается в 0
	 *  (IBlockExtendedMetaData:49) — связь «материал → требуемый уровень» терялась целиком.
	 *  Здесь контракт восстанавливает точку, которую 1.7.10 имел даром: «уровень блока В ЭТОЙ ПОЗИЦИИ». Дефолт —
	 *  прежнее поведение (уровень по мете порта); иерархии, у которых мета занята под другое, перекрывают его и
	 *  отдают 1.7.10-величину сами (знание о своей мете остаётся в блоке, как и было у Грегориуса).
	 *  Читает ЦЕНТР {@link gregapi.util.WD#harvestLevel(net.minecraft.world.level.BlockGetter,int,int,int)}. */
	default int getHarvestLevel(net.minecraft.world.level.BlockGetter aWorld, int aX, int aY, int aZ) {
		return getHarvestLevel(gregapi.util.WD.meta(aWorld, aX, aY, aZ));
	}
	/** F-hardness (читает ЦЕНТР WD.hardness): 1.7.10 Block.getBlockHardness(World,x,y,z) — Forge-точка per-position
	 *  твёрдости; GT6-иерархии несут свои значения (BlockBase-подклассы per-meta, PrefixBlock per-material,
	 *  MTE-блоки per-TE mHardness из NBT_HARDNESS) — носители перекрывают дефолт автоматически одноимёнными
	 *  методами. Дефолт = vanilla: destroyTime реального state на позиции. Потребитель — износ инструмента
	 *  (MultiItemTool.onBlockDestroyed: урон × твёрдость, 1:1 оригинал). */
	default float getBlockHardness(net.minecraft.world.level.Level aWorld, int aX, int aY, int aZ) {
		net.minecraft.core.BlockPos tPos = new net.minecraft.core.BlockPos(aX, aY, aZ);
		return aWorld.getBlockState(tPos).getDestroySpeed(aWorld, tPos);
	}
}
