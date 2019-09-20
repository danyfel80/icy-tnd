/**
 * 
 */
package algorithms.danyfel80.topologicalnetworkdescription;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

import javax.vecmath.Point3i;
import javax.vecmath.Vector3d;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.DepthFirstIterator;

import icy.image.IcyBufferedImage;
import icy.sequence.Sequence;
import icy.sequence.SequenceUtil;
import icy.type.DataType;
import icy.type.TypeUtil;
import icy.type.point.Point5D;

/**
 * @author Daniel Felipe Gonzalez Obando
 *
 */
public class TopologicalNetworkDescriptor {

	public static class CostElement implements Comparable<CostElement> {
		private final Double cost;
		private final Point5D point;

		public CostElement(Double cost, Point5D point) {
			this.cost = cost;
			this.point = point;
		}

		public Double getCost() {
			return cost;
		}

		public Point5D getPoint() {
			return point;
		}

		// inverted to use max in priority queue
		@Override
		public int compareTo(CostElement ce) {
			if (cost > ce.getCost())
				return -1;
			if (cost < ce.getCost())
				return 1;
			return 0;
		}

	}

	private Sequence endnessSequence;
	private Sequence minimumSpanningTree;
	private Sequence distanceMap;

	private Sequence labelSequence;
	private Sequence endPointSequence;
	private Sequence branchSequence;
	private Sequence skeletonSequence;
	private Sequence labeledSkeletonSequence;
	private DirectedGraph<Point3i, DefaultEdge> graph;
	private Set<Point3i> seeds;

	private int minLabelingSphereRadius;
	private double radiusScale = 2.5;

	public TopologicalNetworkDescriptor(Sequence endnessSequence, Sequence minimumSpaningTree,
	    Sequence squaredDistanceMap, int minLabelingSphereRadius, double radiusScale) {
		this.endnessSequence = endnessSequence;
		this.minimumSpanningTree = minimumSpaningTree;
		this.distanceMap = squaredDistanceMap;

		this.labelSequence = null;
		this.endPointSequence = null;
		this.branchSequence = null;
		this.skeletonSequence = null;

		this.graph = null;
		
		this.minLabelingSphereRadius = minLabelingSphereRadius;
		this.radiusScale = radiusScale;
	}

	/**
	 * @return skeleton.
	 */
	public Sequence process() {

		labelSequence = new Sequence(minimumSpanningTree.getName() + "_Labels");
		endPointSequence = new Sequence(minimumSpanningTree.getName() + "_EndPoints");
		branchSequence = new Sequence(minimumSpanningTree.getName() + "_Branches");
		skeletonSequence = new Sequence(minimumSpanningTree.getName() + "_Skeleton");

		if (!endnessSequence.getDataType_().equals(DataType.DOUBLE))
			SequenceUtil.convertToType(endnessSequence, DataType.DOUBLE, false);
		if (!minimumSpanningTree.getDataType_().equals(DataType.INT))
			SequenceUtil.convertToType(minimumSpanningTree, DataType.INT, false);
		if (!distanceMap.getDataType_().equals(DataType.INT))
			SequenceUtil.convertToType(distanceMap, DataType.INT, false);

		double[][][] endnessData = endnessSequence.getDataXYCZAsDouble(0);
		int[][][] parentData = minimumSpanningTree.getDataXYCZAsInt(0);
		int[][][] distanceData = distanceMap.getDataXYCZAsInt(0);
		int sizeX = endnessSequence.getSizeX();
		int sizeY = endnessSequence.getSizeY();
		int sizeZ = endnessSequence.getSizeZ();

		PriorityQueue<CostElement> q = new PriorityQueue<>();

		// Fill queue
		Point5D point;
		CostElement ce;
		for (int z = 0; z < sizeZ; z++) {
			for (int x = 0; x < sizeX; x++) {
				for (int y = 0; y < sizeY; y++) {
					if (endnessData[z][0][x + y * sizeX] > 0) {
						point = new Point5D.Double(x, y, z, 0, 0);
						ce = new CostElement(endnessData[z][0][x + y * sizeX], point);
						q.add(ce);
					}
				}
			}
		}

		// Create Containers l, e, y, s
		labelSequence.beginUpdate();
		endPointSequence.beginUpdate();
		branchSequence.beginUpdate();
		skeletonSequence.beginUpdate();

		for (int z = 0; z < sizeZ; z++) {
			IcyBufferedImage tempLImage = new IcyBufferedImage(sizeX, sizeY, 1, DataType.INT);
			IcyBufferedImage tempEImage = new IcyBufferedImage(sizeX, sizeY, 1, DataType.UBYTE);
			IcyBufferedImage tempYImage = new IcyBufferedImage(sizeX, sizeY, 1, DataType.UBYTE);
			IcyBufferedImage tempSImage = new IcyBufferedImage(sizeX, sizeY, 1, DataType.UBYTE);
			labelSequence.setImage(0, z, tempLImage);
			endPointSequence.setImage(0, z, tempEImage);
			branchSequence.setImage(0, z, tempYImage);
			skeletonSequence.setImage(0, z, tempSImage);
		}
		labelSequence.dataChanged();
		endPointSequence.dataChanged();
		branchSequence.dataChanged();
		skeletonSequence.dataChanged();

		int[][][] labelData = labelSequence.getDataXYCZAsInt(0);
		byte[][][] endPointData = endPointSequence.getDataXYCZAsByte(0);
		byte[][][] branchData = branchSequence.getDataXYCZAsByte(0);
		byte[][][] skeletonData = skeletonSequence.getDataXYCZAsByte(0);

		int branchId = 1;

		int ceX, ceY, ceZ, pX, pY, pZ;

		while (!q.isEmpty()) {

			ce = q.remove();
			ceX = (int) ce.getPoint().getX();
			ceY = (int) ce.getPoint().getY();
			ceZ = (int) ce.getPoint().getZ();

			if (labelData[ceZ][0][ceX + ceY * sizeX] == 0) {
				// Treat not marked element ce

				// Mark neighbor sphere
				markPoint(labelData, distanceData, sizeX, sizeY, sizeZ, ceX, ceY, ceZ, branchId);

				endPointData[ceZ][0][ceX + ceY * sizeX] = (byte) DataType.UBYTE.getMaxValue();
				skeletonData[ceZ][0][ceX + ceY * sizeX] = (byte) DataType.UBYTE.getMaxValue();

				boolean addNewPoints = true;
				pX = parentData[ceZ][0][ceX + ceY * sizeX];
				pY = parentData[ceZ][1][ceX + ceY * sizeX];
				pZ = parentData[ceZ][2][ceX + ceY * sizeX];
				do {
					ceX = pX;
					ceY = pY;
					ceZ = pZ;
					pX = parentData[ceZ][0][ceX + ceY * sizeX];
					pY = parentData[ceZ][1][ceX + ceY * sizeX];
					pZ = parentData[ceZ][2][ceX + ceY * sizeX];

					if (addNewPoints) {
						if (TypeUtil.unsign(skeletonData[ceZ][0][ceX + ceY * sizeX]) == 0) {
							labelData[ceZ][0][ceX + ceY * sizeX] = branchId;
							// Mark neighbor sphere
							markPoint(labelData, distanceData, sizeX, sizeY, sizeZ, ceX, ceY, ceZ, branchId);

							skeletonData[ceZ][0][ceX + ceY * sizeX] = (byte) DataType.UBYTE.getMaxValue();
						} else {
							branchData[ceZ][0][ceX + ceY * sizeX] = (byte) DataType.UBYTE.getMaxValue();
							branchId++;
							addNewPoints = false;
						}
					} else {
						if (TypeUtil.unsign(branchData[ceZ][0][ceX + ceY * sizeX]) == 0) {
							labelData[ceZ][0][ceX + ceY * sizeX] = branchId;
							// mark neighbor sphere
							markPoint(labelData, distanceData, sizeX, sizeY, sizeZ, ceX, ceY, ceZ, branchId);

						} else {
							branchId++;
						}
					}

				} while (ceX != pX || ceY != pY || ceZ != pZ);
				branchId++;
			}
		}

		labelSequence.endUpdate();
		endPointSequence.endUpdate();
		branchSequence.endUpdate();
		skeletonSequence.endUpdate();

		return skeletonSequence;
	}

	/**
	 * Creates a sphere with value branchId in the labelData of a radius equal to
	 * the value in distanceData at pX, pY, pZ.
	 * 
	 * @param labelData
	 * @param distanceData
	 * @param sX
	 * @param sY
	 * @param sZ
	 * @param pX
	 * @param pY
	 * @param pZ
	 * @param branchId
	 */
	private void markPoint(int[][][] labelData, int[][][] distanceData, int sX, int sY, int sZ, int pX, int pY, int pZ,
	    int branchId) {

		int x, y, z, r, rs, x2, y2;

		//double rScale = 2.5;

		double rTemp = Math.ceil(Math.sqrt(distanceData[pZ][0][pX + pY * sX]));
		r = (rTemp < minLabelingSphereRadius) ? minLabelingSphereRadius : (int) rTemp;
		r *= radiusScale;
		rs = r * r;
		// int rs = (int) Math.ceil(Math.sqrt((distanceData[ceZ][0][ceX+ceY*sizeX] >
		// 4)? distanceData[ceZ][0][ceX+ceY*sizeX]: 4)*rScale);
		// rs*=rs;
		// int r = (int) Math.ceil(Math.sqrt(rs));
		for (x = 0; x <= r; x++) {
			x2 = x * x;
			for (y = 0; y <= r; y++) {
				y2 = y * y;
				for (z = 0; (sZ > 1) ? z <= r : z < 1; z++) {
					if (x2 + y2 + z * z <= rs) {
						if (pX + x < sX && pY + y < sY && pZ + z < sZ && labelData[pZ + z][0][pX + x + (pY + y) * sX] == 0)
							labelData[pZ + z][0][pX + x + (pY + y) * sX] = branchId;
						if (pX + x < sX && pY - y >= 0 && pZ + z < sZ && labelData[pZ + z][0][pX + x + (pY - y) * sX] == 0)
							labelData[pZ + z][0][pX + x + (pY - y) * sX] = branchId;
						if (pX - x >= 0 && pY + y < sY && pZ + z < sZ && labelData[pZ + z][0][pX - x + (pY + y) * sX] == 0)
							labelData[pZ + z][0][pX - x + (pY + y) * sX] = branchId;
						if (pX - x >= 0 && pY - y >= 0 && pZ + z < sZ && labelData[pZ + z][0][pX - x + (pY - y) * sX] == 0)
							labelData[pZ + z][0][pX - x + (pY - y) * sX] = branchId;
						if (pX + x < sX && pY + y < sY && pZ - z >= 0 && labelData[pZ - z][0][pX + x + (pY + y) * sX] == 0)
							labelData[pZ - z][0][pX + x + (pY + y) * sX] = branchId;
						if (pX + x < sX && pY - y >= 0 && pZ - z >= 0 && labelData[pZ - z][0][pX + x + (pY - y) * sX] == 0)
							labelData[pZ - z][0][pX + x + (pY - y) * sX] = branchId;
						if (pX - x >= 0 && pY + y < sY && pZ - z >= 0 && labelData[pZ - z][0][pX - x + (pY + y) * sX] == 0)
							labelData[pZ - z][0][pX - x + (pY + y) * sX] = branchId;
						if (pX - x >= 0 && pY - y >= 0 && pZ - z >= 0 && labelData[pZ - z][0][pX - x + (pY - y) * sX] == 0)
							labelData[pZ - z][0][pX - x + (pY - y) * sX] = branchId;
					}
				}
			}
		}
	}

	/**
	 * @return the labelSequence
	 */
	public Sequence getLabelSequence() {
		return labelSequence;
	}

	/**
	 * @return the endPointSequence
	 */
	public Sequence getEndPointSequence() {
		return endPointSequence;
	}

	/**
	 * @return the branchSequence
	 */
	public Sequence getBranchSequence() {
		return branchSequence;
	}

	/**
	 * @return the skeletonSequence
	 */
	public Sequence getSkeletonSequence() {
		return skeletonSequence;
	}

	public Sequence getLabeledSkeletonSequence() {
		if (skeletonSequence != null && labelSequence != null) {
			if (labeledSkeletonSequence == null) {
				labeledSkeletonSequence = new Sequence(minimumSpanningTree.getName() + "_LabeledSkeleton");

				int sizeX = endnessSequence.getSizeX();
				int sizeY = endnessSequence.getSizeY();
				int sizeZ = endnessSequence.getSizeZ();

				int[][][] labelData = labelSequence.getDataXYCZAsInt(0);
				byte[][][] skeletonData = skeletonSequence.getDataXYCZAsByte(0);

				labeledSkeletonSequence.beginUpdate();

				for (int z = 0; z < sizeZ; z++) {
					IcyBufferedImage tempLSImage = new IcyBufferedImage(sizeX, sizeY, 1, DataType.INT);
					int[][] tempData = tempLSImage.getDataXYCAsInt();
					for (int xy = 0; xy < sizeX * sizeY; xy++) {
						tempData[0][xy] = (TypeUtil.unsign(skeletonData[z][0][xy]) == 0 ? 0 : labelData[z][0][xy]);
					}
					tempLSImage.dataChanged();
					labeledSkeletonSequence.setImage(0, z, tempLSImage);
				}
				labeledSkeletonSequence.endUpdate();
			}
		}
		return labeledSkeletonSequence;
	}

	public DirectedGraph<Point3i, DefaultEdge> getGraph() {
		if (graph != null)
			return graph;

		this.graph = new DefaultDirectedGraph<Point3i, DefaultEdge>(DefaultEdge.class);

		byte[][][] endPointData = endPointSequence.getDataXYCZAsByte(0);
		byte[][][] branchData = branchSequence.getDataXYCZAsByte(0);
		int[][][] mstData = minimumSpanningTree.getDataXYCZAsInt(0);
		int[][][] distanceData = distanceMap.getDataXYCZAsInt(0);
		int sizeX = endPointSequence.getSizeX();
		int sizeY = endPointSequence.getSizeY();
		int sizeZ = endPointSequence.getSizeZ();

		seeds = new HashSet<Point3i>();

		for (int z = 0; z < sizeZ; z++) {
			for (int x = 0; x < sizeX; x++) {
				for (int y = 0; y < sizeY; y++) {
					if (TypeUtil.unsign(endPointData[z][0][x + y * sizeX]) != 0) {
						Point3i child = new Point3i(x, y, z);
						Point3i node = new Point3i(x, y, z);
						

						int parentx = mstData[node.z][0][node.x + node.y * sizeX];
						int parenty = mstData[node.z][1][node.x + node.y * sizeX];
						int parentz = mstData[node.z][2][node.x + node.y * sizeX];
						Point3i parent = new Point3i(parentx, parenty, parentz);

						while (!node.equals(parent)) {
							if (TypeUtil.unsign(branchData[parent.z][0][parent.x + parent.y * sizeX]) != 0) {
								graph.addVertex(parent);
								graph.addVertex(child);
								graph.addEdge(parent, child);
								child = parent;
							}
							node = parent;
							parentx = mstData[node.z][0][node.x + node.y * sizeX];
							parenty = mstData[node.z][1][node.x + node.y * sizeX];
							parentz = mstData[node.z][2][node.x + node.y * sizeX];
							parent = new Point3i(parentx, parenty, parentz);
						}
						seeds.add(node);
						
						if (!parent.equals(child)) {
							graph.addVertex(parent);
							graph.addVertex(child);
							graph.addEdge(parent, child);
						}
					}
				}
			}
		}
		
		// Filter fake small edges.
		List<DefaultEdge> edgesToDelete = new ArrayList<DefaultEdge>();
		for (Point3i seed : seeds) {
			DepthFirstIterator<Point3i, DefaultEdge> it = new DepthFirstIterator<Point3i, DefaultEdge>(graph, seed);
			while(it.hasNext()) {
				Point3i parent = it.next();
				Set<DefaultEdge> children = graph.outgoingEdgesOf(parent);
				for (DefaultEdge childE : children) {
					Point3i child = graph.getEdgeTarget(childE);
					Vector3d v = new Vector3d(child.x - parent.x, child.y - parent.y, child.z - parent.z);
					if (v.length() * v.length() < distanceData[parent.z][0][parent.x + parent.y*sizeX]) {
						edgesToDelete.add(childE);
					}
				}
			}
		}
		Collections.reverse(edgesToDelete);
		for (DefaultEdge edge : edgesToDelete) {
			Point3i parent = graph.getEdgeSource(edge);
			Point3i child = graph.getEdgeTarget(edge);
			Set<DefaultEdge> children = graph.outgoingEdgesOf(child);
			for (DefaultEdge childEdge : children) {
				graph.addEdge(parent, graph.getEdgeTarget(childEdge));
			}
			graph.removeVertex(child);
		}
		

		return this.graph;
	}

	/**
	 * @return Seed points
	 */
	public List<Point3i> getSeedPoints() {
		return new ArrayList<Point3i>(seeds);
	}
}
