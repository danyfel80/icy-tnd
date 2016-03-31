/**
 * 
 */
package algorithms.danyfel80.topologicalnetworkdescription;

import java.util.PriorityQueue;

import javax.vecmath.Point3i;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

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
public class NetworkDescriptionConstructor {

  public static class CostElement implements Comparable<CostElement>
  {
    private final Double cost;
    private final Point5D point;

    public CostElement(Double cost, Point5D point)
    {
      this.cost  = cost;
      this.point = point;
    }

    public Double getCost()   { return cost; }
    public Point5D getPoint() { return point; }

    // inverted to use max in priority queue
    @Override
    public int compareTo(CostElement ce) {
      if (cost > ce.getCost()) return -1;
      if (cost < ce.getCost()) return 1;
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

  public NetworkDescriptionConstructor(Sequence endnessSequence, Sequence minimumSpaningTree, Sequence squaredDistanceMap) {
    this.endnessSequence = endnessSequence;
    this.minimumSpanningTree = minimumSpaningTree;
    this.distanceMap = squaredDistanceMap;

    this.labelSequence = null;
    this.endPointSequence = null;
    this.branchSequence = null;
    this.skeletonSequence = null;

    this.graph = null;
  }

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
    for (int z = 0; z < sizeZ; z++) {
      for (int x = 0; x < sizeX; x++) {
        for (int y = 0; y < sizeY; y++) {
          if (endnessData[z][0][x+y*sizeX] > 0) {
            Point5D point = new Point5D.Double(x, y, z, 0, 0);
            CostElement ce = new CostElement(endnessData[z][0][x+y*sizeX], point);
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
    
    while (!q.isEmpty()) {
      
      CostElement ce = q.remove();
      int ceX = (int)ce.getPoint().getX();
      int ceY = (int)ce.getPoint().getY();
      int ceZ = (int)ce.getPoint().getZ();

      if (labelData[ceZ][0][ceX+ceY*sizeX] == 0) {
        //Treat not marked element ce
        
        // Mark neighbor sphere
        markPoint(labelData, distanceData, sizeX, sizeY, sizeZ, ceX, ceY, ceZ, branchId);
        
        endPointData[ceZ][0][ceX+ceY*sizeX] = (byte)DataType.UBYTE.getMaxValue();
        skeletonData[ceZ][0][ceX+ceY*sizeX] = (byte)DataType.UBYTE.getMaxValue();

        boolean addNewPoints = true;
        int pX = parentData[ceZ][0][ceX+ceY*sizeX];
        int pY = parentData[ceZ][1][ceX+ceY*sizeX];
        int pZ = parentData[ceZ][2][ceX+ceY*sizeX];
        do {
          ceX = pX;
          ceY = pY;
          ceZ = pZ;
          pX = parentData[ceZ][0][ceX+ceY*sizeX];
          pY = parentData[ceZ][1][ceX+ceY*sizeX];
          pZ = parentData[ceZ][2][ceX+ceY*sizeX];

          if (addNewPoints) {
            if (TypeUtil.unsign(skeletonData[ceZ][0][ceX+ceY*sizeX]) == 0) {
              labelData[ceZ][0][ceX+ceY*sizeX] = branchId;
              // Mark neighbor sphere
              markPoint(labelData, distanceData, sizeX, sizeY, sizeZ, ceX, ceY, ceZ, branchId);
              
              skeletonData[ceZ][0][ceX+ceY*sizeX] = (byte)DataType.UBYTE.getMaxValue();
            }
            else {
              branchData[ceZ][0][ceX+ceY*sizeX] = (byte)DataType.UBYTE.getMaxValue();
              branchId++;
              addNewPoints = false;
            }
          }
          else {
            if (TypeUtil.unsign(branchData[ceZ][0][ceX+ceY*sizeX]) == 0) {
              labelData[ceZ][0][ceX+ceY*sizeX] = branchId;
              // mark neighbor sphere
              markPoint(labelData, distanceData, sizeX, sizeY, sizeZ, ceX, ceY, ceZ, branchId);
              
            }
            else {
              branchId++;
            }
          }

        } while (ceX != pX || ceY != pY || ceZ != pZ);

      }
    }


    labelSequence.endUpdate();
    endPointSequence.endUpdate();
    branchSequence.endUpdate();
    skeletonSequence.endUpdate();

    return skeletonSequence;
  }

  private void markPoint(int[][][] labelData, int[][][] distanceData, int sizeX, int sizeY, int sizeZ, int ceX, int ceY, int ceZ,
      int branchId) {

    double rSize = 1.5;
    
    int rs = (int) Math.ceil(Math.sqrt((distanceData[ceZ][0][ceX+ceY*sizeX] > 4)? distanceData[ceZ][0][ceX+ceY*sizeX]: 4)*rSize);
    rs*=rs;
    int r = (int) Math.ceil(Math.sqrt(rs));
    for (int x = 0; x <= r; x++) {
      for (int y = 0; y <= r; y++) {
        for (int z = 0; z <= r; z++) {
          if (x*x+y*y+z*z <= rs) {
            if (ceX+x < sizeX && ceY+y < sizeY && ceZ+z < sizeZ &&
                labelData[ceZ+z][0][ceX+x+(ceY+y)*sizeX] == 0)
              labelData[ceZ+z][0][ceX+x+(ceY+y)*sizeX] = branchId;
            if (ceX+x < sizeX && ceY-y >= 0 && ceZ+z < sizeZ &&
                labelData[ceZ+z][0][ceX+x+(ceY-y)*sizeX] == 0)
              labelData[ceZ+z][0][ceX+x+(ceY-y)*sizeX] = branchId;
            if (ceX-x >= 0 && ceY+y < sizeY && ceZ+z < sizeZ &&
                labelData[ceZ+z][0][ceX-x+(ceY+y)*sizeX] == 0)
              labelData[ceZ+z][0][ceX-x+(ceY+y)*sizeX] = branchId;
            if (ceX-x >= 0 && ceY-y >= 0 && ceZ+z < sizeZ &&
                labelData[ceZ+z][0][ceX-x+(ceY-y)*sizeX] == 0)
              labelData[ceZ+z][0][ceX-x+(ceY-y)*sizeX] = branchId;
            if (ceX+x < sizeX && ceY+y < sizeY && ceZ-z >= 0 &&
                labelData[ceZ-z][0][ceX+x+(ceY+y)*sizeX] == 0)
              labelData[ceZ-z][0][ceX+x+(ceY+y)*sizeX] = branchId;
            if (ceX+x < sizeX && ceY-y >= 0 && ceZ-z >= 0 &&
                labelData[ceZ-z][0][ceX+x+(ceY-y)*sizeX] == 0)
              labelData[ceZ-z][0][ceX+x+(ceY-y)*sizeX] = branchId;
            if (ceX-x >= 0 && ceY+y < sizeY && ceZ-z >= 0 &&
                labelData[ceZ-z][0][ceX-x+(ceY+y)*sizeX] == 0)
              labelData[ceZ-z][0][ceX-x+(ceY+y)*sizeX] = branchId;
            if (ceX-x >= 0 && ceY-y >= 0 && ceZ-z >= 0 &&
                labelData[ceZ-z][0][ceX-x+(ceY-y)*sizeX] == 0)
              labelData[ceZ-z][0][ceX-x+(ceY-y)*sizeX] = branchId;
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
          for (int xy = 0; xy < sizeX*sizeY; xy++) {
            tempData[0][xy] = (TypeUtil.unsign(skeletonData[z][0][xy]) == 0? 0: labelData[z][0][xy]);
          }
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
    int sizeX = endPointSequence.getSizeX();
    int sizeY = endPointSequence.getSizeY();
    int sizeZ = endPointSequence.getSizeZ();


    for (int z = 0; z < sizeZ; z++) {
      for (int x = 0; x < sizeX; x++) {
        for (int y = 0; y < sizeY; y++) {
          if (TypeUtil.unsign(endPointData[z][0][x+y*sizeX]) != 0) {
            Point3i p = new Point3i(x,y,z);
            Point3i b = new Point3i(x,y,z);

            int ppx = mstData[p.z][0][p.x+ p.y*sizeX];
            int ppy = mstData[p.z][1][p.x+ p.y*sizeX];
            int ppz = mstData[p.z][2][p.x+ p.y*sizeX];
            Point3i pp = new Point3i(ppx, ppy, ppz);

            while (!p.equals(pp)) {
              if (TypeUtil.unsign(branchData[pp.z][0][pp.x + pp.y*sizeX]) != 0) {
                graph.addVertex(pp);
                graph.addVertex(b);
                graph.addEdge(pp, b);
                b = pp;
              }
              p = pp;
              ppx = mstData[p.z][0][p.x+ p.y*sizeX];
              ppy = mstData[p.z][1][p.x+ p.y*sizeX];
              ppz = mstData[p.z][2][p.x+ p.y*sizeX];
              pp = new Point3i(ppx, ppy, ppz);
            }
          }
        }
      }
    }

    return this.graph;
  }
}
