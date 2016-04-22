package algorithms.danyfel80.networktopology.analysis;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.vecmath.Point2d;
import javax.vecmath.Point3i;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.BreadthFirstIterator;

import icy.sequence.Sequence;

/**
 * Graph analysis class. Provides analysis functions for graph structures.
 * 
 * @author Daniel Felipe Gonzalez Obando
 */
public class GraphAnalysis {

	public static Map<Point3i, Integer> getVertexDepthMap(
	    DirectedGraph<Point3i, DefaultEdge> graph, List<Point3i> seedPoints) {
		Map<Point3i, Integer> depths = new HashMap<>();
		for (Point3i seed : seedPoints) {
			Map<Point3i, Integer> tmpDpths = getVertexDepthMap(graph, seed);
			depths.putAll(tmpDpths);
		}
		return depths;
	}
	
	/**
	 * Computes the depth of all the nodes in the graph with respect to the given
	 * root.
	 * 
	 * @param g
	 *          Graph
	 * @param root
	 *          Root node
	 * @return Map with all the children nodes of the given root associating their
	 *         depth.
	 */
	public static Map<Point3i, Integer> getVertexDepthMap(
	    DirectedGraph<Point3i, DefaultEdge> g, Point3i root) {
		if (!g.containsVertex(root))
			throw new IllegalArgumentException("The root point " + root + " is not present in the graph.");

		Map<Point3i, Integer> depths = new HashMap<>(g.vertexSet().size());
		int depth;
		
		depths.put(root, g.outgoingEdgesOf(root).size()-1);
		
		BreadthFirstIterator<Point3i, DefaultEdge> it = new BreadthFirstIterator<>(g, root);
		
		// BFS
		while (it.hasNext()) {
			Point3i n = (Point3i) it.next();
			depth = depths.get(n);
			System.out.println(n);
			Set<DefaultEdge> children = g.outgoingEdgesOf(n);
			for (DefaultEdge chE : children) {
				depths.put(g.getEdgeTarget(chE), depth+1);
			}
		}
		return depths;
	}

	/**
	 * Computes the distance from a point in the sequence to the minimum spanning
	 * tree root.
	 * 
	 * @param mst
	 *          Minimum spanning tree sequence.
	 * @param node
	 *          Point in the sequence.
	 * @return Length of the path from the node to the minimum spanning tree root.
	 */
	public static double getNodeDistanceToRoot(
	    Sequence mst, Point3i node) {
		int[][][] mstData = mst.getDataXYCZAsInt(0);
		int sx = mst.getSizeX();
		double distance = 0;

		Point3i n = node;
		Point3i p =
		    new Point3i(mstData[n.z][0][n.x + n.y * sx], mstData[n.z][1][n.x + n.y * sx], mstData[n.z][2][n.x + n.y * sx]);

		while (!n.equals(p)) {
			Point3i diff = new Point3i();
			diff.sub(n, p);
			diff.absolute();
			distance += Math.sqrt(diff.x * diff.x + diff.y * diff.y + diff.z * diff.z);
			n.set(p);
			p.set(mstData[n.z][0][n.x + n.y * sx], mstData[n.z][1][n.x + n.y * sx], mstData[n.z][2][n.x + n.y * sx]);
		}
		return distance;
	}

	/**
	 * Computes the yaw and pitch of the edge from source to target. These angles
	 * are calculated with the Cartesian axes.
	 * 
	 * @param g
	 *          Graph
	 * @param e
	 *          Edge to compute its angles.
	 * @return Pair of values (Yaw and Pitch) in radians.
	 */
	public static Point2d getBranchOrientation(
	    DirectedGraph<Point3i, DefaultEdge> g, DefaultEdge e) {
		Point3i p1, p2, p3 = new Point3i();
		p1 = g.getEdgeSource(e);
		p2 = g.getEdgeTarget(e);
		p3.sub(p2, p1);

		Point2d res = new Point2d();
		// Yaw
		res.x = Math.atan2(p3.z, p3.x) - Math.PI;
		// Pitch
		res.y = -Math.atan2(p3.y, Math.sqrt(p3.x * p3.x + p3.z * p3.z));

		return res;
	}

	
}
