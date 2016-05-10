package algorithms.danyfel80.topologicalnetworkdescription;

import icy.image.IcyBufferedImage;
import icy.sequence.Sequence;
import icy.sequence.SequenceUtil;
import icy.type.DataType;

/**
 * @author Daniel Felipe Gonzalez Obando
 */
public class EndnessMapGenerator {

	public static Sequence process(Sequence squaredDistanceMapSequence, Sequence costFunctionToSeedSequence) {
		int sizeX = squaredDistanceMapSequence.getSizeX();
		int sizeY = squaredDistanceMapSequence.getSizeY();
		int sizeZ = squaredDistanceMapSequence.getSizeZ();
		Sequence result = new Sequence(squaredDistanceMapSequence.getName() + "_Endness");

		if (!squaredDistanceMapSequence.getDataType_().equals(DataType.INT))
			squaredDistanceMapSequence = SequenceUtil.convertToType(squaredDistanceMapSequence, DataType.INT, false);
		if (!costFunctionToSeedSequence.getDataType_().equals(DataType.DOUBLE))
			costFunctionToSeedSequence = SequenceUtil.convertToType(costFunctionToSeedSequence, DataType.DOUBLE, false);

		int[][][] distanceData = squaredDistanceMapSequence.getDataXYCZAsInt(0);
		double[][][] costData = costFunctionToSeedSequence.getDataXYCZAsDouble(0);

		result.beginUpdate();
		try {
			for (int z = 0; z < sizeZ; z++) {
				IcyBufferedImage tmpImage = new IcyBufferedImage(sizeX, sizeY, 1, DataType.DOUBLE);
				double[][] tmpImageData = tmpImage.getDataXYCAsDouble();
				for (int x = 0; x < sizeX; x++) {
					for (int y = 0; y < sizeY; y++) {
						if (distanceData[z][0][x + y * sizeX] > 0) {
							tmpImageData[0][x + y * sizeX] = costData[z][0][x + y * sizeX]
							    / (double) distanceData[z][0][x + y * sizeX];
						} else {
							tmpImageData[0][x + y * sizeX] = -1;
						}
					}
				}
				tmpImage.dataChanged();
				result.setImage(0, z, tmpImage);
			}
		} finally {
			result.endUpdate();
		}
		return result;
	}

}
