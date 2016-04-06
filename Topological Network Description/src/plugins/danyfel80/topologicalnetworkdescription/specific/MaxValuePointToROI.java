package plugins.danyfel80.topologicalnetworkdescription.specific;

import icy.gui.dialog.MessageDialog;
import icy.sequence.Sequence;
import icy.sequence.SequenceDataIterator;
import icy.type.point.Point5D;
import plugins.adufour.blocks.lang.Block;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.ezplug.EzPlug;
import plugins.adufour.ezplug.EzVarSequence;
import plugins.adufour.vars.lang.Var;
import plugins.kernel.roi.roi2d.ROI2DPoint;

/**
 * Finds a point with the maximum value in the gray-level sequence.
 * @author Daniel Felipe Gonzalez Obando
 */
public class MaxValuePointToROI extends EzPlug implements Block {

  // Input
  private EzVarSequence inSequence = new EzVarSequence("Gray-level Sequence");
  
  // Output
  private Var<ROI2DPoint> outROI = new Var<ROI2DPoint>("ROI", ROI2DPoint.class);
  
  /* (non-Javadoc)
   * @see plugins.adufour.ezplug.EzPlug#initialize()
   */
  @Override
  protected void initialize() {
    addEzComponent(inSequence);
  }
  
  /* (non-Javadoc)
   * @see plugins.adufour.ezplug.EzPlug#execute()
   */
  @Override
  protected void execute() {
    System.out.println("MaxValuePointToROI.execute()");
    if (inSequence.getValue() != null && !inSequence.getValue().isEmpty()) {
      Sequence seq = inSequence.getValue();
      if(inSequence.getValue().getSizeC() > 1) {
        MessageDialog.showDialog("Warning... Multi-channel sequence", "The input image has more than one channel. Using only the first channel.", MessageDialog.WARNING_MESSAGE);
      }
      
      double maxVal = seq.getChannelMax(0);
      SequenceDataIterator it ;
      
      for (int z = 0; z < seq.getSizeZ(); z++) {
        it = new SequenceDataIterator(seq, z, 0, 0);
        while (!it.done()) {
          if (it.get() == maxVal) {
            Point5D point = new Point5D.Integer(it.getPositionX(), it.getPositionY(), z, 0, 0);
            ROI2DPoint roi = new ROI2DPoint(point);
            roi.setZ(z);
            outROI.setValue(roi);
            System.out.println(point);
            return;
          }
          it.next();
        }
      }
      System.out.println("No maximum");
      MessageDialog.showDialog("Error", "No maximum found.", MessageDialog.ERROR_MESSAGE);
    } else {
      MessageDialog.showDialog("Error", "Please select a valid sequence.", MessageDialog.ERROR_MESSAGE);
    }
    System.out.println("MaxValuePointToROI.execute()");
  }
  
  /* (non-Javadoc)
   * @see plugins.adufour.blocks.lang.Block#declareInput(plugins.adufour.blocks.util.VarList)
   */
  @Override
  public void declareInput(VarList inputMap) {
    inputMap.add(inSequence.name, inSequence.getVariable());
  }

  /* (non-Javadoc)
   * @see plugins.adufour.blocks.lang.Block#declareOutput(plugins.adufour.blocks.util.VarList)
   */
  @Override
  public void declareOutput(VarList outputMap) {
    outputMap.add(outROI.getName(), outROI);
  }

  /* (non-Javadoc)
   * @see plugins.adufour.ezplug.EzPlug#clean()
   */
  @Override
  public void clean() {}

}
