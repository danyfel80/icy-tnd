package algorithms.danyfel80.topologicalnetworkdescription;

import java.util.List;
import java.util.PriorityQueue;

import javax.vecmath.Vector3d;

import icy.image.IcyBufferedImage;
import icy.sequence.Sequence;
import icy.sequence.SequenceUtil;
import icy.type.DataType;
import icy.type.point.Point5D;
import plugins.kernel.roi.roi2d.ROI2DPoint;

/**
 * This class performs a Dijkstra algorithm on an inverted distance map starting
 * from the seed points and as a result giving the cost function to go from any
 * point of the image to the seed.
 * 
 * @author Daniel Felipe Gonzalez Obando
 */
public class CostToSeedMapGenerator {

  public static class CostElement implements Comparable<CostElement> {
    private final double   cost;
    private final Point5D  point;
    private final Vector3d direction;

    public CostElement(double cost, Point5D point, Vector3d direction) {
      this.cost = cost;
      this.point = point;
      this.direction = direction;
    }

    public double getCost() {
      return cost;
    }

    public Point5D getPoint() {
      return point;
    }
    
    public Vector3d getDirection() {
    	return direction;
    }

    @Override
    public int compareTo(CostElement ce) {
      if (cost < ce.getCost())
        return -1;
      if (cost > ce.getCost())
        return 1;
      return 0;
    }

  }

  private Sequence         invertedDistanceMapSequence;
  private List<ROI2DPoint> seeds;
  private Sequence         result;
  private Sequence         minimumSpaningTree;
  private double           levelChangeWeightMultiplier;
  private double					 directionChangeWeightMultiplier;

  public CostToSeedMapGenerator(Sequence invertedDistanceMapSequence,
      List<ROI2DPoint> seeds, double levelChangeWeightMultiplier, 
      double directionChangeWeightMultiplier) {
    super();
    this.invertedDistanceMapSequence = invertedDistanceMapSequence;
    this.seeds = seeds;
    this.result = null;
    this.minimumSpaningTree = null;
    this.levelChangeWeightMultiplier = levelChangeWeightMultiplier;
    this.directionChangeWeightMultiplier = directionChangeWeightMultiplier;
  }

  public Sequence process() {
    int x, y, z, ysX;
    int sX = invertedDistanceMapSequence.getSizeX();
    int sY = invertedDistanceMapSequence.getSizeY();
    int sZ = invertedDistanceMapSequence.getSizeZ();

    // Construct initial result with costs -1
    result = new Sequence(
        invertedDistanceMapSequence.getName() + "_CostFunction(L = " + levelChangeWeightMultiplier + ")");
    minimumSpaningTree = new Sequence(
        invertedDistanceMapSequence.getName() + "_MinimumSpanningTree");
    invertedDistanceMapSequence = SequenceUtil
        .convertToType(invertedDistanceMapSequence, DataType.DOUBLE, false);

    result.beginUpdate();
    minimumSpaningTree.beginUpdate();
    try {
      for (z = 0; z < sZ; z++) {
        IcyBufferedImage tmpImage = new IcyBufferedImage(sX, sY, 1,
            DataType.DOUBLE);
        double[][] tmpImageData = tmpImage.getDataXYCAsDouble();
        for (x = 0; x < sX; x++) {
          for (y = 0; y < sY; y++) {
            tmpImageData[0][x + y * sX] = Double.POSITIVE_INFINITY;
          }
        }
        result.setImage(0, z, tmpImage);

        tmpImage = new IcyBufferedImage(sX, sY, 3, DataType.INT);
        minimumSpaningTree.setImage(0, z, tmpImage);
      }
      result.dataChanged();
      minimumSpaningTree.dataChanged();

      double[][][] distanceData = invertedDistanceMapSequence
          .getDataXYCZAsDouble(0);
      double[][][] resultData = result.getDataXYCZAsDouble(0);
      int[][][] mstData = minimumSpaningTree.getDataXYCZAsInt(0);
      boolean[][] visited = new boolean[sZ][sX*sY];

      int pX, pY, pZ, pT, pC;

      // Dijkstra algorithm using distances to center as costs
      PriorityQueue<CostElement> q = new PriorityQueue<CostElement>();
      for (ROI2DPoint roi2dPoint : seeds) {
        Point5D point = roi2dPoint.getPosition5D();
        pX = (int) point.getX();
        pY = (int) point.getY();
        pZ = (int) point.getZ();
        
        double cost = 0.0;
        Vector3d direction = new Vector3d(0, 0, 0);
        CostElement e = new CostElement(cost, point, direction);
        q.add(e);
        resultData[pZ][0][pX + pY * sX] = 0;
        mstData[pZ][0][pX + pY * sX] = pX;
        mstData[pZ][1][pX + pY * sX] = pY;
        mstData[pZ][2][pX + pY * sX] = pZ;
      }

      while (!q.isEmpty()) {
        CostElement ce = q.poll();
        Point5D cePoint = ce.getPoint();
        pX = (int) cePoint.getX();
        pY = (int) cePoint.getY();
        pZ = (int) cePoint.getZ();
        pT = (int) cePoint.getT();
        pC = (int) cePoint.getC();

        if (visited[pZ][pX+pY*sX])
        	continue;
        
        visited[pZ][pX+pY*sX] = true;
        
        double ceCost = ce.getCost();
        double ceHeight = 1.0/distanceData[pZ][0][pX+pY*sX];
        Vector3d ceDirection = ce.getDirection();
        
        // Check every neighbor of cePoint
        for (int dx = -1; dx <= 1; dx++) {
          x = pX + dx;
          for (int dy = -1; dy <= 1; dy++) {
            y = pY + dy;
            ysX = y * sX;
            for (int dz = -1; dz <= 1; dz++) {
              z = pZ + dz;
              if (dx == 0 && dy == 0 && dz == 0) {
                continue;
              }
              
              if (x >= 0 && x < sX && y >= 0 && y < sY && z >= 0 && z < sZ) {
                double currCost = resultData[z][0][x + ysX];
                double pHeight = distanceData[z][0][x + ysX];
                Vector3d pDirection = new Vector3d(dx, dy, dz);
                
                double heightDiff = Math.abs(ceHeight - 1.0/pHeight);
                double angleDiff = pDirection.angle(ceDirection)/Math.PI;
                angleDiff = ceDirection.length() > 0? angleDiff: 0.0;
                Vector3d direction = new Vector3d(pDirection);
                direction.scale(directionChangeWeightMultiplier*angleDiff);
                direction.add(ceDirection);
                
                double cost = ceCost + pHeight * (1.0+levelChangeWeightMultiplier*heightDiff) * (pDirection.length());
                if (cost < currCost) {
                  Point5D point = new Point5D.Double(x, y, z, pT, pC);
                  
                  CostElement e = new CostElement(cost, point, direction);
                  q.add(e);
                  resultData[z][0][x + ysX] = cost;
                  mstData[z][0][x + ysX] = pX;
                  mstData[z][1][x + ysX] = pY;
                  mstData[z][2][x + ysX] = pZ;
                }
              }
            }
          }
        }

      }
    }
    finally {
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
