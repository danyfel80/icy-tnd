package plugins.danyfel80.topologicalnetworkdescription.specific;

import algorithms.danyfel80.topologicalnetworkdescription.Thresholder;
import icy.gui.dialog.MessageDialog;
import icy.sequence.Sequence;
import icy.sequence.SequenceUtil;
import icy.system.profile.CPUMonitor;
import icy.type.DataType;
import plugins.adufour.blocks.lang.Block;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.ezplug.EzPlug;
import plugins.adufour.ezplug.EzVarBoolean;
import plugins.adufour.ezplug.EzVarInteger;
import plugins.adufour.ezplug.EzVarSequence;

public class ThresholderPlugin extends EzPlug implements Block {
	
	private EzVarSequence inputSequence = new EzVarSequence("Input Sequence");
	private EzVarInteger inputThreshold = new EzVarInteger("Threshold");
	private EzVarBoolean inputAddResult = new EzVarBoolean("Show Result Sequence", true);
	
	private EzVarSequence outputSequence = new EzVarSequence("Thresholded Sequence");

	@Override
	protected void initialize() {
		addEzComponent(inputSequence);
		addEzComponent(inputThreshold);
		addEzComponent(inputAddResult);
	}
	
	@Override
	protected void execute() {
			// Check if sequence is present
			if (inputSequence.getValue() == null) {
				MessageDialog.showDialog("Error", "Please select a sequence before starting the algorithm", MessageDialog.ERROR_MESSAGE);
				return;
			}

			Sequence sequence = inputSequence.getValue();
			Short threshold = inputThreshold.getValue().shortValue();
			
			CPUMonitor cpu = new CPUMonitor();
			cpu.start();
			
			// Get threshold image sequence
			Sequence shortSequence = SequenceUtil.convertToType(sequence, DataType.SHORT, false);
			shortSequence.setName(sequence.getName());
			Sequence threshedSequence = Thresholder.process(shortSequence, threshold);
			
			cpu.stop();
			if (inputAddResult.getValue()) {
				addSequence(threshedSequence);
			}
			outputSequence.setValue(threshedSequence);
			MessageDialog.showDialog("Result Threshold", "Threshold Execution time : " + cpu.getCPUElapsedTimeSec() + "s.", MessageDialog.INFORMATION_MESSAGE);
			System.out.println("Threshold Execution time : " + cpu.getCPUElapsedTimeSec() + "s.");
	}

	@Override
	public void clean() {}

	@Override
	public void declareInput(VarList inputMap) {
		inputMap.add(inputSequence.name, inputSequence.getVariable());
		inputMap.add(inputThreshold.name, inputThreshold.getVariable());
		inputMap.add(inputAddResult.name, inputAddResult.getVariable());
	}

	@Override
	public void declareOutput(VarList outputMap) {
		outputMap.add(outputSequence.name, outputSequence.getVariable());
	}
	
	
}
