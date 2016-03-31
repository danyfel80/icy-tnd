package plugins.danyfel80.topologicalnetworkdescription.specific;

import algorithms.danyfel80.topologicalnetworkdescription.EndnessCalculator;
import icy.gui.dialog.MessageDialog;
import icy.sequence.Sequence;
import icy.system.profile.CPUMonitor;
import plugins.adufour.blocks.lang.Block;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.ezplug.EzPlug;
import plugins.adufour.ezplug.EzVarBoolean;
import plugins.adufour.ezplug.EzVarSequence;

public class SegmentationEndnessPlugin extends EzPlug implements Block{

  private EzVarSequence inputCostToSeedSequence = new EzVarSequence("Cost To Seed");
  private EzVarSequence inputSquaredDistanceMapSequence = new EzVarSequence("Squared Distance Map");
  private EzVarBoolean inputAddResult = new EzVarBoolean("Show Result Sequences", true);

  private EzVarSequence outputEndnessSequence = new EzVarSequence("Endness Map");

  @Override
  protected void initialize() {
    addEzComponent(inputCostToSeedSequence);
    addEzComponent(inputSquaredDistanceMapSequence);
    addEzComponent(inputAddResult);
  }

  @Override
  protected void execute() {

    // Check if sequence is present
    if (inputCostToSeedSequence.getValue() == null || inputSquaredDistanceMapSequence.getValue() == null) {
      MessageDialog.showDialog("Error", "Please select valid sequences before starting the algorithm", MessageDialog.ERROR_MESSAGE);
      return;
    }

    Sequence costFunctionToSeedSequence = inputCostToSeedSequence.getValue();
    Sequence squaredDistanceMapSequence = inputSquaredDistanceMapSequence.getValue();

    CPUMonitor cpu = new CPUMonitor();

    cpu.start();

    // Get endness image
    Sequence endnessSequence = EndnessCalculator.process(squaredDistanceMapSequence, costFunctionToSeedSequence);

    cpu.stop();
    if (inputAddResult.getValue()) {
      addSequence(endnessSequence);
    }
    outputEndnessSequence.setValue(endnessSequence);
    //MessageDialog.showDialog("Endness Map", "Endness Map Execution time : " + cpu.getCPUElapsedTimeSec() + "s.", MessageDialog.INFORMATION_MESSAGE);
    System.out.println("Endness Map Execution time : " + cpu.getCPUElapsedTimeSec() + "s.");
  }

  @Override
  public void clean() {}

  @Override
  public void declareInput(VarList inputMap) {
    inputMap.add(inputCostToSeedSequence.name, inputCostToSeedSequence.getVariable());
    inputMap.add(inputSquaredDistanceMapSequence.name, inputSquaredDistanceMapSequence.getVariable());
    inputMap.add(inputAddResult.name, inputAddResult.getVariable());
  }

  @Override
  public void declareOutput(VarList outputMap) {
    outputMap.add(outputEndnessSequence.name, outputEndnessSequence.getVariable());
  }

}
