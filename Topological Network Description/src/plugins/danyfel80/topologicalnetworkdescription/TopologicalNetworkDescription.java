package plugins.danyfel80.topologicalnetworkdescription;

import java.util.List;

import algorithms.danyfel80.topologicalnetworkdescription.CostToSeedCalculator;
import algorithms.danyfel80.topologicalnetworkdescription.EndnessCalculator;
import algorithms.danyfel80.topologicalnetworkdescription.NetworkDescriptionConstructor;
import algorithms.danyfel80.topologicalnetworkdescription.RegionGrowingSegmenter;
import algorithms.danyfel80.topologicalnetworkdescription.SegmentDistanceCalcultator;
import algorithms.danyfel80.topologicalnetworkdescription.Thresholder;
import icy.gui.dialog.MessageDialog;
import icy.roi.ROI;
import icy.sequence.Sequence;
import icy.system.profile.CPUMonitor;
import plugins.adufour.ezplug.EzPlug;
import plugins.adufour.ezplug.EzVarInteger;
import plugins.adufour.ezplug.EzVarSequence;
import plugins.kernel.roi.roi2d.ROI2DPoint;

/**
 * Topological Network Description plugin
 * @author Daniel Felipe Gonzalez Obando
 */
public class TopologicalNetworkDescription extends EzPlug {

  private EzVarSequence sequenceIn = new EzVarSequence("Sequence");
  private EzVarInteger thresholdIn = new EzVarInteger("Threshold value");
  private EzVarInteger inMinLabelingRadius = new EzVarInteger("Minimum Labeling Radius");

  @Override
  protected void initialize() {
    thresholdIn.setValue(128);
    thresholdIn.setMinValue(1);
    inMinLabelingRadius.setValue(4);
    inMinLabelingRadius.setMinValue(2);
    addEzComponent(sequenceIn);
    addEzComponent(thresholdIn);
  }

  @Override
  protected void execute() {

    // Check if sequence is present
    if (sequenceIn.getValue() == null) {
      MessageDialog.showDialog("Error", "Please select a sequence before starting the algorithm", MessageDialog.ERROR_MESSAGE);
      return;
    }

    Sequence sequence = sequenceIn.getValue();
    Double threshold = thresholdIn.getValue().doubleValue();

    // Check if seed is given
    if (sequence.getROIs(ROI2DPoint.class).size() == 0) {
      MessageDialog.showDialog("Error", "Please specify the starting points of the region growing before starting the algorithm", MessageDialog.ERROR_MESSAGE);
      return;
    }

    List<? extends ROI> seeds = sequence.getROIs(ROI2DPoint.class);

    CPUMonitor cpu = new CPUMonitor();

    // Get threshold image sequence
    cpu.start();
    Sequence threshedSequence = Thresholder.process(sequence, threshold);
    cpu.stop();
    addSequence(threshedSequence);
    //MessageDialog.showDialog("Result Threshold", "Threshold Execution time : " + cpu.getCPUElapsedTimeSec() + "s.", MessageDialog.INFORMATION_MESSAGE);
    System.out.println("Threshold Execution time : " + cpu.getCPUElapsedTimeSec() + "s.");
    // Get segmented image sequence
    cpu.start();
    @SuppressWarnings("unchecked")
    Sequence segmentedSequence = RegionGrowingSegmenter.process(threshedSequence, (List<ROI2DPoint>)seeds);
    cpu.stop();
    addSequence(segmentedSequence);
    //MessageDialog.showDialog("Result Segmentation", "Segmentation Execution time : " + cpu.getCPUElapsedTimeSec() + "s.", MessageDialog.INFORMATION_MESSAGE);
    System.out.println("Segmentation Execution time : " + cpu.getCPUElapsedTimeSec() + "s.");

    // Get distance map sequence
    cpu.start();
    SegmentDistanceCalcultator sdc = new SegmentDistanceCalcultator(segmentedSequence);
    Sequence squaredDistanceMapSequence = sdc.process();
    Sequence distanceMapSequence = sdc.getDistanceMap();
    Sequence invertedDistanceMapSequence = sdc.getInvertedSquaredDistanceMap();
    cpu.stop();
    addSequence(squaredDistanceMapSequence);
    addSequence(distanceMapSequence);
    addSequence(invertedDistanceMapSequence);
    //MessageDialog.showDialog("Result Distance Map", "Distance Map Calculus Execution time : " + cpu.getCPUElapsedTimeSec() + "s.", MessageDialog.INFORMATION_MESSAGE);
    System.out.println("Distance Map Execution time : " + cpu.getCPUElapsedTimeSec() + "s.");

    // Get cost function to seeds
    cpu.start();
    @SuppressWarnings("unchecked")
    CostToSeedCalculator ctsc = new CostToSeedCalculator(invertedDistanceMapSequence, (List<ROI2DPoint>)seeds);
    Sequence costFunctionToSeedSequence = ctsc.process();
    Sequence minimumSpanningTreeSequence = ctsc.getMinimumSpaningTree();
    cpu.stop();
    addSequence(costFunctionToSeedSequence);
    addSequence(minimumSpanningTreeSequence);

    //MessageDialog.showDialog("Cost Function to Seed", "Cost Function to Seed Execution time : " + cpu.getCPUElapsedTimeSec() + "s.", MessageDialog.INFORMATION_MESSAGE);
    System.out.println("Cost Function to Seed Execution time : " + cpu.getCPUElapsedTimeSec() + "s.");

    // Get endness image
    cpu.start();
    Sequence endnessSequence = EndnessCalculator.process(squaredDistanceMapSequence, costFunctionToSeedSequence);
    cpu.stop();
    addSequence(endnessSequence);
    //MessageDialog.showDialog("Endness Map", "Endness Map Execution time : " + cpu.getCPUElapsedTimeSec() + "s.", MessageDialog.INFORMATION_MESSAGE);
    System.out.println("Endness Map Execution time : " + cpu.getCPUElapsedTimeSec() + "s.");

    // Get sequence description graph
    cpu.start();
    NetworkDescriptionConstructor ndc = new NetworkDescriptionConstructor(endnessSequence, minimumSpanningTreeSequence, squaredDistanceMapSequence, 4);
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

    //MessageDialog.showDialog("Network Description Construction", "Network Description Execution time : " + cpu.getCPUElapsedTimeSec() + "s.", MessageDialog.INFORMATION_MESSAGE);
    System.out.println("Network Description Execution time : " + cpu.getCPUElapsedTimeSec() + "s.");
    System.out.println(ndc.getGraph());
  }

  @Override
  public void clean() {
  }
}
