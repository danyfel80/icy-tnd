package plugins.vannary.shganalysis;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.vecmath.Point3d;
import javax.vecmath.Point3i;

import icy.common.exception.UnsupportedFormatException;
import icy.file.FileUtil;
import icy.file.Loader;
import icy.file.Saver;
import icy.gui.dialog.MessageDialog;
import icy.image.IcyBufferedImage;
import icy.image.IcyBufferedImageUtil;
import icy.sequence.Sequence;
import icy.type.DataType;
import icy.type.collection.array.Array1DUtil;
import jxl.Workbook;
import jxl.write.Label;
import jxl.write.Number;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;
import plugins.adufour.blocks.lang.Block;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.connectedcomponents.ConnectedComponent;
import plugins.adufour.connectedcomponents.ConnectedComponents;
import plugins.adufour.ezplug.EzGroup;
import plugins.adufour.ezplug.EzPlug;
import plugins.adufour.ezplug.EzVar;
import plugins.adufour.ezplug.EzVarBoolean;
import plugins.adufour.ezplug.EzVarDouble;
import plugins.adufour.ezplug.EzVarEnum;
import plugins.adufour.ezplug.EzVarInteger;
import plugins.adufour.ezplug.EzVarListener;
import plugins.adufour.ezplug.EzVarSequence;
import plugins.nherve.toolbox.image.BinaryIcyBufferedImage;
import plugins.nherve.toolbox.image.feature.fuzzy.HysteresisThresholder;
import plugins.vannary.morphomaths.MorphOp;

public class SHGAnalysis extends EzPlug implements Block {

	private EzVarSequence input;
	EzVarSequence varSequence;

	private EzVarSequence input2;

	// Method used
	EzVarEnum<MethodType> method;

	private EzVarDouble percentLowThreshold;
	private EzVarDouble percentHighThreshold;

	private EzVarInteger valueLowThreshold;
	private EzVarInteger valueHighThreshold;

	private EzVarBoolean inputShowResults = new EzVarBoolean("Show Result", true);

	private EzVarSequence outputSequence = new EzVarSequence("Result Image");

	@Override
	protected void initialize() {

		super.addEzComponent(input = new EzVarSequence("Input"));
		// varSequence = new EzVarSequence("Input sequence");

		super.addEzComponent(input2 = new EzVarSequence("Input Biopsie"));

		method = new EzVarEnum<MethodType>("Method:", MethodType.values());
		super.addEzComponent(method);

		/*
		 * percentLowThreshold = new EzVarInteger("Low threshold", 10, 0, 100, 1);
		 * percentHighThreshold = new EzVarInteger("High threshold", 90, 0, 100, 1);
		 */

		valueLowThreshold = new EzVarInteger("Low threshold", 400, 0, 65000, 1);
		valueHighThreshold = new EzVarInteger("High threshold", 120, 0, 65000, 1);

		EzGroup hysteresisVar = new EzGroup("Hysteresis parameters", valueLowThreshold, valueHighThreshold);
		super.addEzComponent(hysteresisVar);

		method.addVarChangeListener(new EzVarListener<MethodType>() {
			@Override
			public void variableChanged(EzVar<MethodType> source, MethodType newValue) {
				updateDefaultParams();
			}

		});
	}

	// Update the default parameters when the input sequence changes
	private void updateDefaultParams() {
		if (true) {
			/*
			 * if (input.getValue(false) != null) {
			 */
			MethodType _method = method.getValue();
			switch (_method) {
			case HYSTERESIS:
				valueLowThreshold.setValue(10);
				valueHighThreshold.setValue(90);
				break;
			case HYSTERESIS2:
				valueLowThreshold.setValue(10);
				valueHighThreshold.setValue(120);
				break;
			case QUANTIF:
				valueLowThreshold.setValue(5);
				valueHighThreshold.setValue(5);

				break;
			case FILL_Hole:
				valueHighThreshold.setValue(2000);
				break;
			case THIN:
				valueLowThreshold.setValue(2);
				break;
			case CROP:
				valueLowThreshold.setValue(512);
				break;
			default:
				break;
			}

			// }
		}
	}

	@Override
	protected void execute() {
		Sequence seqIn = input.getValue();

		Sequence seqBiopsy = input2.getValue();

		MethodType _method = method.getValue();
		MorphOp mOp = new MorphOp();

		/*
		 * try {
		 */
		switch (_method) {
		case HYSTERESIS:
			// doHysteresis(seqIn);
			Point3d pt = new Point3d();
			pt.x = 4;
			pt.y = 1;

			/*
			 * Point3d[] segment = new Point3d[2]; segment[0].x = 0.5; segment[0].y =
			 * 1; segment[1].x = 5; segment[1].y = 12;
			 */

			Point3d ptA = new Point3d();
			ptA.x = -2;
			ptA.y = 0;

			Point3d ptB = new Point3d();
			ptB.x = 1;
			ptB.y = 6;

			computeDistancePointfromSegment(pt, ptA, ptB);
			break;
		case HYSTERESIS2:
			doHysteresis2(seqIn);
			break;
		case QUANTIF:
			doQuantifBin(seqIn, seqBiopsy);
		case QUANTIF_Mean:
			doQuantifMean(seqIn, seqBiopsy);
			break;
		case FILL_Hole:
			int holeSize = valueHighThreshold.getValue();
			Sequence seqSignal = fill_holeregion_by_size(seqIn, holeSize);

			outputSequence.setValue(seqSignal);
			if (inputShowResults.getValue()) {
				addSequence(seqSignal);
			}

			break;
		case THIN:

			mOp.thinning(input.getValue(), valueLowThreshold.getValue());
			break;
		case CROP:
			doCrop(input.getValue(), valueLowThreshold.getValue());
			break;
		default:
			break;
		}

		/*
		 * } catch (UnhandledImageTypeException e) { String message =
		 * "Unsupported image type"; EzMessage.message(message, MessageType.ERROR,
		 * OutputType.DIALOG); }
		 */

	}

	// remove small regions
	private Sequence remove_by_size(Sequence seqIn, int size) {
		int w = seqIn.getWidth();
		int h = seqIn.getHeight();

		int minVolume = 1;
		int maxVolume = w * h;
		IcyBufferedImage img = seqIn.getFirstImage();
		double[] tabIn = Array1DUtil.arrayToDoubleArray(img.getDataXY(0), img.isSignedDataType());
		List<ConnectedComponent> ccs = ConnectedComponents.extractConnectedComponents(seqIn, minVolume, maxVolume, null)
		    .get(0);
		Point3i[] pts = null;
		int id = 0;
		for (ConnectedComponent cc : ccs) {
			if (cc.getSize() <= size) {
				pts = cc.getPoints();
				for (int i = 0; i < pts.length; i++) {// CC

					Point3i p = pts[i];

					tabIn[p.x + w * p.y] = 0; // suppress little regions
					p = null;
				}
			}
			id++;
		}
		IcyBufferedImage imgOut = new IcyBufferedImage(w, h, 1, DataType.DOUBLE);
		imgOut.setDataXY(0, Array1DUtil.doubleArrayToArray(tabIn, imgOut.getDataXY(0)));
		Sequence seqOut = new Sequence();
		seqOut.setName("Remove" + size);
		seqOut.addImage(imgOut);
		// addSequence(seqOut);
		return (seqOut);

	}

	// Fill small Hole
	// TODO:
	private Sequence fill_holeregion_by_size(Sequence seqIn, int sizeFill) {
		int w = seqIn.getWidth();
		int h = seqIn.getHeight();
		BinaryIcyBufferedImage bin = new BinaryIcyBufferedImage(w, h);
		int bt = 0;

		/*
		 * SomeImageTools.binarize(input2.getValue().getFirstImage(), bin, 0, 0, 1,
		 * bt); bin.invert(); List<My2DConnectedComponent> keep =
		 * SomeImageTools.findConnectedComponents(bin); System.out.println(
		 * " Nb cc :"+keep.size());
		 * 
		 * Sequence seqBin = new Sequence(); seqBin.addImage(bin);
		 * addSequence(seqBin);
		 */

		int minVolume = 1;
		int maxVolume = w * h;
		IcyBufferedImage img = seqIn.getFirstImage();
		notIcyBufferedImage(img);
		double[] tabIn = Array1DUtil.arrayToDoubleArray(img.getDataXY(0), img.isSignedDataType());
		List<ConnectedComponent> ccs = ConnectedComponents.extractConnectedComponents(seqIn, minVolume, maxVolume, null)
		    .get(0);
		Point3i[] pts = null;
		int id = 0;
		for (ConnectedComponent cc : ccs) {
			if (cc.getSize() < sizeFill) {
				pts = cc.getPoints();
				for (int i = 0; i < pts.length; i++) {// CC

					Point3i p = pts[i];

					tabIn[p.x + w * p.y] = 0; // suppress little regions
					p = null;
				}
			}
			id++;
		}

		IcyBufferedImage imgOut = new IcyBufferedImage(w, h, 1, DataType.DOUBLE);
		imgOut.setDataXY(0, Array1DUtil.doubleArrayToArray(tabIn, imgOut.getDataXY(0)));
		notIcyBufferedImage(imgOut);
		Sequence seqOut = new Sequence();
		seqOut.setName("FillHole");
		seqOut.addImage(imgOut);
		addSequence(seqOut);
		return (seqOut);
	}

	// TODO A REVOIR
	private IcyBufferedImage readCortex(Sequence seqBiopsy) {
		Sequence seqCortex = null;
		Sequence biopsieSeq = null;
		Sequence seqCaps = null;

		String filename = seqBiopsy.getFilename();
		;
		File inputSequenceFile = new File(filename);
		String currentdir = inputSequenceFile.getAbsolutePath();
		System.out.println(currentdir);

		currentdir = currentdir.substring(0, currentdir.length() - (seqBiopsy.getName().length() + 4));
		System.out.println("dir..." + currentdir);

		String tif = ".tif";
		String biopsy = "Biopsie";
		String nameTM = seqBiopsy.getName();
		nameTM = nameTM.substring(0, nameTM.length() - biopsy.length());
		nameTM = nameTM + tif;

		System.out.println("nameTM..." + nameTM);

		File mainDir = new File(currentdir);

		// FileFilter filter=new FileFilter(".tif");
		List<File> listImages = Arrays.asList(mainDir.listFiles());
		Collections.sort(listImages);

		String capsName = null;
		for (File file : listImages) {
			if (!file.getAbsolutePath().endsWith(".tif")) {
				System.out.println("file: " + file);

			} else {
				filename = file.getName();
				// System.out.println("FICHIER : " + file );

				capsName = currentdir + nameTM + ".Caps.tif";
			}

		}

		System.out.println("capsName... " + capsName);

		IcyBufferedImage imgCaps = null;
		IcyBufferedImage imgCortex = null;

		if (new File(capsName).exists()) {
			imgCaps = loadIcyBufferedImage(capsName);
			imgCaps = notIcyBufferedImage(imgCaps);
			imgCortex = andIcyBufferedImage(seqBiopsy.getFirstImage(), imgCaps);
			// seqCortex=sequencesAnd(biopsieSeq, seqCaps);
		} else
			imgCortex = seqBiopsy.getFirstImage();
		// }
		// seqCortex.setName("Cortex");

		/*
		 * seqCaps.addImage(imgCortex); addSequence(seqCaps); seqCaps=null;
		 */
		return (imgCortex);
	}

	Sequence loadSequence(String nameFile) {
		Sequence sequence = new Sequence();

		IcyBufferedImage img = null;
		try {
			img = Loader.loadImage(nameFile);
		} catch (UnsupportedFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		sequence.addImage(img);
		return (sequence);
	}

	IcyBufferedImage loadIcyBufferedImage(String nameFile) {
		IcyBufferedImage img = null;
		try {
			img = Loader.loadImage(nameFile);
		} catch (UnsupportedFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return (img);
	}

	// TODO verfier des type suppose image binaire
	IcyBufferedImage andIcyBufferedImage(IcyBufferedImage img, IcyBufferedImage img2) {

		int w = img.getWidth();
		int h = img.getHeight();

		double[] tabInDouble = Array1DUtil.arrayToDoubleArray(img.getDataXY(0), img.isSignedDataType());
		double[] tabInDouble2 = Array1DUtil.arrayToDoubleArray(img2.getDataXY(0), img2.isSignedDataType());
		double[] tabOutDouble = Arrays.copyOf(tabInDouble, tabInDouble.length);

		for (int x = 0; x < w; ++x)
			for (int y = 0; y < h; ++y) {
				double val = tabInDouble[x + y * w];
				double val2 = tabInDouble2[x + y * w];
				if (val > 0 && val2 > 0)
					tabOutDouble[x + y * w] = val;
				else
					tabOutDouble[x + y * w] = 0;
			}

		img.setDataXY(0, Array1DUtil.doubleArrayToArray(tabOutDouble, img.getDataXY(0)));

		return (img);
	}

	IcyBufferedImage notIcyBufferedImage(IcyBufferedImage img) {

		int w = img.getWidth();
		int h = img.getHeight();

		double[] tabInDouble = Array1DUtil.arrayToDoubleArray(img.getDataXY(0), img.isSignedDataType());
		double[] minmax = img.getChannelTypeBounds(0);
		double min = minmax[0], max = minmax[1];

		for (int x = 0; x < w; ++x)
			for (int y = 0; y < h; ++y) {
				tabInDouble[x + w * y] = max - tabInDouble[x + w * y];
			}

		img.setDataXY(0, Array1DUtil.doubleArrayToArray(tabInDouble, img.getDataXY(0)));

		return (img);
	}

	private void doQuantifBin(Sequence seqIn, Sequence seqBiopsy) {

		int w = seqIn.getWidth();
		int h = seqIn.getHeight();
		// IcyBufferedImage imaBiopsy=seqBiopsy.getFirstImage();

		// Cortex
		IcyBufferedImage imgCortex = readCortex(seqBiopsy);
		IcyBufferedImage imaIn = seqIn.getFirstImage();
		IcyBufferedImage imgSignal = andIcyBufferedImage(imaIn, imgCortex);

		Sequence seqSignal = new Sequence();
		seqSignal.addImage(imgSignal);

		int holeSize = valueHighThreshold.getValue();
		int smallRemoveSize = valueLowThreshold.getValue();
		Sequence seqtmp = remove_by_size(seqSignal, smallRemoveSize);
		seqSignal = fill_holeregion_by_size(seqtmp, holeSize);

		// addSequence(seqSignal);

		imaIn = seqSignal.getFirstImage();
		double[] tabInDouble = Array1DUtil.arrayToDoubleArray(imaIn.getDataXY(0), imaIn.isSignedDataType());

		double[] tabCortexDouble = Array1DUtil.arrayToDoubleArray(imgCortex.getDataXY(0), imgCortex.isSignedDataType());

		int nbsignal = 0;
		int sizeBiopsy = 0;

		for (int x = 0; x < w; x++)
			for (int y = 0; y < h; y++) {
				double valb = tabCortexDouble[x + y * w];
				if (valb != 0) {
					double vals = tabInDouble[x + y * w];
					if (vals > 0)
						nbsignal++;
					sizeBiopsy++;
				}
			}

		System.out.println(" spixels: " + nbsignal + " imagesize:" + w * h + " percent:" + (double) nbsignal / (w * h)
		    + " Size biopsy: " + sizeBiopsy + " percent:" + (double) nbsignal / sizeBiopsy);
		saveResult2Excel(seqIn, sizeBiopsy, nbsignal, smallRemoveSize, holeSize);
		MessageDialog.showDialog("SHG Analysis is working fine !");
	}

	// Average
	private void doQuantifMean(Sequence seqIn, Sequence seqBiopsy) {

		int w = seqIn.getWidth();
		int h = seqIn.getHeight();
		// IcyBufferedImage imaBiopsy=seqBiopsy.getFirstImage();

		// Cortex
		IcyBufferedImage imgCortex = readCortex(seqBiopsy);
		IcyBufferedImage imaIn = seqIn.getFirstImage();
		IcyBufferedImage imgSignal = andIcyBufferedImage(imaIn, imgCortex);

		Sequence seqSignal = new Sequence();
		seqSignal.addImage(imgSignal);

		int holeSize = valueHighThreshold.getValue();
		int smallRemoveSize = valueLowThreshold.getValue();
		Sequence seqtmp = remove_by_size(seqSignal, smallRemoveSize);
		seqSignal = fill_holeregion_by_size(seqtmp, holeSize);

		// addSequence(seqSignal);

		imaIn = seqSignal.getFirstImage();
		double[] tabInDouble = Array1DUtil.arrayToDoubleArray(imaIn.getDataXY(0), imaIn.isSignedDataType());

		double[] tabCortexDouble = Array1DUtil.arrayToDoubleArray(imgCortex.getDataXY(0), imgCortex.isSignedDataType());

		double signal = 0;
		int sizeBiopsy = 0;
		double value = 0;
		for (int x = 0; x < w; x++)
			for (int y = 0; y < h; y++) {
				double valb = tabCortexDouble[x + y * w];
				if (valb != 0) {
					signal += tabInDouble[x + y * w];

					sizeBiopsy++;
				}
			}

		System.out.println(" spixels: " + sizeBiopsy + " imagesize:" + w * h + " percent:" + (double) signal / (w * h)
		    + " percent:" + (double) signal / sizeBiopsy);
		saveResult2Excel(seqIn, sizeBiopsy, signal, smallRemoveSize, holeSize);
		MessageDialog.showDialog("SHG Analysis is working fine !");
	}

	private void doHysteresis(Sequence seqIn) {

		IcyBufferedImage imaIn = seqIn.getFirstImage();
		String filename = seqIn.getName();

		double[] tabInDouble = Array1DUtil.arrayToDoubleArray(imaIn.getDataXY(0), imaIn.isSignedDataType());
		int w = imaIn.getWidth();
		int h = imaIn.getHeight();

		int binPrecision = 256; // Short.MAX_VALUE*2;

		switch (imaIn.getDataType_()) {
		case UBYTE:
			binPrecision = (Byte.MAX_VALUE + 1) * 2;
			System.out.println(" Byte max: " + binPrecision);
			break;
		case USHORT:
			binPrecision = Short.MAX_VALUE * 2;
			System.out.println(" Short max: " + binPrecision);
			break;
		case INT:
			binPrecision = Integer.MAX_VALUE;
			break;
		default:
			break;
		}

		double[] histo = new double[binPrecision];
		double[] minmax = seqIn.getChannelTypeBounds(0);
		double min = minmax[0], max = minmax[1];
		double fact = (binPrecision - 1) / (max - min);

		for (double d : tabInDouble)
			histo[(int) ((d - min) * fact)]++;

		int nbPixels = w * h - (int) histo[0];
		int nbPixelsAll = w * h;
		double valcum = 0;
		double percent = 0.1;

		percent = valueLowThreshold.getValue() / 100.; //
		double percentHigh = valueHighThreshold.getValue() / 100.;
		// sans le 0
		int i = 0;
		while (valcum < nbPixels * percent) {
			valcum += histo[i++];
			// System.out.println(" mini: "+i+ " valcum:"+valcum/nbPixels);
		}
		min = i;
		double hl = (double) min / (double) binPrecision;
		// i=255;
		// valcum = 0;
		percent = 0.05;
		while (valcum < nbPixels * percentHigh) {
			valcum += histo[i++];
			// System.out.println(" max: "+i+ " valcum:"+valcum/nbPixels);
		}
		max = i;
		double hh = (double) max / (double) binPrecision;
		System.out.println(" min: " + min + " max:" + max);

		/*
		 * double hl = valueLowThreshold.getValue()/256.; // suppose que lq valeur
		 * est [0-255] double hh = valueHighThreshold.getValue()/256.;
		 * 
		 */

		IcyBufferedImage ima = IcyBufferedImageUtil.convertToType(seqIn.getFirstImage(), DataType.DOUBLE, true);

		IcyBufferedImage imaHyst = hysteresisThreshold(ima, hl, hh);

		imaHyst = IcyBufferedImageUtil.convertToType(imaHyst, DataType.UBYTE, true);
		Sequence seqOut = new Sequence();
		seqOut.setImage(0, 0, imaHyst);
		seqOut.setName(filename + "Hystlow" + min + "_High" + max);
		outputSequence.setValue(seqOut);
		if (inputShowResults.getValue()) {
			addSequence(seqOut);
		}

	}

	private void doHysteresis2(Sequence seqIn) {

		String filename = seqIn.getName();

		double[] minmax = seqIn.getChannelBounds(0);
		double minv = minmax[0], maxv = minmax[1];
		System.out.println(" min:" + minv + " max:" + maxv);

		int binPrecision = 256;// Short.MAX_VALUE*2;

		IcyBufferedImage imaIn = seqIn.getFirstImage();
		switch (imaIn.getDataType_()) {
		case UBYTE:
			binPrecision = (Byte.MAX_VALUE + 1) * 2;
			System.out.println(" Byte max: " + binPrecision);
			break;
		case USHORT:
			binPrecision = Short.MAX_VALUE * 2;
			System.out.println(" Short max: " + binPrecision);
			break;
		case INT:
			binPrecision = Integer.MAX_VALUE;
			break;
		default:
			break;
		}

		double min = valueLowThreshold.getValue();
		double max = valueHighThreshold.getValue();

		double hl = min / binPrecision; // suppose que lq valeur est [0-255]
		double hh = max / binPrecision;
		System.out.println("Hystlow: " + hl + "_High: " + hh);

		int t = 0;
		Sequence seqOut = new Sequence();
		for (int z = 0; z < seqIn.getSizeZ(); z++) {
			IcyBufferedImage ima = IcyBufferedImageUtil.convertToType(seqIn.getImage(t, z), DataType.DOUBLE, true);
			IcyBufferedImage imaHyst = hysteresisThreshold(ima, hl, hh);

			imaHyst = IcyBufferedImageUtil.convertToType(imaHyst, DataType.UBYTE, true);
			seqOut.setImage(t, z, imaHyst);
		}

		seqOut.setName(filename + "Hystlow" + min + "_High" + max);

		outputSequence.setValue(seqOut);
		if (inputShowResults.getValue()) {
			addSequence(seqOut);
		}
		// MessageDialog.showDialog("SHG Analysis is working fine !");
	}

	// do crop vignette
	private void doCrop(Sequence seqIn, int sizeCrop) {

		int w = seqIn.getSizeX();
		int h = seqIn.getSizeY();
		int wcrop = sizeCrop;
		int hcrop = wcrop;

		IcyBufferedImage buf = seqIn.getFirstImage();

		String s = seqIn.getName();
		String currentDirectory = FileUtil.getDirectory(seqIn.getFilename());
		// File newfolder = new File(currentDirectory + "/CROP"+s.substring(0,
		// s.indexOf("(")) ); // nom issu de avec parenthese?
		File newfolder = new File(currentDirectory + "/CROP" + s);

		newfolder.mkdir();
		int inc = 1;

		for (int y = 0; y < h; y = y + hcrop)
			for (int x = 0; x < w; x = x + wcrop) {

				IcyBufferedImage crop = IcyBufferedImageUtil.getSubImage(buf, x, y, wcrop, hcrop);
				Sequence seqcrop = new Sequence();
				seqcrop.addImage(crop);

				// String nomImage = newfolder+ "/" + s.substring(0, s.indexOf("(")) +
				// "v" + inc+".tif";
				String nomImage = newfolder + "/" + s + "v" + inc + ".tif";
				System.out.println(inc + nomImage + " x:" + x + " y:" + y);
				Saver.save(seqcrop, new File(nomImage), false);
				inc++;
			}

	}

	// do create mosaic from vignettes
	private void doImageFromCrops(Sequence seqIn, int sizeCrop) {

		int w = seqIn.getSizeX();
		int h = seqIn.getSizeY();
		int wcrop = sizeCrop;
		int hcrop = wcrop;

		IcyBufferedImage buf = seqIn.getFirstImage();

		String s = seqIn.getName();
		String currentDirectory = FileUtil.getDirectory(seqIn.getFilename());
		File newfolder = new File(currentDirectory + "/CROP" + s.substring(0, s.indexOf("(")));
		newfolder.mkdir();
		int inc = 1;

		for (int y = 0; y < h; y = y + hcrop)
			for (int x = 0; x < w; x = x + wcrop) {

			}

	}

	// Enum containing the methods that can be called in this plug in
	private enum MethodType {
		HYSTERESIS("Hysteresis percentage"), HYSTERESIS2("Hysteresis intensity"), QUANTIF("Quantif"), QUANTIF_Mean(
		    "Quantif Mean"), FILL_Hole("Fill hole"), THIN(" thin"), CROP(" crop");

		private final String name;

		private MethodType(String string) {
			this.name = string;
		}

		public String toString() {
			return this.name;
		}
	}

	private void saveResult2Excel(Sequence seq, int sizeBiopsy, double nbsignal, int sizeRemoveSmall, int holeSize) {

		int w = seq.getWidth();

		File xlsFile;

		String fileName = seq.getFilename();
		File inputSequenceFile = new File(fileName);
		String file = inputSequenceFile.getAbsolutePath();
		file = file.substring(0, file.length() - inputSequenceFile.getName().length());
		String folder = file;
		File dirResult = new File(folder, FileUtil.separator + "SAVE");
		if (!dirResult.exists())
			dirResult.mkdir();

		String filename = inputSequenceFile.getName();
		String nameFile = folder + "SAVE" + File.separator + filename + "_" + sizeRemoveSmall + "_" + holeSize + ".xls";
		System.out.println("nom fichier: " + nameFile);

		try {
			WritableWorkbook pageresultat;
			pageresultat = Workbook.createWorkbook(new File(nameFile));

			WritableSheet page = pageresultat.createSheet("RESULT ", 0);

			int col = 0;
			Label label = new Label(col, 0, "Nom:");
			page.addCell(label);
			label = new Label(col + 1, 0, "Biopsy size:");
			page.addCell(label);
			label = new Label(col + 2, 0, "Fibrosis:");
			page.addCell(label);
			label = new Label(col + 3, 0, "% Fibrosis :");
			page.addCell(label);
			label = new Label(col + 4, 0, "sizeRemoveSmall :");
			page.addCell(label);
			label = new Label(col + 5, 0, "holeSize :");
			page.addCell(label);

			label = new Label(col, 1, filename);
			page.addCell(label);

			Number number = new Number(col + 1, 1, sizeBiopsy);
			page.addCell(number);
			number = new Number(col + 2, 1, nbsignal);
			page.addCell(number);
			number = new Number(col + 3, 1, (double) nbsignal / (double) sizeBiopsy);
			page.addCell(number);
			number = new Number(col + 4, 1, sizeRemoveSmall);
			page.addCell(number);
			number = new Number(col + 5, 1, holeSize);
			page.addCell(number);

			pageresultat.write();
			pageresultat.close();

		} catch (IOException e) {
			e.printStackTrace();
		} catch (WriteException we) {
			we.printStackTrace();
		}

		/*
		 * int ik=0; for (int i=0; i<nbCells; i++) {
		 * 
		 * // here 0 is the number of the page. WritableSheet page =
		 * pageresultat.createSheet("cell "+(i+1), i+1 );
		 * 
		 * int col=0; Label label = new Label( col , 0 , "Nom:" );
		 * page.addCell(label); label = new Label( col+1 , 0 , "Biopsy size:" );
		 * page.addCell(label); label = new Label( col+2 , 0 , "Fibrosis:" );
		 * page.addCell(label); label = new Label( col+3 , 0 , "Fibrosis:" );
		 * page.addCell(label); label = new Label( col+4 , 0 , " :" );
		 * page.addCell(label); int li=1; // indice line int nbSpotsC =
		 * cNbSpots.get(i); double sIntensity=0.; int nbCellLim = ik+nbSpotsC; int
		 * sizeSpotbyCell = 0; // taille des spots dans une cellule for (int k=ik
		 * ;k<nbCellLim; k++) { Spot obj =null; // if (k<clistSpot.size()) //{ obj =
		 * (Spot) clistSpot.get(k); int x = (int) Math.rint(obj.mass_center.x); int
		 * y = (int) Math.rint(obj.mass_center.y); int z = (int)
		 * Math.rint(obj.mass_center.z);
		 * 
		 * col=0; ik++; Number number = new Number( col , li , x );
		 * page.addCell(number); number = new Number( col+1 , li , y );
		 * page.addCell(number); number = new Number( col+2 , li ,
		 * obj.point3DList.size() ); page.addCell(number); number = new Number(
		 * col+3 , li , obj.meanIntensity ); page.addCell(number); number = new
		 * Number( col+4 , li , obj.meanIntensity*obj.point3DList.size() );
		 * page.addCell(number); sIntensity +=
		 * obj.meanIntensity*obj.point3DList.size();
		 * 
		 * sizeSpotbyCell+=obj.point3DList.size(); // } li++;
		 * 
		 * } cIntensitySpot.add(sIntensity);
		 * cmeanSizeSpots.add((double)sizeSpotbyCell/nbSpotsC);
		 * 
		 * }
		 * 
		 * 
		 * 
		 * int li =1; for (int k=0 ;k<nbCells; k++) {//TODO limit col=0; Number
		 * number = new Number( col , li , (k+1) ); page.addCell(number); col++;
		 * 
		 * 
		 * li++;
		 * 
		 * } pageresultat.write(); pageresultat.close();
		 * 
		 * 
		 * } catch(IOException e){ e.printStackTrace(); } catch(WriteException we){
		 * we.printStackTrace(); }
		 */

	}

	@Override
	public void clean() {
		// TODO Auto-generated by Icy4Eclipse
	}

	public static IcyBufferedImage hysteresisThreshold(IcyBufferedImage img, double hl, double hh) {

		HysteresisThresholder hta = new HysteresisThresholder(hh, hl);
		IcyBufferedImage hyst = hta.work(img);

		return hyst;
	}

	// distance from a point to line segment AB, H : projection point
	public double computeDistancePointfromSegment(Point3d pt, Point3d ptA, Point3d ptB) {
		double res = 0;
		double prodScalaire = 0;

		prodScalaire = (pt.x - ptA.x) * (ptB.x - ptA.x) + (pt.y - ptA.y) * (ptB.y - ptA.y);
		double distSegABcarre = distanceCarre(ptA, ptB);
		double distPointExtAMcarre = distanceCarre(ptA, pt);
		double distAHcarre = prodScalaire * prodScalaire / distSegABcarre;

		res = Math.sqrt(distPointExtAMcarre - distAHcarre);

		/*
		 * Point3d ptH = new Point3d(); ptH.x = ptA.x +
		 * distAHcarre/prodScalaire*(ptB.x-ptA.x); ptH.y = ptA.y +
		 * distAHcarre/prodScalaire*(ptB.y-ptA.y);
		 */

		return res;
	}

	private double distanceCarre(Point3d pt1, Point3d pt2) {
		double val = 0;
		double dx = pt1.x - pt2.x;
		double dy = pt1.y - pt2.y;
		double dz = pt1.z - pt2.z;
		val = (dx * dx + dy * dy + dz * dz);
		return (val);
	}

	@Override
	public void declareInput(VarList inputMap) {
		// TODO Auto-generated method stub
		input = new EzVarSequence("Input");
		input2 = new EzVarSequence("Input Biopsie");
		method = new EzVarEnum<MethodType>("Method:", MethodType.values());
		valueLowThreshold = new EzVarInteger("Low threshold", 400, 0, 65000, 1);
		valueHighThreshold = new EzVarInteger("High threshold", 120, 0, 65000, 1);
		method.addVarChangeListener(new EzVarListener<MethodType>() {
			@Override
			public void variableChanged(EzVar<MethodType> source, MethodType newValue) {
				updateDefaultParams();
			}

		});
		inputMap.add(input.name, input.getVariable());
		inputMap.add(input2.name, input2.getVariable());
		inputMap.add(method.name, method.getVariable());
		inputMap.add(valueLowThreshold.name, valueLowThreshold.getVariable());
		inputMap.add(valueHighThreshold.name, valueHighThreshold.getVariable());

	}

	@Override
	public void declareOutput(VarList outputMap) {
		// TODO Auto-generated method stub

	}

	/*
	 * //Illumination Correction private void computeUnshading( File fileIn,
	 * Sequence seqShading) { System.gc();
	 * 
	 * 
	 * System.out.println("Compute " + fileIn );
	 * 
	 * if ( !fileIn.getAbsolutePath().endsWith(".tif") ) { System.out.println(
	 * "Ignoring file " + fileIn ); return; } String namefile = fileIn.getName();
	 * String nameFileOrig=namefile;
	 * 
	 * System.out.print("Load..");
	 * 
	 * Sequence sequence = new Sequence(); Loader loader = new Loader( sequence ,
	 * new File[]{fileIn}); loader.start(); try { loader.join(); } catch
	 * (InterruptedException e) { e.printStackTrace(); }
	 * 
	 * 
	 * // compute the correction double [] input =
	 * ImageDoubleConverter.convertFrom(sequence.getImageAt(0))[0]; double []
	 * bufShading = ImageDoubleConverter.convertFrom(seqShading.getImageAt(0))[0];
	 * double [] bufOutput = new
	 * ImageDouble(input.width,input.height,input.depth,input.resolutionX,input.
	 * resolutionY,input.resolutionZ);
	 * 
	 * for (int i=0; i<input.width*input.height; i++)
	 * bufOutput.data[i]=input.data[i]/bufShading.data[i]*correctValue; // TODO:
	 * Verifier: correctValue ???
	 * 
	 * currentdir = fileIn.getAbsolutePath(); currentdir = currentdir.substring( 0
	 * , currentdir.length() - sequence.file[0].getName().length() );
	 * 
	 * System.out.print("dir..."+ currentdir);
	 * 
	 * String filename = null; Sequence seqOut=new Sequence();
	 * seqOut.add(bufOutput.toVolumetricImage(false));
	 * 
	 * if (nbf<10) filename = currentdir + "RESULT/" + "StackCorr_000"+nbf; else
	 * if (nbf<100) filename = currentdir + "RESULT/" + "StackCorr_00"+nbf; else
	 * if (nbf<1000) filename = currentdir + "RESULT/" + "StackCorr_0"+nbf; else
	 * filename = currentdir + "RESULT/" + "StackCorr_"+nbf; // filename =
	 * "/Users/vannary0/Desktop/SHG2/pgmRecalagePourVannary/patient1/05_2333_06/testRedress";
	 * System.out.println("  filename : "+filename); nbf++; Saver saver = new
	 * Saver( seqOut, new File( filename), FileType.TIFF, false); saver.run();
	 * 
	 * 
	 * }
	 */

}
