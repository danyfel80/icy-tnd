package plugins.danyfel80.topologicalnetworkdescription.classes;

import icy.image.IcyBufferedImage;
import icy.sequence.Sequence;
import icy.type.DataType;

/**
 * @author Daniel Felipe Gonzalez Obando
 * This class creates a distance map of a segmented image. the segmented image type must be SHORT.
 */
/**
 * @author Daniel
 *
 */
public class SegmentDistanceCalcultator {

	private Sequence segmentedSequence;
	private Sequence distanceMapXYZ;
	private Sequence squaredDistanceMap;
	private Sequence invertedSquaredDistanceMap;

	public SegmentDistanceCalcultator(Sequence segmentedSequence) {
		this.segmentedSequence = segmentedSequence;
		this.squaredDistanceMap = null;
		this.distanceMapXYZ = null;
		this.invertedSquaredDistanceMap = null;
	}



	/**
	 * @return the distanceMapXYZ
	 */
	public Sequence getDistanceMapXYZ() {
		return distanceMapXYZ;
	}



	/**
	 * @return the distanceMap
	 */
	public Sequence getSquaredDistanceMap() {
		return squaredDistanceMap;
	}



	public Sequence process() {
		distanceMapXYZ = new Sequence(segmentedSequence.getName() + "_DistanceMapXYZ");
		squaredDistanceMap = new Sequence(segmentedSequence.getName() + "_SquaredDistanceMap");

		distanceMapXYZ.beginUpdate();
		squaredDistanceMap.beginUpdate();
		try {
			int[][] prevDMapXYZSliceData = null;
			int sizeX = segmentedSequence.getSizeX();
			int sizeY = segmentedSequence.getSizeY();
			int sizeZ = segmentedSequence.getSizeZ();

			int maxDistVal = Math.max(sizeX, Math.max(sizeY, sizeZ));

			// First Pass
			for (int z = 0; z < sizeZ; z++) {

				short[][] segSliceData = segmentedSequence.getDataXYCAsShort(0, z);		

				IcyBufferedImage dMapXYZSlice = new IcyBufferedImage(sizeX, sizeY, 3, DataType.INT);
				int[][] dMapXYZSliceData=(int[][]) dMapXYZSlice.getDataXYC();
				IcyBufferedImage dMapSlice = new IcyBufferedImage(sizeX, sizeY, 1, DataType.INT);
				int[][] dMapSliceData=(int[][]) dMapSlice.getDataXYC();

				// Calculate minimum distance
				for (int x = 0; x < sizeX; x++) {
					for (int y = 0; y < sizeY; y++) {

						// Initialize distance map slice
						if (segSliceData[0][x + y*sizeX] != 0) {
							dMapXYZSliceData[0][x + y*sizeX] = maxDistVal;
							dMapXYZSliceData[1][x + y*sizeX] = maxDistVal;
							dMapXYZSliceData[2][x + y*sizeX] = maxDistVal;
						}

						int bestDistX = dMapXYZSliceData[0][x + y*sizeX];
						int bestDistY = dMapXYZSliceData[1][x + y*sizeX];
						int bestDistZ = dMapXYZSliceData[2][x + y*sizeX];
						int bestVal = bestDistX*bestDistX + bestDistY*bestDistY + bestDistZ*bestDistZ;

						// Z-1
						if (z > 0) {
							for (int i = -1; i < 2; i++) {
								for (int j = -1; j < 2; j++) {
									if (x+i >= 0 && x+i < sizeX && y+j >= 0 && y+j < sizeY) {

										int distX = prevDMapXYZSliceData[0][x+i + (y+j)*sizeX];
										int distY = prevDMapXYZSliceData[1][x+i + (y+j)*sizeX];
										int distZ = prevDMapXYZSliceData[2][x+i + (y+j)*sizeX];
										distX += Math.abs(i);
										distY += Math.abs(j);
										distZ += 1;
										int val = distX*distX + distY*distY + distZ*distZ; 

										if (val < bestVal) {
											bestDistX = distX;
											bestDistY = distY;
											bestDistZ = distZ;
											bestVal = val;
										}
									}
								}
							}
						}
						// Z
						for (int i = -1; i < 1; i++) {
							for (int j = -1; (i==-1? (j < 2): (j < 0)); j++) {
								if (x+i >= 0 && x+i < sizeX && y+j >= 0 && y+j < sizeY) {
									int distX = dMapXYZSliceData[0][x+i + (y+j)*sizeX];
									int distY = dMapXYZSliceData[1][x+i + (y+j)*sizeX];
									int distZ = dMapXYZSliceData[2][x+i + (y+j)*sizeX];
									distX += Math.abs(i);
									distY += Math.abs(j);

									int val = distX*distX + distY*distY + distZ*distZ; 

									if (val < bestVal) {
										bestDistX = distX;
										bestDistY = distY;
										bestDistZ = distZ;
										bestVal = val;
									}
								}
							}
						}

						dMapXYZSliceData[0][x + y*sizeX] = bestDistX;
						dMapXYZSliceData[1][x + y*sizeX] = bestDistY;
						dMapXYZSliceData[2][x + y*sizeX] = bestDistZ;

						dMapSliceData[0][x + y*sizeX] = bestVal;
					}
				}
				dMapXYZSlice.dataChanged();
				distanceMapXYZ.setImage(0, z, dMapXYZSlice);
				prevDMapXYZSliceData = dMapXYZSliceData;

				dMapSlice.dataChanged();
				squaredDistanceMap.setImage(0, z, dMapSlice);
			}

			// Second Pass
			prevDMapXYZSliceData = null;
			for (int z = sizeZ-1; z >= 0; z--) {
				int[][] dMapXYZSliceData = distanceMapXYZ.getDataXYCAsInt(0, z);
				int[][] dMapSliceData = squaredDistanceMap.getDataXYCAsInt(0, z);

				// Calculate minimum distance
				for (int x = sizeX-1; x >= 0; x--) {
					for (int y = sizeY-1; y >= 0; y--) {
						int bestDistX = dMapXYZSliceData[0][x + y*sizeX];
						int bestDistY = dMapXYZSliceData[1][x + y*sizeX];
						int bestDistZ = dMapXYZSliceData[2][x + y*sizeX];
						int bestVal = bestDistX*bestDistX + bestDistY*bestDistY + bestDistZ*bestDistZ;

						// Z+1
						if (z < sizeZ-1) {
							for (int i = 1; i >= -1; i--) {
								for (int j = 1; j >= -1; j--) {
									if (x+i >= 0 && x+i < sizeX && y+j >= 0 && y+j < sizeY) {

										int distX = prevDMapXYZSliceData[0][x+i + (y+j)*sizeX];
										int distY = prevDMapXYZSliceData[1][x+i + (y+j)*sizeX];
										int distZ = prevDMapXYZSliceData[2][x+i + (y+j)*sizeX];
										distX += Math.abs(i);
										distY += Math.abs(j);
										distZ += 1;
										int val = distX*distX + distY*distY + distZ*distZ; 

										if (val < bestVal) {
											bestDistX = distX;
											bestDistY = distY;
											bestDistZ = distZ;
											bestVal = val;
										}
									}
								}
							}
						}
						// Z
						for (int i = 1; i >= 0; i--) {
							for (int j = 1; (i==1? (j >= -1): (j >= 1)); j--) {
								if (x+i >= 0 && x+i < sizeX && y+j >= 0 && y+j < sizeY) {
									int distX = dMapXYZSliceData[0][x+i + (y+j)*sizeX];
									int distY = dMapXYZSliceData[1][x+i + (y+j)*sizeX];
									int distZ = dMapXYZSliceData[2][x+i + (y+j)*sizeX];
									distX += Math.abs(i);
									distY += Math.abs(j);

									int val = distX*distX + distY*distY + distZ*distZ; 

									if (val < bestVal) {
										bestDistX = distX;
										bestDistY = distY;
										bestDistZ = distZ;
										bestVal = val;
									}
								}
							}
						}

						dMapXYZSliceData[0][x + y*sizeX] = bestDistX;
						dMapXYZSliceData[1][x + y*sizeX] = bestDistY;
						dMapXYZSliceData[2][x + y*sizeX] = bestDistZ;

						dMapSliceData[0][x + y*sizeX] = bestVal;
					}
				}
				
				
				prevDMapXYZSliceData = dMapXYZSliceData;

				
			}
			distanceMapXYZ.dataChanged();
			squaredDistanceMap.dataChanged();
		} finally {
			
			distanceMapXYZ.endUpdate();
			squaredDistanceMap.endUpdate();
		}

		return squaredDistanceMap;
	}



	public Sequence getInvertedSquaredDistanceMap() {
		if (squaredDistanceMap != null) {
			if (invertedSquaredDistanceMap == null) {
				invertedSquaredDistanceMap = new Sequence(segmentedSequence.getName() + "_InvertedSquaredDistanceMap");
				invertedSquaredDistanceMap.beginUpdate();
				int sizeX = squaredDistanceMap.getSizeX();
				int sizeY = squaredDistanceMap.getSizeY();
				int sizeZ = squaredDistanceMap.getSizeZ();
				try {
					for (int z = 0; z < sizeZ; z++) {
						IcyBufferedImage tmpImg = new IcyBufferedImage(sizeX, sizeY, 1, DataType.DOUBLE);
						invertedSquaredDistanceMap.setImage(0, z, tmpImg);
					}
				}
				finally {
					invertedSquaredDistanceMap.endUpdate();
				}
				
				invertedSquaredDistanceMap.beginUpdate();
				try {
					double[][][][] iSDMData = invertedSquaredDistanceMap.getDataXYCZTAsDouble();
					int[][][][] sDMData = squaredDistanceMap.getDataXYCZTAsInt();
					for(int z = 0; z < sizeZ; z++) {
						for (int xy = 0; xy < sizeX * sizeY; xy++) {
							if (sDMData[0][z][0][xy] > 0)
								iSDMData[0][z][0][xy] = 1.0/(new Integer(sDMData[0][z][0][xy]).doubleValue());
							else
								iSDMData[0][z][0][xy] = Double.POSITIVE_INFINITY;
						}
					}
				} finally {
					invertedSquaredDistanceMap.endUpdate();
				}
			}
		}
		return invertedSquaredDistanceMap;
	}

}
