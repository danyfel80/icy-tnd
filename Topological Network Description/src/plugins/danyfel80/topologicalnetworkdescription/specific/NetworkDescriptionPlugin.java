package plugins.danyfel80.topologicalnetworkdescription.specific;

import javax.vecmath.Point3i;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import algorithms.danyfel80.topologicalnetworkdescription.NetworkDescriptionConstructor;
import icy.gui.dialog.MessageDialog;
import icy.sequence.Sequence;
import icy.system.profile.CPUMonitor;
import plugins.adufour.blocks.lang.Block;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.ezplug.EzPlug;
import plugins.adufour.ezplug.EzVarBoolean;
import plugins.adufour.ezplug.EzVarSequence;
import plugins.adufour.vars.lang.Var;
import plugins.danyfel80.topologicalnetworkdescription.overlays.Forest3DOverlay;

public class NetworkDescriptionPlugin extends EzPlug implements Block{

  private EzVarSequence inputOriginalSequence = new EzVarSequence("Original Sequence");
  private EzVarSequence inputEndnessSequence = new EzVarSequence("Endness Map");
  private EzVarSequence inputMinimumSpanningTreeMapSequence = new EzVarSequence("Minimum Spanning Tree");
  private EzVarSequence inputSquaredDistanceMapSequence = new EzVarSequence("Squared Distance Map");
  private EzVarBoolean inputAddResult = new EzVarBoolean("Show Result Sequences", true);

  private EzVarSequence outputBranchesSequence = new EzVarSequence("Network Branch Points");
  private EzVarSequence outputEndPointsSequence = new EzVarSequence("Network End Points");
  private EzVarSequence outputLabelsSequence = new EzVarSequence("Network Labels");
  private EzVarSequence outputSkeletonSequence = new EzVarSequence("Network Skeleton");
  private EzVarSequence outputLabeledSkeletonSequence = new EzVarSequence("Network Labeled Skeleton");
  private Var<DirectedGraph<Point3i, DefaultEdge>> outputGraphDescription = new Var<DirectedGraph<Point3i, DefaultEdge>>("Network Graph Description", new DefaultDirectedGraph<Point3i, DefaultEdge>(DefaultEdge.class));

  @Override
  protected void initialize() {
    addEzComponent(inputOriginalSequence);
    addEzComponent(inputEndnessSequence);
    addEzComponent(inputMinimumSpanningTreeMapSequence);
    addEzComponent(inputSquaredDistanceMapSequence);
    addEzComponent(inputAddResult);
  }

  @Override
  protected void execute() {
    // Check if sequence is present
    if (inputEndnessSequence.getValue() == null || inputMinimumSpanningTreeMapSequence.getValue() == null || inputSquaredDistanceMapSequence.getValue() == null) {
      MessageDialog.showDialog("Error", "Please select valid sequences before starting the algorithm", MessageDialog.ERROR_MESSAGE);
      return;
    }

    Sequence minimumSpanningTreeMapSequence = inputMinimumSpanningTreeMapSequence.getValue();
    Sequence endnessMapSequence = inputEndnessSequence.getValue();
    Sequence distanceMapSequence = inputSquaredDistanceMapSequence.getValue();

    CPUMonitor cpu = new CPUMonitor();

    cpu.start();

    // Get sequence description graph
    NetworkDescriptionConstructor ndc = new NetworkDescriptionConstructor(endnessMapSequence, minimumSpanningTreeMapSequence, distanceMapSequence);
    Sequence skeletonSequence = ndc.process(); // skeleton
    Sequence labelsSequence = ndc.getLabelSequence();
    Sequence branchSequence = ndc.getBranchSequence();
    Sequence endPointSequence = ndc.getEndPointSequence();
    Sequence labeledSkeletonSequence = ndc.getLabeledSkeletonSequence();

    cpu.stop();
    if (inputAddResult.getValue()) {
      addSequence(skeletonSequence);
      addSequence(labelsSequence);
      addSequence(branchSequence);
      addSequence(endPointSequence);
      addSequence(labeledSkeletonSequence);

      
    }
    Forest3DOverlay fol = new Forest3DOverlay("3D Forest", ndc.getGraph(), ndc.getSeedPoints());
    inputOriginalSequence.getValue().addOverlay(fol);
    addSequence(inputOriginalSequence.getValue());
    outputSkeletonSequence.setValue(skeletonSequence);
    outputBranchesSequence.setValue(branchSequence);
    outputEndPointsSequence.setValue(endPointSequence);
    outputLabelsSequence.setValue(labelsSequence);
    outputLabeledSkeletonSequence.setValue(labeledSkeletonSequence);
    outputGraphDescription = new Var<DirectedGraph<Point3i, DefaultEdge>>("Network Graph Description", ndc.getGraph());

    //MessageDialog.showDialog("Network Description Construction", "Network Description Execution time : " + cpu.getCPUElapsedTimeSec() + "s.", MessageDialog.INFORMATION_MESSAGE);
    System.out.println("Network Description Execution time : " + cpu.getCPUElapsedTimeSec() + "s.");
    System.out.println(ndc.getGraph());
  }

  @Override
  public void clean() {}

  @Override
  public void declareInput(VarList inputMap) {
    inputMap.add(inputOriginalSequence.name, inputOriginalSequence.getVariable());
    inputMap.add(inputMinimumSpanningTreeMapSequence.name, inputMinimumSpanningTreeMapSequence.getVariable());
    inputMap.add(inputEndnessSequence.name, inputEndnessSequence.getVariable());
    inputMap.add(inputSquaredDistanceMapSequence.name, inputSquaredDistanceMapSequence.getVariable());
    inputMap.add(inputAddResult.name, inputAddResult.getVariable());
  }

  @Override
  public void declareOutput(VarList outputMap) {
    outputMap.add(outputSkeletonSequence.name, outputSkeletonSequence.getVariable());
    outputMap.add(outputBranchesSequence.name, outputBranchesSequence.getVariable());
    outputMap.add(outputEndPointsSequence.name, outputEndPointsSequence.getVariable());
    outputMap.add(outputLabelsSequence.name, outputLabelsSequence.getVariable());
    outputMap.add(outputLabeledSkeletonSequence.name, outputLabeledSkeletonSequence.getVariable());
    outputMap.add(outputGraphDescription.getName(), outputGraphDescription);
  }
}
