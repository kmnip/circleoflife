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
import java.util.ArrayDeque;
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
    
    private static void moveSW(Area a, Point2D p) {
        // move area's southwest corner to given point
        AffineTransform t = new AffineTransform();
        Rectangle r = a.getBounds();
        t.translate(p.getX(), p.getY() - r.height);
        a.transform(t);
    }

    private static void moveNW(Area a, Point2D p) {
        // move area's northwest corner to given point
        AffineTransform t = new AffineTransform();
        t.translate(p.getX(), p.getY());
        a.transform(t);
    }
    
    private static void moveNE(Area a, Point2D p) {
        // move area's northeast corner to given point
        AffineTransform t = new AffineTransform();
        Rectangle r = a.getBounds();
        t.translate(p.getX() - r.width, p.getY());
        a.transform(t);
    }

    private static void moveSE(Area a, Point2D p) {
        // move area's southeast corner to given point
        AffineTransform t = new AffineTransform();
        Rectangle r = a.getBounds();
        t.translate(p.getX() - r.width, p.getY() - r.height);
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
    
    public static double diagonal(Area a) {
        Rectangle b = a.getBounds();
        return Math.sqrt(Math.pow(b.width, 2) + Math.pow(b.height, 2));
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
    
    public static double energy(Area[] areas, Point2D origin) {
        double e = 0;
        for (Area a : areas) {
            e += 1/distance(a, origin);
        }
        return e;
    }
    
    /*
        Q1        Q2        Q3        Q4
        
         ^ _       o---->    <----o      _  ^
         ||_|      |\_          _/|     |_| |
         |/        ||_|        |_||        \|
         o---->    v              v    <----o
    */
    public static void layout(Area[] areas, Point2D origin, int radius) {
        // layout areas around the circle

        int currentRadius = radius;
        ArrayDeque<Area> ringMembers = new ArrayDeque<>();
        
        Area lastArea = null;
        int lastQuadrant = -1;
        
        int maxAreaDiagonal = -1;
        
        for (Area a : areas) {
            if (lastArea == null) {
                lastQuadrant = 1;
                
                // first area in the ring
                
            }
            else {
                // check quadrant
                
                // calculate coordinate of corner point
                
                // if in quadrant 4, check whether there is enough room
                // if no room, increase radius, and start a new ring
                
                // assign area to point
                
                
            }
        }
    }
    
    /*
        Q1
        
         ^ _
         ||_|
         |/
         o---->
    */
    private static void layoutQ1(Area a, Point2D origin, int radius, Area lastArea) {
        if (lastArea == null) {
            Point2D p = new Point2D.Double(origin.getX(), origin.getY() - radius);
            moveSW(a, p);
        }
        else {
            Rectangle lastAreaBound = lastArea.getBounds();
            Rectangle thisAreaBound = a.getBounds();
            double ox = origin.getX();
            double oy = origin.getY();
            double radiusSquare = Math.pow(radius, 2);
            
            // Option A: put area NEXT to the previous area
            double x = lastAreaBound.getMaxX();
            double y = oy - Math.sqrt(radiusSquare - Math.pow(Math.abs(x-ox), 2));
            Point2D p1 = new Point2D.Double(x, y);
            
            // Option B: put area UNDER the previous area
            y = lastAreaBound.getMaxY() + thisAreaBound.height;
            x = ox + Math.sqrt(radiusSquare - Math.pow(Math.abs(oy-y), 2));
            Point2D p2 = new Point2D.Double(x, y);
            
            // move to the closest option
            if (distance(lastArea, p1) <= distance(lastArea, p2)) {
                moveSW(a, p1);
            }
            else {
                moveSW(a, p2);
            }
        }
    }
    
    /*
        Q2
        
        o---->
        |\_
        ||_|
        v
    */
    private static void layoutQ2(Area a, Point2D origin, int radius, Area lastArea) {
        if (lastArea == null) {
            Point2D p = new Point2D.Double(origin.getX() + radius, origin.getY());
            moveNW(a, p);
        }
        else {
            Rectangle lastAreaBound = lastArea.getBounds();
            Rectangle thisAreaBound = a.getBounds();
            double ox = origin.getX();
            double oy = origin.getY();
            double radiusSquare = Math.pow(radius, 2);
            
            // Option A: put area UNDER the previous area
            double y = lastAreaBound.getMaxY();
            double x = ox + Math.sqrt(radiusSquare - Math.pow(Math.abs(y-oy), 2));
            Point2D p1 = new Point2D.Double(x, y);
            
            // Option B: put area NEXT to the previous area
            x = lastAreaBound.x - thisAreaBound.width;
            y = oy + Math.sqrt(radiusSquare - Math.pow(Math.abs(x-oy), 2));
            Point2D p2 = new Point2D.Double(x, y);
            
            // move to the closest option
            if (distance(lastArea, p1) <= distance(lastArea, p2)) {
                moveNW(a, p1);
            }
            else {
                moveNW(a, p2);
            }
        }
    }
    
    /*
        Q3
        
         <----o
            _/|
           |_||
              v
    */
    private static void layoutQ3(Area a, Point2D origin, int radius, Area lastArea){
        if (lastArea == null) {
            Point2D p = new Point2D.Double(origin.getX(), origin.getY() + radius);
            moveNE(a, p);
        }
        else {
            Rectangle lastAreaBound = lastArea.getBounds();
            Rectangle thisAreaBound = a.getBounds();
            double ox = origin.getX();
            double oy = origin.getY();
            double radiusSquare = Math.pow(radius, 2);
            
            // Option A: put area NEXT to the previous area
            
            double x = lastAreaBound.x;
            double y = oy + Math.sqrt(radiusSquare - Math.pow(Math.abs(ox-x), 2));
            Point2D p1 = new Point2D.Double(x, y);
            
            // Option B: put area ABOVE the previous area
            y = lastAreaBound.y - thisAreaBound.height;
            x = ox - Math.sqrt(radiusSquare - Math.pow(Math.abs(y-oy), 2));
            Point2D p2 = new Point2D.Double(x, y);
            
            // move to the closest option
            if (distance(lastArea, p1) <= distance(lastArea, p2)) {
                moveNE(a, p1);
            }
            else {
                moveNE(a, p2);
            }
        }
    }
    
    /*
        Q4
        
           _  ^
          |_| |
             \|
         <----o
    */
    private static void layoutQ4(Area a, Point2D origin, int radius, Area lastArea){
        if (lastArea == null) {
            Point2D p = new Point2D.Double(origin.getX()-radius, origin.getY());
            moveSE(a, p);
        }
        else {
            Rectangle lastAreaBound = lastArea.getBounds();
            Rectangle thisAreaBound = a.getBounds();
            double ox = origin.getX();
            double oy = origin.getY();
            double radiusSquare = Math.pow(radius, 2);
            
            // Option A: put area ABOVE the previous area
            double y = lastAreaBound.y;
            double x = Math.sqrt(radiusSquare - Math.pow(Math.abs(oy-y), 2));
            Point2D p1 = new Point2D.Double(x, y);
            
            // Option B: put area NEXT to the previous area
            x = lastAreaBound.getMaxX() + thisAreaBound.width;
            y = Math.sqrt(radiusSquare - Math.pow(Math.abs(ox-x), 2));
            Point2D p2 = new Point2D.Double(x, y);
            
            // move to the closest option
            if (distance(lastArea, p1) <= distance(lastArea, p2)) {
                moveSE(a, p1);
            }
            else {
                moveSE(a, p2);
            }
        }
    }
    
    private static boolean inQuadrant1(Area a, Point2D origin) {
        double ox = origin.getX();
        double oy = origin.getY();
        Rectangle b = a.getBounds();
        
        double x = b.x;
        double y = b.y;
        double maxX = b.getMaxX();
        double maxY = b.getMaxY();
        
        return (y <= oy && x >= ox) || (y <= oy && maxX >= ox) || (maxY <= oy && maxX >= ox) || (maxY <= oy && x >= ox); 
    }
    
    private static boolean inQuadrant2(Area a, Point2D origin) {
        double ox = origin.getX();
        double oy = origin.getY();
        Rectangle b = a.getBounds();
        
        double x = b.x;
        double y = b.y;
        double maxX = b.getMaxX();
        double maxY = b.getMaxY();
        
        return (y >= oy && x >= ox) || (y >= oy && maxX >= ox) || (maxY >= oy && maxX >= ox) || (maxY >= oy && x >= ox);
    }
    
    private static boolean inQuadrant3(Area a, Point2D origin) {
        double ox = origin.getX();
        double oy = origin.getY();
        Rectangle b = a.getBounds();
        
        double x = b.x;
        double y = b.y;
        double maxX = b.getMaxX();
        double maxY = b.getMaxY();
        
        return (y >= oy && x <= ox) || (y >= oy && maxX <= ox) || (maxY >= oy && maxX <= ox) || (maxY >= oy && x <= ox);
    }

    private static boolean inQuadrant4(Area a, Point2D origin) {
        double ox = origin.getX();
        double oy = origin.getY();
        Rectangle b = a.getBounds();
        
        double x = b.x;
        double y = b.y;
        double maxX = b.getMaxX();
        double maxY = b.getMaxY();
        
        return (y <= oy && x <= ox) || (y <= oy && maxX <= ox) || (maxY <= oy && maxX <= ox) || (maxY <= oy && x <= ox);
    }
    
    public static int checkQuadrant(Point2D origin, Point2D p) {
        double ox = origin.getX();
        double oy = origin.getY();
        double px = p.getX();
        double py = p.getY();
        
        if (py <= oy && px >= ox) {
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
    
    public static Area[] neighbor(Area[] areas, Point2D origin, int radius) {        
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
        
        layout(clone, origin, radius);
        
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
