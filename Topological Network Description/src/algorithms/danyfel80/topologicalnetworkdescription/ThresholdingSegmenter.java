package algorithms.danyfel80.topologicalnetworkdescription;

import icy.image.IcyBufferedImage;
import icy.sequence.Sequence;
import icy.sequence.SequenceUtil;
import icy.type.DataType;

/**
 * Simple image thresholder for images with a single channel.
 * 
 * @author Daniel Felipe Gonzalez Obando
 */
public class ThresholdingSegmenter {

	/**
	 * @param sequence
	 * @return
	 */
	public static Sequence process(Sequence sequence, double thresholdValue) {
		Sequence result = new Sequence(sequence.getName() + "_Threshold_" + thresholdValue);

		sequence = SequenceUtil.convertToType(sequence, DataType.DOUBLE, false);

		double[][][] sequenceData = sequence.getDataXYCZAsDouble(0);
		result.beginUpdate();
		try {
			for (int z = 0; z < sequence.getSizeZ(); z++) {
				IcyBufferedImage threshImage = new IcyBufferedImage(sequence.getSizeX(), sequence.getSizeY(), 1,
				    DataType.UBYTE);
				byte[][] threshData = threshImage.getDataXYCAsByte();
				for (int xy = 0; xy < sequenceData[z][0].length; xy++) {
					threshData[0][xy] = (byte) ((sequenceData[z][0][xy] <= thresholdValue) ? 0d : DataType.UBYTE_MAX_VALUE);
				}

				threshImage.dataChanged();
				result.setImage(0, z, threshImage);
			}

		} finally {
			result.endUpdate();
		}

		return result;
	}

}
