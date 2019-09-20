package plugins.danyfel80.topologicalnetworkdescription;

import java.util.List;

import algorithms.danyfel80.topologicalnetworkdescription.CostToSeedMapGenerator;
import algorithms.danyfel80.topologicalnetworkdescription.DistanceMapGenerator;
import algorithms.danyfel80.topologicalnetworkdescription.EndnessMapGenerator;
import algorithms.danyfel80.topologicalnetworkdescription.FloodFillFilter;
import algorithms.danyfel80.topologicalnetworkdescription.ThresholdingSegmenter;
import algorithms.danyfel80.topologicalnetworkdescription.TopologicalNetworkDescriptor;
import icy.gui.dialog.MessageDialog;
import icy.sequence.Sequence;
import icy.system.profile.CPUMonitor;
import plugins.adufour.ezplug.EzPlug;
import plugins.adufour.ezplug.EzVarDouble;
import plugins.adufour.ezplug.EzVarInteger;
import plugins.adufour.ezplug.EzVarSequence;
import plugins.kernel.roi.roi2d.ROI2DPoint;

/**
 * Topological Network Description plugin
 * 
 * @author Daniel Felipe Gonzalez Obando
 */
public class TopologicalNetworkDescriptionFrameworkPlugin extends EzPlug {

	private EzVarSequence sequenceIn = new EzVarSequence("Sequence");
	private EzVarInteger thresholdIn = new EzVarInteger("Threshold value");
	private EzVarInteger inMinLabelingRadius = new EzVarInteger("Minimum Labeling Radius");
	private EzVarDouble inLabelingRadiusScale = new EzVarDouble("Labeling Radius Scale");
	private EzVarDouble inLevelChangeWeightMultiplier = new EzVarDouble("Level change weight multiplier");
	private EzVarDouble inDirectionChangeWeightMultiplier = new EzVarDouble("Direction change weight multiplier");

	@Override
	protected void initialize() {
		thresholdIn.setValue(128);
		thresholdIn.setMinValue(1);
		inMinLabelingRadius.setValue(4);
		inMinLabelingRadius.setMinValue(2);
		inLabelingRadiusScale.setValue(2.5);
		inLabelingRadiusScale.setMinValue(0.1);
		inLevelChangeWeightMultiplier.setValue(2.0);
		inDirectionChangeWeightMultiplier.setValue(1.0);
		addEzComponent(sequenceIn);
		addEzComponent(thresholdIn);
		addEzComponent(inLevelChangeWeightMultiplier);
		addEzComponent(inDirectionChangeWeightMultiplier);
		inLevelChangeWeightMultiplier.setMinValue(0.0);
		inDirectionChangeWeightMultiplier.setMinValue(0.0);
	}

	@Override
	protected void execute() {

		// Check if sequence is present
		if (sequenceIn.getValue() == null) {
			MessageDialog.showDialog("Error", "Please select a sequence before starting the algorithm",
					MessageDialog.ERROR_MESSAGE);
			return;
		}

		Sequence sequence = sequenceIn.getValue();
		Double threshold = thresholdIn.getValue().doubleValue();

		// Check if seed is given
		if (sequence.getROIs(ROI2DPoint.class, false).size() == 0) {
			MessageDialog.showDialog("Error",
					"Please specify the starting points of the region growing before starting the algorithm",
					MessageDialog.ERROR_MESSAGE);
			return;
		}

		List<ROI2DPoint> seeds = sequence.getROIs(ROI2DPoint.class, false);

		CPUMonitor cpu = new CPUMonitor();

		// Get threshold image sequence
		cpu.start();
		Sequence threshedSequence = ThresholdingSegmenter.process(sequence, threshold);
		cpu.stop();
		addSequence(threshedSequence);
		// MessageDialog.showDialog("Result Threshold", "Threshold Execution time :
		// " + cpu.getCPUElapsedTimeSec() + "s.",
		// MessageDialog.INFORMATION_MESSAGE);
		System.out.println("Threshold Execution time : " + cpu.getCPUElapsedTimeSec() + "s.");
		// Get segmented image sequence
		cpu.start();
		Sequence ccLabelsSequence = new Sequence(threshedSequence.getName() + "_LabeledConnectedComponents");
		Sequence segmentedSequence = FloodFillFilter.process(threshedSequence, seeds, ccLabelsSequence);
		cpu.stop();
		addSequence(segmentedSequence);
		// MessageDialog.showDialog("Result Segmentation", "Segmentation Execution
		// time : " + cpu.getCPUElapsedTimeSec() + "s.",
		// MessageDialog.INFORMATION_MESSAGE);
		System.out.println("Segmentation Execution time : " + cpu.getCPUElapsedTimeSec() + "s.");

		// Get distance map sequence
		cpu.start();
		DistanceMapGenerator sdc = new DistanceMapGenerator(segmentedSequence);
		Sequence squaredDistanceMapSequence = sdc.process();
		Sequence distanceMapSequence = sdc.getDistanceMap();
		Sequence invertedDistanceMapSequence = sdc.getInvertedSquaredDistanceMap();
		cpu.stop();
		addSequence(squaredDistanceMapSequence);
		addSequence(distanceMapSequence);
		addSequence(invertedDistanceMapSequence);
		// MessageDialog.showDialog("Result Distance Map", "Distance Map Calculus
		// Execution time : " + cpu.getCPUElapsedTimeSec() + "s.",
		// MessageDialog.INFORMATION_MESSAGE);
		System.out.println("Distance Map Execution time : " + cpu.getCPUElapsedTimeSec() + "s.");

		// Get cost function to seeds
		cpu.start();
		CostToSeedMapGenerator ctsc = new CostToSeedMapGenerator(invertedDistanceMapSequence, seeds,
				inLevelChangeWeightMultiplier.getValue(), inDirectionChangeWeightMultiplier.getValue());
		Sequence costFunctionToSeedSequence = ctsc.process();
		Sequence minimumSpanningTreeSequence = ctsc.getMinimumSpaningTree();
		cpu.stop();
		addSequence(costFunctionToSeedSequence);
		addSequence(minimumSpanningTreeSequence);

		// MessageDialog.showDialog("Cost Function to Seed", "Cost Function to Seed
		// Execution time : " + cpu.getCPUElapsedTimeSec() + "s.",
		// MessageDialog.INFORMATION_MESSAGE);
		System.out.println("Cost Function to Seed Execution time : " + cpu.getCPUElapsedTimeSec() + "s.");

		// Get endness image
		cpu.start();
		Sequence endnessSequence = EndnessMapGenerator.process(squaredDistanceMapSequence, costFunctionToSeedSequence);
		cpu.stop();
		addSequence(endnessSequence);
		// MessageDialog.showDialog("Endness Map", "Endness Map Execution time : " +
		// cpu.getCPUElapsedTimeSec() + "s.", MessageDialog.INFORMATION_MESSAGE);
		System.out.println("Endness Map Execution time : " + cpu.getCPUElapsedTimeSec() + "s.");

		// Get sequence description graph
		cpu.start();
		TopologicalNetworkDescriptor ndc = new TopologicalNetworkDescriptor(endnessSequence, minimumSpanningTreeSequence,
				squaredDistanceMapSequence, inMinLabelingRadius.getValue(), inLabelingRadiusScale.getValue());
		Sequence skeletonSequence = ndc.process(); // skeleton
		Sequence labelsSequence = ndc.getLabelSequence();
		Sequence branchSequence = ndc.getBranchSequence();
		Sequence endPointSequence = ndc.getEndPointSequence();
		Sequence labeledSkeletonSequence = ndc.getLabeledSkeletonSequence();

		cpu.stop();
		addSequence(skeletonSequence);
		addSequence(labelsSequence);
		addSequence(branchSequence);
		addSequence(endPointSequence);
		addSequence(labeledSkeletonSequence);

		// MessageDialog.showDialog("Network Description Construction", "Network
		// Description Execution time : " + cpu.getCPUElapsedTimeSec() + "s.",
		// MessageDialog.INFORMATION_MESSAGE);
		System.out.println("Network Description Execution time : " + cpu.getCPUElapsedTimeSec() + "s.");
		System.out.println(ndc.getGraph());
	}

	@Override
	public void clean() {}
}
