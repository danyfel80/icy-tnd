package plugins.danyfel80.topologicalnetworkdescription.specific;

import algorithms.danyfel80.topologicalnetworkdescription.DistanceMapGenerator;
import icy.gui.dialog.MessageDialog;
import icy.sequence.Sequence;
import icy.system.profile.CPUMonitor;
import plugins.adufour.blocks.lang.Block;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.ezplug.EzPlug;
import plugins.adufour.ezplug.EzVarBoolean;
import plugins.adufour.ezplug.EzVarSequence;

public class DistanceMapGenerationPlugin extends EzPlug implements Block {

	private EzVarSequence inputSegmentedSequence = new EzVarSequence("Segmented Sequence");
	private EzVarBoolean inputAddResult = new EzVarBoolean("Show Result Sequences", true);

	private EzVarSequence outputDistanceSequence = new EzVarSequence("Distance Map");
	private EzVarSequence outputSquaredDistanceSequence = new EzVarSequence("Squared Distance Map");
	private EzVarSequence outputInvertedSquaredDistanceSequence = new EzVarSequence("Inverted Squared Distance Map");

	@Override
	protected void initialize() {
		addEzComponent(inputSegmentedSequence);
		addEzComponent(inputAddResult);
	}

	@Override
	protected void execute() {
		// Check if sequence is present
		if (inputSegmentedSequence.getValue() == null) {
			MessageDialog.showDialog("Error", "Please select a sequence before starting the algorithm",
			    MessageDialog.ERROR_MESSAGE);
			return;
		}

		Sequence segmentedSequence = inputSegmentedSequence.getValue();

		CPUMonitor cpu = new CPUMonitor();
		cpu.start();

		// Get distance map sequence
		DistanceMapGenerator sdc = new DistanceMapGenerator(segmentedSequence);
		sdc.process();
		Sequence squaredDistanceMapSequence = sdc.getSquaredDistanceMap();
		Sequence distanceMapSequence = sdc.getDistanceMap();
		Sequence invertedDistanceMapSequence = sdc.getInvertedSquaredDistanceMap();
		cpu.stop();
		if (inputAddResult.getValue()) {
			addSequence(squaredDistanceMapSequence);
			addSequence(distanceMapSequence);
			addSequence(invertedDistanceMapSequence);
		}
		outputDistanceSequence.setValue(distanceMapSequence);
		outputSquaredDistanceSequence.setValue(squaredDistanceMapSequence);
		outputInvertedSquaredDistanceSequence.setValue(invertedDistanceMapSequence);
		// MessageDialog.showDialog("Result Distance Map", "Distance Map Calculus
		// Execution time : " + cpu.getCPUElapsedTimeSec() + "s.",
		// MessageDialog.INFORMATION_MESSAGE);
		System.out.println("Distance Map Execution time : " + cpu.getCPUElapsedTimeSec() + "s.");
	}

	@Override
	public void clean() {
	}

	@Override
	public void declareInput(VarList inputMap) {
		inputMap.add(inputSegmentedSequence.name, inputSegmentedSequence.getVariable());
		inputMap.add(inputAddResult.name, inputAddResult.getVariable());
	}

	@Override
	public void declareOutput(VarList outputMap) {
		outputMap.add(outputDistanceSequence.name, outputDistanceSequence.getVariable());
		outputMap.add(outputSquaredDistanceSequence.name, outputSquaredDistanceSequence.getVariable());
		outputMap.add(outputInvertedSquaredDistanceSequence.name, outputInvertedSquaredDistanceSequence.getVariable());
	}
}
