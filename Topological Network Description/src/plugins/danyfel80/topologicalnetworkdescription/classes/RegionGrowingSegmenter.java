package plugins.danyfel80.topologicalnetworkdescription.classes;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import icy.image.IcyBufferedImage;
import icy.sequence.Sequence;
import icy.type.DataType;
import icy.type.point.Point5D;
import plugins.kernel.roi.roi2d.ROI2DPoint;

/**
 * @author Daniel Felipe Gonzalez Obando
 * This class allows to segment a binary image using region growing starting from a given set of seed points. 
 * The segmentation creates connected components that respect the predicate of having a value different than zero.  
 */
public class RegionGrowingSegmenter {

	public static Sequence process(Sequence thresholdedSequence, List<ROI2DPoint> seeds) {
		Sequence result = new Sequence(thresholdedSequence.getName() + "_Segmentation");
		result.beginUpdate();
		for (int i = 0; i < thresholdedSequence.getSizeZ(); i++) {
			IcyBufferedImage tempImage = new IcyBufferedImage(thresholdedSequence.getSizeX(),
					thresholdedSequence.getSizeY(), 1, DataType.SHORT);
			result.setImage(0, i, tempImage);
		}
		result.endUpdate();

		
		Queue<Point5D> q = new LinkedList<Point5D>();
		for (ROI2DPoint roi2dPoint : seeds) {
			Point5D point = roi2dPoint.getPosition5D();
			q.add(point);
		}
		
		short[][][] thresholdedData = thresholdedSequence.getDataXYCZAsShort(0);
		short[][][] resultData = result.getDataXYCZAsShort(0);
		int sizeX = thresholdedSequence.getSizeX();
		int sizeY = thresholdedSequence.getSizeY();
		int sizeZ = thresholdedSequence.getSizeZ();
		
		result.beginUpdate();
		while (!q.isEmpty()) {
			Point5D point = q.poll();
			if (thresholdedData[(int) point.getZ()][0][(int)point.getX() + ((int)point.getY()) * sizeX] > 0 &&
					resultData[(int) point.getZ()][0][(int)point.getX() + ((int)point.getY()) * sizeX] == 0) {
				resultData[(int) point.getZ()][0][(int)point.getX() + ((int)point.getY()) * sizeX] = (short)(result.getDataTypeMax() - 1);
				
				// <x-1, y, z>
				if ((int)point.getX() > 0 &&
						resultData[(int)point.getZ()][0][(int)point.getX()-1 + (int)point.getY()*sizeX] == 0)
					q.add(new Point5D.Double(point.getX()-1, point.getY(), point.getZ(), point.getT(), point.getC()));
				// <x, y-1, z>
				if ((int)point.getY() > 0  &&
						resultData[(int)point.getZ()][0][(int)point.getX() + ((int)point.getY()-1)*sizeX] == 0)
					q.add(new Point5D.Double(point.getX(), point.getY()-1, point.getZ(), point.getT(), point.getC()));
				// <x+1, y, z>
				if ((int)point.getX() < sizeX-1 &&
						resultData[(int)point.getZ()][0][(int)point.getX()+1 + (int)point.getY()*sizeX] == 0)
					q.add(new Point5D.Double(point.getX()+1, point.getY(), point.getZ(), point.getT(), point.getC()));
				// <x, y+1, z>
				if ((int)point.getY() < sizeY-1 &&
						resultData[(int)point.getZ()][0][(int)point.getX() + ((int)point.getY()+1)*sizeX] == 0)
					q.add(new Point5D.Double(point.getX(), point.getY()+1, point.getZ(), point.getT(), point.getC()));
				// <x, y, z-1>
				if ((int)point.getZ() > 0 &&
						resultData[(int)point.getZ()-1][0][(int)point.getX() + (int)point.getY()*sizeX] == 0)
					q.add(new Point5D.Double(point.getX(), point.getY(), point.getZ()-1, point.getT(), point.getC()));
				if ((int)point.getZ() < sizeZ-1 &&
						resultData[(int)point.getZ()+1][0][(int)point.getX() + (int)point.getX()*sizeX] == 0)
					q.add(new Point5D.Double(point.getX(), point.getY(), point.getZ()+1, point.getT(), point.getC()));
			}
		}
		result.dataChanged();
		result.endUpdate();

		return result;
	}

}
