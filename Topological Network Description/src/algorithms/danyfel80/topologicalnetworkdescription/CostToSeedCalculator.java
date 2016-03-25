/**
 * 
 */
package algorithms.danyfel80.topologicalnetworkdescription;

import java.util.List;
import java.util.PriorityQueue;

import icy.image.IcyBufferedImage;
import icy.sequence.Sequence;
import icy.sequence.SequenceUtil;
import icy.type.DataType;
import icy.type.point.Point5D;
import plugins.kernel.roi.roi2d.ROI2DPoint;

/**
 * This class performs a Dijkstra algorithm on an inverted distance map starting from the seed points and as a 
 * result giving the cost function to go from any point of the image to the seed.
 * @author Daniel Felipe Gonzalez Obando
 */
public class CostToSeedCalculator {

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

    @Override
    public int compareTo(CostElement ce) {
      if (cost < ce.getCost()) return -1;
      if (cost > ce.getCost()) return 1;
      return 0;
    }

  }

  private Sequence invertedDistanceMapSequence;
  private List<ROI2DPoint> seeds;
  private Sequence result;
  private Sequence minimumSpaningTree;

  public CostToSeedCalculator(Sequence invertedDistanceMapSequence, List<ROI2DPoint> seeds) {
    super();
    this.invertedDistanceMapSequence = invertedDistanceMapSequence;
    this.seeds = seeds;
    this.result = null;
    this.minimumSpaningTree = null;
  }

  public Sequence process() {
    int sizeX = invertedDistanceMapSequence.getSizeX();
    int sizeY = invertedDistanceMapSequence.getSizeY();
    int sizeZ = invertedDistanceMapSequence.getSizeZ();
    

    // Construct initial result with costs -1
    result = new Sequence(invertedDistanceMapSequence.getName() + "_CostFunction");
    minimumSpaningTree = new Sequence(invertedDistanceMapSequence.getName() + "_MinimumSpanningTree");
    invertedDistanceMapSequence = SequenceUtil.convertToType(invertedDistanceMapSequence, DataType.DOUBLE, false);
    
    result.beginUpdate();
    minimumSpaningTree.beginUpdate();
    try {
      for (int z = 0; z < sizeZ; z++) {
        IcyBufferedImage tmpImage = new IcyBufferedImage(sizeX, sizeY, 1, DataType.DOUBLE);
        double[][] tmpImageData = tmpImage.getDataXYCAsDouble();
        for (int x = 0; x < sizeX; x++) {
          for (int y = 0; y < sizeY; y++) {
            tmpImageData[0][x + y*sizeX] = Double.POSITIVE_INFINITY;
          }
        }
        result.setImage(0, z, tmpImage);

        tmpImage = new IcyBufferedImage(sizeX, sizeY, 3, DataType.INT);
        minimumSpaningTree.setImage(0, z, tmpImage);
      }
      result.dataChanged();
      minimumSpaningTree.dataChanged();

      double[][][] distanceData = invertedDistanceMapSequence.getDataXYCZAsDouble(0);
      double[][][] resultData = result.getDataXYCZAsDouble(0);
      int[][][] mstData = minimumSpaningTree.getDataXYCZAsInt(0);

      int pX, pY, pZ, pT, pC;

      // Dijkstra algorithm using distances to center as costs
      PriorityQueue<CostElement> q = new PriorityQueue<CostElement>();
      for (ROI2DPoint roi2dPoint : seeds) {
        Point5D point = roi2dPoint.getPosition5D();
        pX = (int)point.getX();
        pY = (int)point.getY();
        pZ = (int)point.getZ();
        double cost = 0.0;
        CostElement e = new CostElement(cost, point);
        q.add(e);
        resultData[pZ][0][pX + pY * sizeX] = 0;
        mstData[pZ][0][pX + pY * sizeX] = pX;
        mstData[pZ][1][pX + pY * sizeX] = pY;
        mstData[pZ][2][pX + pY * sizeX] = pZ;
      }

      while (!q.isEmpty()) {
        CostElement ce = q.poll();
        Point5D cePoint = ce.getPoint();
        pX = (int)cePoint.getX();
        pY = (int)cePoint.getY();
        pZ = (int)cePoint.getZ();
        pT = (int)cePoint.getT();
        pC = (int)cePoint.getC();

        double ceCost = ce.getCost();

        // Check every neighbor of cePoint
        for (int i = -1; i <= 1; i++) {
          for (int j = -1; j <= 1; j++) {
            for (int k = -1; k <= 1; k++) {
              if (i == 0 && j == 0 && k == 0) continue;

              int x = pX + i;
              int y = pY + j;
              int z = pZ + k;
              if (x >= 0 && x < sizeX &&
                  y >= 0 && y < sizeY &&
                  z >= 0 && z < sizeZ) {
                double val = resultData[z][0][x + y*sizeX];
                double costVal = distanceData[z][0][x + y*sizeX];
                if (ceCost+costVal < val) {
                  Point5D point = new Point5D.Double(x, y, z, pT, pC);
                  double cost = ceCost+costVal;
                  CostElement e = new CostElement(cost, point);
                  q.add(e);
                  resultData[z][0][x + y*sizeX] = cost;
                  mstData[z][0][x + y*sizeX] = pX;
                  mstData[z][1][x + y*sizeX] = pY;
                  mstData[z][2][x + y*sizeX] = pZ;
                }
              }
            }
          }
        }


      }
    } finally {
      result.dataChanged();
      result.endUpdate();
      minimumSpaningTree.dataChanged();
      minimumSpaningTree.endUpdate();
    }
    return result;
  }

  /**
   * @return the result
   */
  public Sequence getResult() {
    return result;
  }

  /**
   * @return the minimumSpaningTree
   */
  public Sequence getMinimumSpaningTree() {
    return minimumSpaningTree;
  }
}
