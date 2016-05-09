package plugins.danyfel80.topologicalnetworkdescription.specific;

import java.util.List;

import algorithms.danyfel80.topologicalnetworkdescription.RegionGrowingSegmenter;
import icy.gui.dialog.MessageDialog;
import icy.roi.ROI;
import icy.sequence.Sequence;
import icy.system.profile.CPUMonitor;
import plugins.adufour.blocks.lang.Block;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.ezplug.EzPlug;
import plugins.adufour.ezplug.EzVarBoolean;
import plugins.adufour.ezplug.EzVarSequence;
import plugins.kernel.roi.roi2d.ROI2DPoint;

public class RegionGrowingSegmentationPlugin extends EzPlug implements Block {

	private EzVarSequence inputThresholdedSequence = new EzVarSequence("Thresholded Sequence");
	private EzVarSequence inputSequence = new EzVarSequence("Sequence with seeds(ROIs)");
	private EzVarBoolean inputAddResult = new EzVarBoolean("Show Result Sequence", true);

	private EzVarSequence outputSequence = new EzVarSequence("Segmented Sequence");

	@Override
	protected void initialize() {
		addEzComponent(inputSequence);
		addEzComponent(inputThresholdedSequence);
		addEzComponent(inputAddResult);
	}

	@Override
	protected void execute() {
		// Check if sequence is present
		if (inputSequence.getValue() == null || inputThresholdedSequence == null) {
			MessageDialog.showDialog("Error", "Please select valid sequences before starting the algorithm",
			    MessageDialog.ERROR_MESSAGE);
			return;
		}

		Sequence sequence = inputSequence.getValue();
		Sequence threshedSequence = inputThresholdedSequence.getValue();

		// Check if seed is given
		if (sequence.getROIs(ROI2DPoint.class).size() == 0) {
			MessageDialog.showDialog("Error",
			    "Please specify the starting points of the region growing before starting the algorithm",
			    MessageDialog.ERROR_MESSAGE);
			return;
		}

		List<? extends ROI> seeds = sequence.getROIs(ROI2DPoint.class);

		CPUMonitor cpu = new CPUMonitor();
		cpu.start();

		// Get segmented image sequence
		Sequence ccLabelsSequence  = new Sequence(threshedSequence.getName() + "_LabeledConnectedComponents");
		@SuppressWarnings("unchecked")
		Sequence segmentedSequence = RegionGrowingSegmenter.process(threshedSequence, (List<ROI2DPoint>) seeds, ccLabelsSequence);

		cpu.stop();
		if (inputAddResult.getValue()) {
			addSequence(segmentedSequence);
			addSequence(ccLabelsSequence);
		}
		outputSequence.setValue(segmentedSequence);
		// MessageDialog.showDialog("Result Segmentation", "Segmentation Execution
		// time : " + cpu.getCPUElapsedTimeSec() + "s.",
		// MessageDialog.INFORMATION_MESSAGE);
		System.out.println("Segmentation Execution time : " + cpu.getCPUElapsedTimeSec() + "s.");

	}

	@Override
	public void clean() {
	}

	@Override
	public void declareInput(VarList inputMap) {
		inputMap.add(inputSequence.name, inputSequence.getVariable());
		inputMap.add(inputThresholdedSequence.name, inputThresholdedSequence.getVariable());
		inputMap.add(inputAddResult.name, inputAddResult.getVariable());
	}

	@Override
	public void declareOutput(VarList outputMap) {
		outputMap.add(outputSequence.name, outputSequence.getVariable());
	}
}
