/*
 * Copyright 2010-2016 Institut Pasteur.
 * 
 * This file is part of Icy.
 * 
 * Icy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Icy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Icy. If not, see <http://www.gnu.org/licenses/>.
 */
package plugins.danyfel80.topologicalnetworkdescription.specific;

import java.util.List;

import javax.vecmath.Point3d;

import icy.plugin.abstract_.PluginActionable;
import icy.roi.ROI2D;
import icy.type.point.Point5D;
import plugins.adufour.blocks.lang.Block;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.vars.lang.VarSequence;

/**
 * @author Daniel Felipe Gonzalez Obando
 */
public class FilterROI2DPointsWithDistanceMap extends PluginActionable implements Block {

	VarSequence	annotatedSequence;
	VarSequence	distanceMap;


	/*
	 * (non-Javadoc)
	 * @see
	 * plugins.adufour.blocks.lang.Block#declareInput(plugins.adufour.blocks.util.
	 * VarList)
	 */
	@Override
	public void declareInput(VarList inputMap) {
		annotatedSequence = new VarSequence("Annotated sequence", null);
		distanceMap = new VarSequence("Distance map", null);
		inputMap.add(annotatedSequence.getName(), annotatedSequence);
		inputMap.add(distanceMap.getName(), distanceMap);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * plugins.adufour.blocks.lang.Block#declareOutput(plugins.adufour.blocks.util
	 * .VarList)
	 */
	@Override
	public void declareOutput(VarList outputMap) {}

	/*
	 * (non-Javadoc)
	 * @see plugins.adufour.blocks.lang.Block#run()
	 */
	@Override
	public void run() {
		if (annotatedSequence.getValue() == null || annotatedSequence.getValue().isEmpty()) {
			throw new IllegalArgumentException("null annotated sequence.");
		}
		if (distanceMap.getValue() == null || distanceMap.getValue().isEmpty()) {
			throw new IllegalArgumentException("null distance map.");
		}

		List<ROI2D> rois = annotatedSequence.getValue().getROIs(ROI2D.class, false);
		for (int i = 0; i < rois.size() - 1; i++) {
			Point5D point5dI = ((ROI2D) rois.get(i)).getPosition5D();
			Point3d point3dI = new Point3d(point5dI.getX(), point5dI.getY(), point5dI.getZ());
			for (int j = i + 1; j < rois.size(); j++) {
				Point5D point5dJ = ((ROI2D) rois.get(j)).getPosition5D();
				Point3d point3dJ = new Point3d(point5dJ.getX(), point5dJ.getY(), point5dJ.getZ());

				double distance = point3dI.distance(point3dJ);
				double distanceMapValue = distanceMap.getValue().getData(0, (int) point3dI.z, 0, (int) point3dI.y,
						(int) point3dI.x);
				if (distance < 2*distanceMapValue) {
					annotatedSequence.getValue().removeROI(rois.get(i));
					break;
				}
			}
		}
	}

}
