package algorithms.danyfel80.topologicalnetworkdescription;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import icy.image.IcyBufferedImage;
import icy.sequence.Sequence;
import icy.sequence.SequenceUtil;
import icy.type.DataType;
import icy.type.TypeUtil;
import icy.type.point.Point5D;
import plugins.kernel.roi.roi2d.ROI2DPoint;

/**
 * This class allows to segment a binary image using region growing starting
 * from a given set of seed points. The segmentation creates connected
 * components that respect the predicate of having a value different than zero.
 * 
 * @author Daniel Felipe Gonzalez Obando
 */
public class RegionGrowingSegmenter {

	public static Sequence process(Sequence thresholdedSequence, List<ROI2DPoint> seeds, Sequence labelsSequence) {
		thresholdedSequence = SequenceUtil.convertToType(thresholdedSequence, DataType.DOUBLE, false);
		int sizeX = thresholdedSequence.getSizeX();
		int sizeY = thresholdedSequence.getSizeY();
		int sizeZ = thresholdedSequence.getSizeZ();

		Sequence connectedComponents = new Sequence(thresholdedSequence.getName() + "_ConnectedComponents");
		Sequence labeledConnectedComponents = (labelsSequence == null)
		    ? new Sequence(thresholdedSequence.getName() + "_LabeledConnectedComponents") : labelsSequence;
		connectedComponents.beginUpdate();
		labeledConnectedComponents.beginUpdate();
		for (int z = 0; z < thresholdedSequence.getSizeZ(); z++) {
			IcyBufferedImage tempImage = new IcyBufferedImage(thresholdedSequence.getSizeX(), thresholdedSequence.getSizeY(),
			    1, DataType.UBYTE);
			IcyBufferedImage tempImage1 = new IcyBufferedImage(thresholdedSequence.getSizeX(), thresholdedSequence.getSizeY(),
			    1, DataType.INT);
			connectedComponents.setImage(0, z, tempImage);
			labeledConnectedComponents.setImage(0, z, tempImage1);
		}
		connectedComponents.endUpdate();
		labeledConnectedComponents.endUpdate();

		Queue<Point5D> q = new LinkedList<Point5D>();
		Queue<Integer> ql = new LinkedList<Integer>();
		int parentId = 1;
		for (ROI2DPoint roi2dPoint : seeds) {
			Point5D point = roi2dPoint.getPosition5D();
			q.add(point);
			ql.add(parentId++);
		}

		double[][][] thresholdedData = thresholdedSequence.getDataXYCZAsDouble(0);
		byte[][][] ccData = connectedComponents.getDataXYCZAsByte(0);
		int[][][] lccData = labeledConnectedComponents.getDataXYCZAsInt(0);
		int[] labelParents = new int[parentId * 2];
		for (int i = 0; i < parentId; i++) {
			labelParents[i] = i;
		}

		while (!q.isEmpty()) {

			Point5D point = q.poll();
			int pX = (int) point.getX();
			int pY = (int) point.getY();
			int pZ = (int) point.getZ();
			int pT = (int) point.getT();
			int pC = (int) point.getC();
			int pId = ql.poll();
			try {
				// if are thresholded
				if (thresholdedData[pZ][0][pX + (pY) * sizeX] > 0) {
					// and not yet labeled
					if (lccData[pZ][0][pX + (pY) * sizeX] == 0) {
						ccData[pZ][0][pX + (pY) * sizeX] = (byte) DataType.UBYTE.getMaxValue();
						lccData[pZ][0][pX + (pY) * sizeX] = pId;

						// add neighbors
						for (int dx = -1; dx <= 1; dx++) {
							if (pX + dx < 0 || pX + dx >= sizeX)
								continue;
							for (int dy = -1; dy <= 1; dy++) {
								if (pY + dy < 0 || pY + dy >= sizeY)
									continue;
								for (int dz = -1; dz <= 1; dz++) {
									if (pZ + dz < 0 || pZ + dz >= sizeZ)
										continue;
									if (dx + dy + dz == 0)
										continue;
									if (TypeUtil.unsign(ccData[pZ + dz][0][(pX + dx) + (pY + dy) * sizeX]) == 0) {
										q.add(new Point5D.Double(pX + dx, pY + dy, pZ + dz, pT, pC));
										ql.add(pId);
									}
								}
							}
						}
					} else { // check labels
						int lpId = getParentLabel(pId, labelParents);
						int ppId = getParentLabel(lccData[pZ][0][pX + (pY) * sizeX], labelParents);
						if (lpId != ppId) {
							labelParents[lpId] = parentId;
							labelParents[ppId] = parentId;
							labelParents[parentId] = parentId;
							parentId++;
						}
					}

				}
			} catch (Exception e) {
				System.out.println(point);
				throw e;
			}
		}
		connectedComponents.dataChanged();

		for (int z = 0; z < sizeZ; z++) {
			for (int y = 0; y < sizeY; y++) {
				for (int x = 0; x < sizeX; x++) {
					lccData[z][0][x + y * sizeX] = getParentLabel(lccData[z][0][x + y * sizeX], labelParents);
				}
			}
		}
		labeledConnectedComponents.dataChanged();

		return connectedComponents;
	}

	private static int getParentLabel(int pId, int[] labelParents) {
		if (labelParents[pId] == pId)
			return pId;

		return getParentLabel(labelParents[pId], labelParents);
	}

}
