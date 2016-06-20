package algorithms.danyfel80.networktopology.analysis;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.vecmath.Point2d;
import javax.vecmath.Point3i;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.BreadthFirstIterator;

import icy.sequence.Sequence;
import plugins.adufour.workbooks.Workbooks;

/**
 * Reporter class for the topology of a network-like sequence.
 * 
 * @author Daniel Felipe Gonzalez Obando
 */
public class NetworkDescriptionReporter {

	/**
	 * @param graph
	 * @param msfSeq
	 * @param seeds
	 * @return A workbook with the details of the different trees in the network.
	 */
	public static Workbook createReport(DirectedGraph<Point3i, DefaultEdge> graph, Sequence msfSeq, List<Point3i> seeds) {
		Workbook workbook = Workbooks.createEmptyWorkbook();
		Sheet sheet = workbook.createSheet("Trees Description");
		setSheetHeader(sheet);

		int rowId = 1;
		int treeId = 1;
		int branchId = 1;
		Map<Point3i, Integer> parentIds = new HashMap<Point3i, Integer>();
		for (Point3i v : seeds) {

			Map<Point3i, Double> distanceToRoot = new HashMap<>();
			Map<Point3i, Integer> depth = GraphAnalysis.getVertexDepthMap(graph, v);
			distanceToRoot.put(v, 0d);

			BreadthFirstIterator<Point3i, DefaultEdge> it = new BreadthFirstIterator<>(graph, v);
			while (it.hasNext()) {
				Point3i v1 = (Point3i) it.next();
				double v1DistToRoot = distanceToRoot.get(v1);
				Set<DefaultEdge> v1ChildEdges = graph.outgoingEdgesOf(v1);
				for (DefaultEdge v1ChildEdge : v1ChildEdges) {
					Point3i v2 = graph.getEdgeTarget(v1ChildEdge);
					Point3i v2v1Diff = new Point3i();
					v2v1Diff.sub(v2, v1);
					v2v1Diff.absolute();
					double v2DistTov1Euc = Math.sqrt(v2v1Diff.x * v2v1Diff.x + v2v1Diff.y * v2v1Diff.y + v2v1Diff.z * v2v1Diff.z);
					double v2DistTov1 = GraphAnalysis.getNodeDistanceToParent(msfSeq, v2, v1);
					distanceToRoot.put(v2, v2DistTov1 + v1DistToRoot);
					Point2d orientation = GraphAnalysis.getBranchOrientation(graph, v1ChildEdge);
					if (orientation.x < 0d) {
						orientation.x += 2.0 * Math.PI;
					}
					if (orientation.y < 0d) {
						orientation.y += 2.0 * Math.PI;
					}
					if (orientation.x == -0.0) {
						orientation.x = 0;
					}
					if (orientation.y == -0.0) {
						orientation.y = 0;
					}
					orientation.x *= 180d / Math.PI;
					orientation.y *= 180d / Math.PI;
					parentIds.put(v2, branchId);
					processBranch(sheet, rowId, treeId, parentIds.getOrDefault(v1, -1), branchId, v1, v2, depth.get(v2), orientation.x, orientation.y,
					    v2DistTov1Euc, v2DistTov1, v2DistTov1 + v1DistToRoot, v1.equals(v));
					rowId++;
					branchId++;
				}
			}

			treeId++;
		}
		return workbook;
	}

	/**
	 * Adds the header to the specified sheet specifying column names.
	 * 
	 * @param sheet
	 */
	private static void setSheetHeader(Sheet sheet) {
		Row row = sheet.createRow(0);
		Cell cell = row.createCell(0, Cell.CELL_TYPE_STRING);
		cell.setCellValue("Tree ID");
		cell = row.createCell(1, Cell.CELL_TYPE_STRING);
		cell.setCellValue("Parent branch ID");
		cell = row.createCell(2, Cell.CELL_TYPE_STRING);
		cell.setCellValue("Branch ID");
		cell = row.createCell(3, Cell.CELL_TYPE_STRING);
		cell.setCellValue("Start Point");
		cell = row.createCell(4, Cell.CELL_TYPE_STRING);
		cell.setCellValue("End Point");
		cell = row.createCell(5, Cell.CELL_TYPE_STRING);
		cell.setCellValue("Depth");
		cell = row.createCell(6, Cell.CELL_TYPE_STRING);
		cell.setCellValue("Orientation Yaw");
		cell = row.createCell(7, Cell.CELL_TYPE_STRING);
		cell.setCellValue("Orientation Pitch");
		cell = row.createCell(8, Cell.CELL_TYPE_STRING);
		cell.setCellValue("Length");
		cell = row.createCell(9, Cell.CELL_TYPE_STRING);
		cell.setCellValue("Real Length");
		cell = row.createCell(10, Cell.CELL_TYPE_STRING);
		cell.setCellValue("Distance from root");
		cell = row.createCell(11, Cell.CELL_TYPE_STRING);
		cell.setCellValue("Is root branch");
	}

	/**
	 * Adds a row to the specified sheet with the details of the specified branch.
	 * 
	 * @param sheet
	 * @param rowId
	 * @param treeId
	 * @param branchId
	 * @param v1
	 *          Source vertex
	 * @param v2
	 *          Target vertex
	 * @param depth
	 *          edgeDepth
	 * @param distanceToParent
	 */
	private static void processBranch(Sheet sheet, int rowId, int treeId, int parentBranchId, int endBranchId, Point3i v1, Point3i v2,
	    Integer depth, double yaw, double pitch, double length, double realLength, double distanceToRoot,
	    boolean isRootBranch) {
		Row row = sheet.createRow(rowId);
		Cell cell;
		// Tree ID
		cell = row.createCell(0, Cell.CELL_TYPE_NUMERIC);
		cell.setCellValue(treeId);
		// Parent branch ID
		cell = row.createCell(1, Cell.CELL_TYPE_NUMERIC);
		cell.setCellValue(parentBranchId);
		// Branch ID
		cell = row.createCell(2, Cell.CELL_TYPE_NUMERIC);
		cell.setCellValue(endBranchId);
		// Start point
		cell = row.createCell(3, Cell.CELL_TYPE_STRING);
		cell.setCellValue(v1.toString());
		// End point
		cell = row.createCell(4, Cell.CELL_TYPE_STRING);
		cell.setCellValue(v2.toString());
		// Depth
		cell = row.createCell(5, Cell.CELL_TYPE_NUMERIC);
		cell.setCellValue(depth);
		// Orientation Pitch
		cell = row.createCell(6, Cell.CELL_TYPE_NUMERIC);
		cell.setCellValue(yaw);
		// Orientation Yaw
		cell = row.createCell(7, Cell.CELL_TYPE_NUMERIC);
		cell.setCellValue(pitch);
		// Branch Lenght
		cell = row.createCell(8, Cell.CELL_TYPE_NUMERIC);
		cell.setCellValue(length);
		// Branch Real Length
		cell = row.createCell(9, Cell.CELL_TYPE_NUMERIC);
		cell.setCellValue(realLength);
		// Distance to root
		cell = row.createCell(10, Cell.CELL_TYPE_NUMERIC);
		cell.setCellValue(distanceToRoot);
		// Is root branch
		cell = row.createCell(11, Cell.CELL_TYPE_BOOLEAN);
		cell.setCellValue(isRootBranch);
	}

}
