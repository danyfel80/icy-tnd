/**
 * 
 */
package plugins.danyfel80.topologicalnetworkdescription.specific;

import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Point3i;

import org.apache.poi.ss.usermodel.Workbook;
import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import algorithms.danyfel80.networktopology.analysis.NetworkDescriptionReporter;
import icy.gui.dialog.MessageDialog;
import icy.plugin.abstract_.PluginActionable;
import icy.sequence.Sequence;
import plugins.adufour.blocks.lang.Block;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.ezplug.EzVarSequence;
import plugins.adufour.vars.lang.Var;
import plugins.adufour.vars.lang.VarWorkbook;
import plugins.adufour.workbooks.Workbooks;

/**
 * Plugin to create the report of the network found on the input sequence.
 * 
 * @author Daniel Felipe Gonzalez Obando
 */
public class NetworkDescriptionReportPlugin extends PluginActionable implements Block {

	// Input variables
	private Var<DefaultDirectedGraph<Point3i, DefaultEdge>> inGraph = new Var<DefaultDirectedGraph<Point3i, DefaultEdge>>(
	    "Graph description", new DefaultDirectedGraph<Point3i, DefaultEdge>(DefaultEdge.class));
	private EzVarSequence inMSF = new EzVarSequence("Minimum spanning forest");
	private Var<List<Point3i>> inSeeds = new Var<>("Seed points", new ArrayList<>()); 

	// Output variables
	private VarWorkbook outWorkbook = new VarWorkbook("Report", Workbooks.createEmptyWorkbook());

	// Internal variables
	DirectedGraph<Point3i, DefaultEdge> graph;
	Sequence msfSeq;
	List<Point3i> seeds;
	Workbook reportWorkbook;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * plugins.adufour.blocks.lang.Block#declareInput(plugins.adufour.blocks.util.
	 * VarList)
	 */
	@Override
	public void declareInput(VarList inputMap) {
		inputMap.add(inGraph.getName(), inGraph);
		inputMap.add(inMSF.name, inMSF.getVariable());
		inputMap.add(inSeeds.getName(), inSeeds);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * plugins.adufour.blocks.lang.Block#declareOutput(plugins.adufour.blocks.util
	 * .VarList)
	 */
	@Override
	public void declareOutput(VarList outputMap) {
		outputMap.add(outWorkbook.getName(), outWorkbook);
	}

	@Override
	public void run() {

		if (!validateInput())
			return;

		graph = inGraph.getValue();
		msfSeq = inMSF.getValue();
		seeds = inSeeds.getValue();

		reportWorkbook = NetworkDescriptionReporter.createReport(graph, msfSeq, seeds);
		outWorkbook.setValue(reportWorkbook);
	}

	private boolean validateInput() {
		if (inGraph.getValue() == null) {
			MessageDialog.showDialog("Wrong input", "The provided graph is null.", MessageDialog.ERROR_MESSAGE);
			return false;
		}
		if (inGraph.getValue().vertexSet().size() == 0) {
			MessageDialog.showDialog("Wrong input", "The provided graph has no vertices.", MessageDialog.ERROR_MESSAGE);
			return false;
		}
		if (inMSF.getValue() == null) {
			MessageDialog.showDialog("Wrong input", "The provided MSF sequence is null.", MessageDialog.ERROR_MESSAGE);
			return false;
		}
		if (inMSF.getValue().isEmpty()) {
			MessageDialog.showDialog("Wrong input", "The provided MSF sequence is empty.", MessageDialog.ERROR_MESSAGE);
			return false;
		}
		if (inSeeds.getValue() == null || inSeeds.getValue().isEmpty()) {
			MessageDialog.showDialog("Wrong input", "No seed points were provided.", MessageDialog.ERROR_MESSAGE);
			return false;
		}
		
		return true;
	}
}
