package plugins.danyfel80.topologicalnetworkdescription.classes;

import icy.image.IcyBufferedImage;
import icy.sequence.Sequence;
import icy.type.DataType;

/**
 * @author Daniel Felipe Gonzalez Obando
 * Simple image thresholder for images with a single channel.
 */
public class Thresholder {

	/**
	 * @param sequence
	 * @return
	 */
	public static Sequence process(Sequence sequence, Short thresholdValue) {
		Sequence result = new Sequence(sequence.getName() + "_Threshold_" + thresholdValue);
		short[][][] sequenceData = sequence.getDataXYCZAsShort(0);
		result.beginUpdate();
		try {
			for (int z = 0; z < sequence.getSizeZ(); z++) {
				IcyBufferedImage threshImage = new IcyBufferedImage(sequence.getSizeX(),
						sequence.getSizeY(), 1, DataType.SHORT);
				short[][] threshData = threshImage.getDataXYCAsShort();
				for (int xy = 0; xy < sequenceData[z][0].length; xy++) {
					threshData[0][xy] = (short)(sequenceData[z][0][xy] <= thresholdValue? 0: 1);
				}
				
				
				//IcyBufferedImage threshImage = getXYThreshold(sequence, result, thresholdValue, z);
				threshImage.dataChanged();
				result.setImage(0, z, threshImage);
			}
			
		} finally {
			result.endUpdate();
		}
		
		return result;
	}

//	private static IcyBufferedImage getXYThreshold(Sequence sequence, Sequence result, double thresholdValue, int z) {
//		IcyBufferedImage res = new IcyBufferedImage(sequence.getSizeX(),
//				sequence.getSizeY(), 1, DataType.SHORT);
//		double[] seqDoubleArray = Array1DUtil.arrayToDoubleArray(sequence.getDataXY(0, z, 0), sequence.isSignedDataType());
//		double[] resDoubleArray = Array1DUtil.arrayToDoubleArray(res.getDataXY(0), res.isSignedDataType());
//		for (int i = 0; i < seqDoubleArray.length; i++)
//				resDoubleArray[i] = (seqDoubleArray[i] <= thresholdValue? res.getDataTypeMin(): res.getDataTypeMax()-1);
//			Array1DUtil.doubleArrayToArray(resDoubleArray, res.getDataXY(0));
//		res.dataChanged();
//		return res;
//	}

}
