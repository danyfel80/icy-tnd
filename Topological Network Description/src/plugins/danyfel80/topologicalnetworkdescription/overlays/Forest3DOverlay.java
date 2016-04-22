package plugins.danyfel80.topologicalnetworkdescription.overlays;

import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.vecmath.Point3i;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import icy.painter.Overlay;
import icy.painter.VtkPainter;
import icy.util.Random;
import vtk.vtkActor;
import vtk.vtkCellArray;
import vtk.vtkLine;
import vtk.vtkPoints;
import vtk.vtkPolyData;
import vtk.vtkPolyDataMapper;
import vtk.vtkProp;
import vtk.vtkUnsignedCharArray;

/**
 * Overlay to create 3d forest based on a given graph.
 * 
 * @author Daniel Felipe Gonzalez Obando
 */
public class Forest3DOverlay extends Overlay implements VtkPainter {

	private vtkActor edgesActor;

	public Forest3DOverlay(String name, DirectedGraph<Point3i, DefaultEdge> graph, List<Point3i> seeds) {
		super(name);
		createForest(graph, seeds);
	}

	private void createForest(DirectedGraph<Point3i, DefaultEdge> graph, List<Point3i> seeds) {

		// Set points
		final vtkPoints vertices = new vtkPoints();
		Map<Point3i, Integer> vertexIds = new HashMap<Point3i, Integer>();
		for (Point3i p : graph.vertexSet()) {
			vertexIds.put(p, vertices.InsertNextPoint(p.x, p.y, p.z));
		}

		// Set lines

		vtkCellArray edges = new vtkCellArray();
		// TODO fix coloring... VTK breaks
		vtkUnsignedCharArray colors = new vtkUnsignedCharArray();
		colors.SetNumberOfComponents(3);
		colors.SetNumberOfTuples(graph.edgeSet().size());
		final byte[] colorData = new byte[graph.edgeSet().size() * 3];

		int edgeI = 0;
		for (DefaultEdge e : graph.edgeSet()) {
			vtkLine line = new vtkLine();
			Point3i pS = graph.getEdgeSource(e);
			Point3i pT = graph.getEdgeTarget(e);

			line.GetPointIds().SetId(0, vertexIds.get(pS));
			line.GetPointIds().SetId(1, vertexIds.get(pT));

			edges.InsertNextCell(line);
			Color c = new Color(Random.nextInt(256), Random.nextInt(256), Random.nextInt(256));
			final byte r = (byte) c.getRed();
			final byte g = (byte) c.getGreen();
			final byte b = (byte) c.getBlue();

			colorData[edgeI * 3 + 0] = r;
			colorData[edgeI * 3 + 1] = g;
			colorData[edgeI * 3 + 2] = b;
			// colors.InsertNextTuple3(Random.nextInt(256),Random.nextInt(256),Random.nextInt(256));
			edgeI++;
		}

		// Create poly data
		final vtkPolyData polyData = new vtkPolyData();
		// set vertex to the poly
		polyData.SetPoints(vertices);
		// set lines to the poly
		polyData.SetLines(edges);
		// set lines colors
		polyData.GetCellData().SetScalars(colors);

		colors.SetJavaArray(colorData);
		colors.Modified();

		// add actor to the renderer
		final vtkPolyDataMapper polyMapper = new vtkPolyDataMapper();
		polyMapper.SetInputData(polyData);

		edgesActor = new vtkActor();
		edgesActor.SetMapper(polyMapper);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see icy.painter.VtkPainter#getProps()
	 */
	@Override
	public vtkProp[] getProps() {
		return new vtkProp[] { edgesActor };
	}

}
