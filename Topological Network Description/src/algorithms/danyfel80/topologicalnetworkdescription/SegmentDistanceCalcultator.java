package algorithms.danyfel80.topologicalnetworkdescription;

import icy.image.IcyBufferedImage;
import icy.sequence.Sequence;
import icy.type.DataType;
import plugins.vannary.morphomaths.MorphOp;

/**
 * This class creates a distance map of a segmented image.
 * @author Daniel Felipe Gonzalez Obando
 */
public class SegmentDistanceCalcultator {

  private Sequence segmentedSequence;
  private Sequence distanceMap;
  private Sequence squaredDistanceMap;
  private Sequence invertedSquaredDistanceMap;

  public SegmentDistanceCalcultator(Sequence segmentedSequence) {
    this.segmentedSequence = segmentedSequence;
    this.squaredDistanceMap = null;
    this.distanceMap = null;
    this.invertedSquaredDistanceMap = null;
  }



  /**
   * @return the distance map
   */
  public Sequence getDistanceMap() {
    return distanceMap;
  }

  /**
   * @return the distanceMap
   */
  public Sequence getSquaredDistanceMap() {
    return squaredDistanceMap;
  }



  public Sequence process() {
    MorphOp mop = new MorphOp();
    distanceMap = new Sequence(segmentedSequence.getName() + "_DistanceMap");
    distanceMap.copyDataFrom(segmentedSequence);
    
    if (distanceMap.getSizeZ() == 1) {
      mop.distanceMap2D(distanceMap, 0, 127, true);
    } else {
      mop.distanceMap3D(distanceMap, 127, true);
    }
    
    distanceMap.dataChanged();
    
    double[][][] distXYZData = distanceMap.getDataXYCZAsDouble(0);
    
    int sizeX = segmentedSequence.getSizeX();
    int sizeY = segmentedSequence.getSizeY();
    int sizeZ = segmentedSequence.getSizeZ();
    
    squaredDistanceMap = new Sequence(segmentedSequence.getName() + "_SquaredDistanceMap");
    squaredDistanceMap.beginUpdate();
    for (int z = 0; z < sizeZ; z++) {
      IcyBufferedImage dMapSlice = new IcyBufferedImage(sizeX, sizeY, 1, DataType.INT);
      squaredDistanceMap.setImage(0, z, dMapSlice);
    }
    squaredDistanceMap.endUpdate();
    
    int[][][] sData = squaredDistanceMap.getDataXYCZAsInt(0);
    for (int z = 0; z < sizeZ; z++) {
      for (int x = 0; x < sizeX; x++) {
        for (int y = 0; y < sizeY; y++) {
          sData[z][0][x + y*sizeX] = (int)(distXYZData[z][0][x + y*sizeX] * distXYZData[z][0][x + y*sizeX]);
        }
      }
    }
    squaredDistanceMap.dataChanged();
    
    /*
    distanceMapXYZ = new Sequence(segmentedSequence.getName() + "_DistanceMapXYZ");
    squaredDistanceMap = new Sequence(segmentedSequence.getName() + "_SquaredDistanceMap");

    distanceMapXYZ.beginUpdate();
    squaredDistanceMap.beginUpdate();
    try {
      int sizeX = segmentedSequence.getSizeX();
      int sizeY = segmentedSequence.getSizeY();
      int sizeZ = segmentedSequence.getSizeZ();

      int maxDistVal = Math.max(sizeX, Math.max(sizeY, sizeZ));

      // Init
      for (int z = 0; z < sizeZ; z++) {
        IcyBufferedImage dMapXYZSlice = new IcyBufferedImage(sizeX, sizeY, 3, DataType.INT);
        IcyBufferedImage dMapSlice = new IcyBufferedImage(sizeX, sizeY, 1, DataType.INT);
        distanceMapXYZ.setImage(0, z, dMapXYZSlice);
        squaredDistanceMap.setImage(0, z, dMapSlice);
      }
      distanceMapXYZ.dataChanged();
      squaredDistanceMap.dataChanged();

      short[][][] segData = segmentedSequence.getDataXYCZAsShort(0);
      int[][][] dMapXYZData = distanceMapXYZ.getDataXYCZAsInt(0);
      int[][][] dMapData= squaredDistanceMap.getDataXYCZAsInt(0);

      int z, x, y, i, j, bestVal, bestDistX, bestDistY, bestDistZ, val, distX, distY, distZ;
      // First Pass
      for (z = 0; z < sizeZ; z++) {

        // Calculate minimum distance
        for (x = 0; x < sizeX; x++) {
          for (y = 0; y < sizeY; y++) {

            // Initialize distance map slice
            if (segData[z][0][x + y*sizeX] != 0) {
              dMapXYZData[z][0][x + y*sizeX] = maxDistVal;
              dMapXYZData[z][1][x + y*sizeX] = maxDistVal;
              dMapXYZData[z][2][x + y*sizeX] = maxDistVal;
            }

            bestDistX = dMapXYZData[z][0][x + y*sizeX];
            bestDistY = dMapXYZData[z][1][x + y*sizeX];
            bestDistZ = dMapXYZData[z][2][x + y*sizeX];
            bestVal = bestDistX*bestDistX + bestDistY*bestDistY + bestDistZ*bestDistZ;

            // Z-1
            if (z > 0) {
              for (i = -1; i < 2; i++) {
                for (j = -1; j < 2; j++) {
                  if (x+i >= 0 && x+i < sizeX && y+j >= 0 && y+j < sizeY) {

                    distX = dMapXYZData[z-1][0][x+i + (y+j)*sizeX];
                    distY = dMapXYZData[z-1][1][x+i + (y+j)*sizeX];
                    distZ = dMapXYZData[z-1][2][x+i + (y+j)*sizeX];
                    distX += Math.abs(i);
                    distY += Math.abs(j);
                    distZ += 1;
                    
                    val = distX*distX + distY*distY + distZ*distZ; 

                    if (val < bestVal) {
                      bestDistX = distX;
                      bestDistY = distY;
                      bestDistZ = distZ;
                      bestVal = val;
                    }
                  }
                }
              }
            }
            // Z
            for (i = -1; i < 1; i++) {
              for (j = -1; (i==-1? (j < 2): (j < 0)); j++) {
                if (x+i >= 0 && x+i < sizeX && y+j >= 0 && y+j < sizeY) {
                  distX = dMapXYZData[z][0][x+i + (y+j)*sizeX];
                  distY = dMapXYZData[z][1][x+i + (y+j)*sizeX];
                  distZ = dMapXYZData[z][2][x+i + (y+j)*sizeX];
                  distX += Math.abs(i);
                  distY += Math.abs(j);

                  val = distX*distX + distY*distY + distZ*distZ; 

                  if (val < bestVal) {
                    bestDistX = distX;
                    bestDistY = distY;
                    bestDistZ = distZ;
                    bestVal = val;
                  }
                }
              }
            }

            dMapXYZData[z][0][x + y*sizeX] = bestDistX;
            dMapXYZData[z][1][x + y*sizeX] = bestDistY;
            dMapXYZData[z][2][x + y*sizeX] = bestDistZ;

            dMapData[z][0][x + y*sizeX] = bestVal;
          }
        }
      }

      // Second Pass
      for (z = sizeZ-1; z >= 0; z--) {

        // Calculate minimum distance
        for (x = sizeX-1; x >= 0; x--) {
          for (y = sizeY-1; y >= 0; y--) {
            bestDistX = dMapXYZData[z][0][x + y*sizeX];
            bestDistY = dMapXYZData[z][1][x + y*sizeX];
            bestDistZ = dMapXYZData[z][2][x + y*sizeX];
            bestVal = bestDistX*bestDistX + bestDistY*bestDistY + bestDistZ*bestDistZ;

            // Z+1
            if (z < sizeZ-1) {
              for (i = 1; i >= -1; i--) {
                for (j = 1; j >= -1; j--) {
                  if (x+i >= 0 && x+i < sizeX && y+j >= 0 && y+j < sizeY) {

                    distX = dMapXYZData[z+1][0][x+i + (y+j)*sizeX];
                    distY = dMapXYZData[z+1][1][x+i + (y+j)*sizeX];
                    distZ = dMapXYZData[z+1][2][x+i + (y+j)*sizeX];
                    distX += Math.abs(i);
                    distY += Math.abs(j);
                    distZ += 1;
                    
                    val = distX*distX + distY*distY + distZ*distZ; 

                    if (val < bestVal) {
                      bestDistX = distX;
                      bestDistY = distY;
                      bestDistZ = distZ;
                      bestVal = val;
                    }
                  }
                }
              }
            }
            // Z
            for (i = 1; i >= 0; i--) {
              for (j = 1; (i==1? (j >= -1): (j >= 1)); j--) {
                if (x+i >= 0 && x+i < sizeX && y+j >= 0 && y+j < sizeY) {
                  distX = dMapXYZData[z][0][x+i + (y+j)*sizeX];
                  distY = dMapXYZData[z][1][x+i + (y+j)*sizeX];
                  distZ = dMapXYZData[z][2][x+i + (y+j)*sizeX];
                  distX += Math.abs(i);
                  distY += Math.abs(j);

                  val = distX*distX + distY*distY + distZ*distZ; 

                  if (val < bestVal) {
                    bestDistX = distX;
                    bestDistY = distY;
                    bestDistZ = distZ;
                    bestVal = val;
                  }
                }
              }
            }

            dMapXYZData[z][0][x + y*sizeX] = bestDistX;
            dMapXYZData[z][1][x + y*sizeX] = bestDistY;
            dMapXYZData[z][2][x + y*sizeX] = bestDistZ;

            dMapData[z][0][x + y*sizeX] = bestVal;
          }
        }

      }
      
    } finally {
      distanceMapXYZ.dataChanged();
      squaredDistanceMap.dataChanged();
      distanceMapXYZ.endUpdate();
      squaredDistanceMap.endUpdate();
    }
*/
    return squaredDistanceMap;
  }



  public Sequence getInvertedSquaredDistanceMap() {
    if (squaredDistanceMap != null) {
      if (invertedSquaredDistanceMap == null) {
        int sizeX = squaredDistanceMap.getSizeX();
        int sizeY = squaredDistanceMap.getSizeY();
        int sizeZ = squaredDistanceMap.getSizeZ();
        
        invertedSquaredDistanceMap = new Sequence(segmentedSequence.getName() + "_InvertedSquaredDistanceMap");
        invertedSquaredDistanceMap.beginUpdate();
        try {
          for (int z = 0; z < sizeZ; z++) {
            IcyBufferedImage tmpImg = new IcyBufferedImage(sizeX, sizeY, 1, DataType.DOUBLE);
            invertedSquaredDistanceMap.setImage(0, z, tmpImg);
          }
        
          double[][][] iSDMData = invertedSquaredDistanceMap.getDataXYCZAsDouble(0);
          int[][][] sDMData = squaredDistanceMap.getDataXYCZAsInt(0);
          for(int z = 0; z < sizeZ; z++) {
            for (int xy = 0; xy < sizeX * sizeY; xy++) {
              if (sDMData[z][0][xy] > 0)
                iSDMData[z][0][xy] = 1.0/(new Integer(sDMData[z][0][xy]).doubleValue());
              else
                iSDMData[z][0][xy] = Double.POSITIVE_INFINITY;
            }
          }
        } finally {
          invertedSquaredDistanceMap.endUpdate();
        }
      }
    }
    return invertedSquaredDistanceMap;
  }

}
