package plugins.danyfel80.topologicalnetworkdescription.specific;

import java.util.List;

import algorithms.danyfel80.topologicalnetworkdescription.CostToSeedCalculator;
import icy.gui.dialog.MessageDialog;
import icy.roi.ROI;
import icy.sequence.Sequence;
import icy.system.profile.CPUMonitor;
import plugins.adufour.blocks.lang.Block;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.ezplug.EzPlug;
import plugins.adufour.ezplug.EzVarBoolean;
import plugins.adufour.ezplug.EzVarDouble;
import plugins.adufour.ezplug.EzVarSequence;
import plugins.kernel.roi.roi2d.ROI2DPoint;

public class CostToSeedPlugin extends EzPlug implements Block {

	private EzVarSequence inputOriginalSequence = new EzVarSequence("Sequence with seeds(ROIs)");
	private EzVarSequence inputInvertedDistanceMapSequence = new EzVarSequence("Inverted Distance Map");
	private EzVarDouble inputBaseDirectionWeightMultiplier = new EzVarDouble("Base direction weight multiplier");
	private EzVarBoolean inputAddResult = new EzVarBoolean("Show Result Sequences", true);

	private EzVarSequence outputCostToSeedSequence = new EzVarSequence("Cost To Seed");
	private EzVarSequence outputMinimumSpanningTree = new EzVarSequence("Minimum Spanning Tree");

	@Override
	protected void initialize() {
	  inputBaseDirectionWeightMultiplier.setValue(1.0);
	  addEzComponent(inputOriginalSequence);
		addEzComponent(inputInvertedDistanceMapSequence);
		addEzComponent(inputBaseDirectionWeightMultiplier);
		addEzComponent(inputAddResult);
		
		inputBaseDirectionWeightMultiplier.setMinValue(1.0);
	}

	@Override
	protected void execute() {
		// Check if sequence is present
		if (inputOriginalSequence.getValue() == null || inputInvertedDistanceMapSequence.getValue() == null) {
			MessageDialog.showDialog("Error", "Please select valid sequences before starting the algorithm",
			    MessageDialog.ERROR_MESSAGE);
			return;
		}

		Sequence originalSequence = inputOriginalSequence.getValue();
		// Check if seed is given
		if (originalSequence.getROIs(ROI2DPoint.class).size() == 0) {
			MessageDialog.showDialog("Error",
			    "Please specify the starting points of the region growing before starting the algorithm",
			    MessageDialog.ERROR_MESSAGE);
			return;
		}

		List<? extends ROI> seeds = originalSequence.getROIs(ROI2DPoint.class);

		Sequence invertedDistanceMapSequence = inputInvertedDistanceMapSequence.getValue();

		CPUMonitor cpu = new CPUMonitor();

		cpu.start();

		// Get cost function to seeds
		double baseDirectionWeightMultiplier = inputBaseDirectionWeightMultiplier.getValue();
		@SuppressWarnings("unchecked")
		CostToSeedCalculator ctsc = new CostToSeedCalculator(invertedDistanceMapSequence, (List<ROI2DPoint>) seeds, baseDirectionWeightMultiplier);
		Sequence costFunctionToSeedSequence = ctsc.process();
		Sequence minimumSpanningTreeSequence = ctsc.getMinimumSpaningTree();

		cpu.stop();
		if (inputAddResult.getValue()) {
			addSequence(costFunctionToSeedSequence);
			addSequence(minimumSpanningTreeSequence);
		}
		outputCostToSeedSequence.setValue(costFunctionToSeedSequence);
		outputMinimumSpanningTree.setValue(minimumSpanningTreeSequence);
		// MessageDialog.showDialog("Cost Function to Seed", "Cost Function to Seed
		// Execution time : " + cpu.getCPUElapsedTimeSec() + "s.",
		// MessageDialog.INFORMATION_MESSAGE);
		System.out.println("Cost Function to Seed Execution time : " + cpu.getCPUElapsedTimeSec() + "s.");
	}

	@Override
	public void clean() {
	}

	@Override
	public void declareInput(VarList inputMap) {
	  inputBaseDirectionWeightMultiplier.setValue(1.0);
		inputMap.add(inputOriginalSequence.name, inputOriginalSequence.getVariable());
		inputMap.add(inputInvertedDistanceMapSequence.name, inputInvertedDistanceMapSequence.getVariable());
		inputMap.add(inputBaseDirectionWeightMultiplier.name, inputBaseDirectionWeightMultiplier.getVariable());
		inputMap.add(inputAddResult.name, inputAddResult.getVariable());
	}

	@Override
	public void declareOutput(VarList outputMap) {
		outputMap.add(outputCostToSeedSequence.name, outputCostToSeedSequence.getVariable());
		outputMap.add(outputMinimumSpanningTree.name, outputMinimumSpanningTree.getVariable());
	}
}
