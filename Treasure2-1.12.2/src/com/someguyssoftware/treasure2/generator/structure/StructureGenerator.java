/**
 * 
 */
package com.someguyssoftware.treasure2.generator.structure;

import java.util.Map.Entry;
import java.util.Random;

import com.someguyssoftware.gottschcore.positional.Coords;
import com.someguyssoftware.gottschcore.positional.ICoords;
import com.someguyssoftware.gottschcore.world.gen.structure.GottschTemplate;
import com.someguyssoftware.gottschcore.world.gen.structure.StructureMarkers;
import com.someguyssoftware.treasure2.Treasure;
import com.someguyssoftware.treasure2.generator.GenUtil;
import com.someguyssoftware.treasure2.meta.StructureArchetype;
import com.someguyssoftware.treasure2.meta.StructureMeta;
import com.someguyssoftware.treasure2.meta.StructureType;
import com.someguyssoftware.treasure2.world.gen.structure.IStructureInfo;
import com.someguyssoftware.treasure2.world.gen.structure.StructureInfo;
import com.someguyssoftware.treasure2.world.gen.structure.TemplateHolder;

import lombok.Setter;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.structure.template.PlacementSettings;

/**
 * @author Mark Gottschling on Jan 24, 2019
 *
 */
@Setter
public class StructureGenerator implements IStructureGenerator {

	private Block nullBlock;
	
	public StructureGenerator() {
		// use the default null block
		setNullBlock(GenUtil.getMarkerBlock(StructureMarkers.NULL));
	}
	
	/**
	 * 
	 */
	@Override
	public IStructureInfo generate(World world, Random random, TemplateHolder templateHolder, PlacementSettings placement,
			ICoords coords) {

		GottschTemplate template = (GottschTemplate) templateHolder.getTemplate();
		
		// get the meta
		StructureMeta meta = (StructureMeta) Treasure.META_MANAGER.getMetaMap().get(templateHolder.getMetaLocation().toString());
		if (meta == null) {
			Treasure.logger.debug("Unable to locate meta data for template -> {}", templateHolder.getLocation());
			return null;
		}

		// TODO getMarkerBlock should be in StructureGenerator as well (passed in)
		// TODO move StructureGenerator to world.gen.structure (in gottschcore)
		// find the offset block
		int offset = 0;
		ICoords offsetCoords = null;
		if (meta.getOffset() != null) {
			// NOTE going to need to negate meta offset since a negative value will be provided for downward movement, whereas
			// an offset derived from a template will always be positive and thus is negated later to correct the positioning.
			offsetCoords = new Coords(0, -meta.getOffset().getY(), 0);
			Treasure.logger.debug("Using meta offset coords -> {}", offsetCoords);
		}
		else {
			offsetCoords = template.findCoords(random, GenUtil.getMarkerBlock(StructureMarkers.OFFSET));
		}
		
		if (offsetCoords != null) {
			offset = -offsetCoords.getY();
		}
		
		// update the spawn coords with the offset
		ICoords spawnCoords = coords.add(0, offset, 0);
		
		// TODO structure gen should probably pass in the replacement map
		// generate the structure
		template.addBlocksToWorld(world, spawnCoords.toPos(), placement, getNullBlock(), Treasure.TEMPLATE_MANAGER.getReplacementMap(), 3);
		
		// TODO if this is handled on template read, this block can go away
		// remove any extra special blocks
		for (ICoords mapCoords : template.getMapCoords()) {
			ICoords c = GottschTemplate.transformedCoords(placement, mapCoords);
			// TODO shouldn't be setting to air, but to null block
			world.setBlockToAir(spawnCoords.toPos().add(c.toPos()));
//			Treasure.logger.debug("removing mapped block -> {} : {}", c, spawnCoords.toPos().add(c.toPos()));
		}
		
		// get the transformed size
		BlockPos transformedSize = template.transformedSize(placement.getRotation());
				
		// TODO need to capture the facing or meta of the chest, perform the rotation on the facing  and save it in the Map with the pos... need a new object to hold more data
				
		// update StrucutreInfo
		IStructureInfo info = new StructureInfo();
		info.setCoords(spawnCoords);
		info.setSize(new Coords(transformedSize));
		// process all specials and adding them to the StructureInfo
		// TODO change to stream
		for (Entry<Block, ICoords> entry : template.getMap().entries()) {
			ICoords c = new Coords(GottschTemplate.transformedCoords(placement, entry.getValue()));
			info.getMap().put(entry.getKey(), c);
			Treasure.logger.debug("adding to structure info transformed coords -> {} : {}", entry.getKey().getLocalizedName(), c.toShortString());
		}
		
		return info;
	}

	@Override
	public Block getNullBlock() {
		if (nullBlock == null) {
			nullBlock = GenUtil.getMarkerBlock(StructureMarkers.NULL);
		}
		return nullBlock;
	}
	
//
//	@Override
//	public void setNullBlock(Block nullBlock) {
//		this.nullBlock = nullBlock;
//	}
}