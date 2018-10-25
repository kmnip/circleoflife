/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package circleoflife;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.util.Random;

/**
 *
 * @author kmnip
 */
public class CircleOfLife {
    public static Random randNumGen = new Random();

    public static Area getRandomShape(int maxWidth, int maxHeight, int reshapeIterations) {
        // initial area is a rectangle
        Area s = new Area(new Rectangle(0, 0, maxWidth, maxHeight));
        
        // create random rectangular slices from corners
        int maxSliceWidth = maxWidth/2;
        int maxSliceHeight = maxHeight/2;
        int w,h;
        
        for (int i=0; i<reshapeIterations; ++i) {
            // top left corner
            w = randNumGen.nextInt(maxSliceWidth);
            h = randNumGen.nextInt(maxSliceHeight);                
            s.subtract(new Area(new Rectangle(0, 0, w, h)));

            // top right corner
            w = randNumGen.nextInt(maxSliceWidth);
            h = randNumGen.nextInt(maxSliceHeight);
            s.subtract(new Area(new Rectangle(maxWidth-w, 0, w, h)));

            // bottom right corner
            w = randNumGen.nextInt(maxSliceWidth);
            h = randNumGen.nextInt(maxSliceHeight);
            s.subtract(new Area(new Rectangle(maxWidth-w, maxHeight-h, w, h)));

            // bottom left corner
            w = randNumGen.nextInt(maxSliceWidth);
            h = randNumGen.nextInt(maxSliceHeight);
            s.subtract(new Area(new Rectangle(0, maxHeight-h, w, h)));
        }
        
        return s;
    }
    
    public static void move(Area a, Point2D p) {
        // move area's midpoint to given point
        AffineTransform t = new AffineTransform();
        Rectangle r = a.getBounds();
        t.translate(p.getX() - r.getCenterX(), p.getY() - r.getCenterY());
        a.transform(t);
    }
    
    public static void rotateForward(Area a) {
        rotate(a, 1);
    }
    
    public static void rotateBackward(Area a) {
        rotate(a, 3);
    }
    
    public static void rotate(Area a, int numQuadrants) {
        Rectangle r = a.getBounds();
        
        AffineTransform at = new AffineTransform();
        at.quadrantRotate(numQuadrants, r.getCenterX(), r.getCenterY());
        
        a.transform(at);
    }
    
    public static boolean hasSimpleOverlap(Area a1, Area a2) {
        return a1.intersects(a2.getBounds2D());
    }
    
    public static boolean hasOverlap(Area a1, Area a2) {
        if (a1.intersects(a2.getBounds2D())) {
            // Note that rectangular bounds are an overestimate!
            
            PathIterator p = a2.getPathIterator(null);
            float[] point = new float[2];
            for (; !p.isDone(); p.next()) {
                p.currentSegment(point);

                if (a1.contains(point[0], point[1])) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    public static double distance(Area a1, Area a2) {
        // distance between center points of 2 areas
        
        Rectangle b1 = a1.getBounds();
        Rectangle b2 = a2.getBounds();

        Point2D c1 = new Point2D.Double(b1.getCenterX(), b1.getCenterY());
        return c1.distance(b2.getCenterX(), b2.getCenterY());
    }
    
    public static double distance(Area a, Point2D p) {
        // distance between center point of area to point
        
        Rectangle b = a.getBounds();
        Point2D c = new Point2D.Double(b.getCenterX(), b.getCenterY());
        return c.distance(p);
    }
    
    public static int numOverlaps(Area a, Area[] areas) {
        int numOverlaps = 0;
        
        for (Area other : areas) {
            if (other != a) {
                if (hasOverlap(a, other)) {
                    ++numOverlaps;
                }
            }
        }
        
        return numOverlaps;
    }
    
    private static double overlappingDistances(Area a, Area[] areas) {
        double total = 0;
        
        for (Area other : areas) {
            if (other != a) {
                if (hasOverlap(a, other)) {
                    // use the inverse distance for the sake of simplicity
                    total += 1.0d/distance(a, other);
                }
            }
        }
        
        return total;
    }
    
    public static double energy(Area[] areas, Point2D p) {
        double e = 0;
        for (Area a : areas) {
            e += 1/distance(a, p);
        }
        return e;
    }
    
    public static void layout(Area[] areas, Point2D p, int radius) {
        // layout of areas around the circle
        
        Area layerStart = null;
        Area lastShape = null;
        
        for (Area a : areas) {
            if (layerStart == null) {
                layerStart = a;
            }
            else {
                
            }
            
            lastShape = a;
        }
    }
    
    public static int checkQuadrant(Point2D origin, Point2D p) {
        double ox = origin.getX();
        double oy = origin.getY();
        double px = p.getX();
        double py = p.getY();
        
        if (py < oy && px >= ox) {
            return 1;
        }
        else if (py >= oy && px >= ox) {
            return 2;
        }
        else if (py >= oy && px <= ox) {
            return 3;
        }
        else if (py <= oy && px <= ox) {
            return 4;
        }
        
        return -1;
    }
    
    public static Area[] neighbor(Area[] areas, Point2D p, int radius) {        
        int num = areas.length;
        
        Area[] clone = new Area[num];
        for (int i=0; i<num; ++i) {
            clone[i] = (Area) areas[i].clone();
        }

        // switch random pair of areas
        int a = randNumGen.nextInt(num);
        int b = randNumGen.nextInt(num);
        
        Area tmp = clone[a];
        clone[a] = clone[b];
        clone[b] = tmp;
        
        layout(clone, p, radius);
        
        return clone;
    }
    
    public static void simulatedAnnealing(Area[] areas, Point2D p, int radius, int steps) {
        /**@TODO
            Let s = s0
            For k = 0 through kmax (exclusive):
            T ← temperature(k ∕ kmax)
            Pick a random neighbour, snew ← neighbour(s)
            If P(E(s), E(snew), T) ≥ random(0, 1):
            s ← snew
            Output: the final state s
        */
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // max dimensions of each shape
        int maxShapeWidth = 2000;
        int maxShapeHeight = 1000;
        int maxShapeDiagonal = (int) Math.ceil(Math.sqrt(Math.pow(maxShapeWidth, 2) + Math.pow(maxShapeHeight, 2)));
        
        int reshapeIterations = 2;
        int gap = 10;
        
        int layoutBaseRadius = 30000;
        int layers = 10;
        int maxWidth = layoutBaseRadius * 2 + layers * maxShapeDiagonal + (layers - 1) * gap;
        int maxHeight = layoutBaseRadius * 2 + layers * maxShapeDiagonal + (layers - 1) * gap;
        
        // calculate the number of shapes to generate for X layers
        int numShapes = 0;
        for (int i=0; i<layers; ++i) {
            numShapes += Math.floor(2 * Math.PI * (layoutBaseRadius + maxShapeDiagonal*i)/maxShapeDiagonal);
        }
        
        // generate random shapes
        Area[] shapes = new Area[numShapes];
        for (int i=0; i<numShapes; ++i) {
            shapes[i] = getRandomShape(maxShapeWidth, maxShapeHeight, reshapeIterations);
        }
        
        /**@TODO*/
    }
    
}
