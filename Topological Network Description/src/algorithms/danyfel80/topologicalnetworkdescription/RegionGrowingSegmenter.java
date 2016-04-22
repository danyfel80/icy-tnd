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

	public static Sequence process(Sequence thresholdedSequence, List<ROI2DPoint> seeds) {
		thresholdedSequence = SequenceUtil.convertToType(thresholdedSequence, DataType.DOUBLE, false);
		int sizeX = thresholdedSequence.getSizeX();
		int sizeY = thresholdedSequence.getSizeY();
		int sizeZ = thresholdedSequence.getSizeZ();

		Sequence result = new Sequence(thresholdedSequence.getName() + "_Segmentation");
		result.beginUpdate();
		for (int z = 0; z < thresholdedSequence.getSizeZ(); z++) {
			IcyBufferedImage tempImage = new IcyBufferedImage(thresholdedSequence.getSizeX(), thresholdedSequence.getSizeY(),
			    1, DataType.UBYTE);
			result.setImage(0, z, tempImage);
		}
		result.endUpdate();

		Queue<Point5D> q = new LinkedList<Point5D>();
		for (ROI2DPoint roi2dPoint : seeds) {
			Point5D point = roi2dPoint.getPosition5D();
			q.add(point);
		}

		double[][][] thresholdedData = thresholdedSequence.getDataXYCZAsDouble(0);
		byte[][][] resultData = result.getDataXYCZAsByte(0);

		while (!q.isEmpty()) {
			Point5D point = q.poll();
			int pX = (int) point.getX();
			int pY = (int) point.getY();
			int pZ = (int) point.getZ();
			int pT = (int) point.getT();
			int pC = (int) point.getC();

			if (thresholdedData[pZ][0][pX + (pY) * sizeX] > 0 && TypeUtil.unsign(resultData[pZ][0][pX + (pY) * sizeX]) == 0) {
				resultData[pZ][0][pX + (pY) * sizeX] = (byte) DataType.UBYTE.getMaxValue();

				// <x-1, y, z>
				if (pX > 0 && TypeUtil.unsign(resultData[pZ][0][pX - 1 + pY * sizeX]) == 0)
					q.add(new Point5D.Double(pX - 1, pY, pZ, pT, pC));
				// <x, y-1, z>
				if (pY > 0 && TypeUtil.unsign(resultData[pZ][0][pX + (pY - 1) * sizeX]) == 0)
					q.add(new Point5D.Double(pX, pY - 1, pZ, pT, pC));
				// <x+1, y, z>
				if (pX < sizeX - 1 && TypeUtil.unsign(resultData[pZ][0][pX + 1 + pY * sizeX]) == 0)
					q.add(new Point5D.Double(pX + 1, pY, pZ, pT, pC));
				// <x, y+1, z>
				if (pY < sizeY - 1 && TypeUtil.unsign(resultData[pZ][0][pX + (pY + 1) * sizeX]) == 0)
					q.add(new Point5D.Double(pX, pY + 1, pZ, pT, pC));
				// <x, y, z-1>
				if (pZ > 0 && TypeUtil.unsign(resultData[pZ - 1][0][pX + pY * sizeX]) == 0)
					q.add(new Point5D.Double(pX, pY, pZ - 1, pT, pC));
				if (pZ < sizeZ - 1 && TypeUtil.unsign(resultData[pZ + 1][0][pX + pX * sizeX]) == 0)
					q.add(new Point5D.Double(pX, pY, pZ + 1, pT, pC));
			}
		}
		result.dataChanged();

		return result;
	}

}
