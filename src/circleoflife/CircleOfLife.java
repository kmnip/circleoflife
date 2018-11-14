/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package circleoflife;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.PathIterator;
import static java.awt.geom.PathIterator.SEG_CLOSE;
import static java.awt.geom.PathIterator.SEG_CUBICTO;
import static java.awt.geom.PathIterator.SEG_LINETO;
import static java.awt.geom.PathIterator.SEG_MOVETO;
import static java.awt.geom.PathIterator.SEG_QUADTO;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 *
 * @author kmnip
 */
public class CircleOfLife {    
    public static Random randNumGen = new Random();
    
    private final Area[] shapes;
    private final Point2D origin;
    private final int layoutBaseRadius;
    private final Area baseCircle;
    private final int gap;
    
    public CircleOfLife(Area[] shapes, Point2D origin, int layoutBaseRadius, int gap) {
        this.shapes = shapes;
        this.origin = origin;
        this.layoutBaseRadius = layoutBaseRadius;
        this.gap = gap;
        this.baseCircle = new Area(new Ellipse2D.Double(origin.getX()-layoutBaseRadius, origin.getY()-layoutBaseRadius, 2*layoutBaseRadius, 2*layoutBaseRadius));
    }
    
    public class MyShape {
        Area area;
        String id;
        int numRotations;
        
        public MyShape(Area area, String id) {
            this.id = id;
            this.area = area;
            this.numRotations = 0;
        }
        
        public MyShape(Area area, String id, int numRotations) {
            this.id = id;
            this.area = area;
            this.numRotations = numRotations;
        }
        
        public void rotate() {
            if (numRotations >= 3) {
                numRotations = 0;
            }
            else {
                ++numRotations;
            }
            
            rotateForward(area);
        }
        
        public Point getPosition() {
            return this.area.getBounds().getLocation();
        }
    }
    
    public static Area createArea(int width, int height, int[] rows, int[] cols) {
        // initial area is rectangle
        Area a = new Area(new Rectangle(0, 0, width, height));

        // slice out left and right portions of each row
        for (int i=0; i<height; ++i) {
            int index = i*2;
            int minX = rows[index];
            int maxX = rows[index+1];
            
            a.subtract(new Area(new Rectangle(0, i, minX-1, 1)));
            a.subtract(new Area(new Rectangle(maxX+1, i, width-1-maxX, 1)));
        }
        
        // slice out top and bottom portions of each column
        for (int i=0; i<width; ++i) {
            int index = i*2;
            int minY = cols[index];
            int maxY = cols[index+1];
            
            a.subtract(new Area(new Rectangle(i, 0, 1, minY-1)));
            a.subtract(new Area(new Rectangle(i, maxY+1, 1, height-1-maxY)));
        }
        
        return a;
    }
    
    public static Area getRandomShape(int maxWidth, int maxHeight, int reshapeIterations) {
        // randomize width and height
        int width = ThreadLocalRandom.current().nextInt(maxWidth/2, maxWidth + 1);
        int height = ThreadLocalRandom.current().nextInt(maxHeight/2, maxHeight + 1);
//        int width = maxWidth;
//        int height = maxHeight;

        // initial area is a rectangle
        Area s = new Area(new Rectangle(0, 0, width, height));
        
        // create random rectangular slices from corners
        int maxSliceWidth = width/2;
        int maxSliceHeight = height/2;
        int w,h;
        
        for (int i=0; i<reshapeIterations; ++i) {
            // top left corner
            w = randNumGen.nextInt(maxSliceWidth);
            h = randNumGen.nextInt(maxSliceHeight);                
            s.subtract(new Area(new Rectangle(0, 0, w, h)));

            // top right corner
            w = randNumGen.nextInt(maxSliceWidth);
            h = randNumGen.nextInt(maxSliceHeight);
            s.subtract(new Area(new Rectangle(width-w, 0, w, h)));

            // bottom right corner
            w = randNumGen.nextInt(maxSliceWidth);
            h = randNumGen.nextInt(maxSliceHeight);
            s.subtract(new Area(new Rectangle(width-w, height-h, w, h)));

            // bottom left corner
            w = randNumGen.nextInt(maxSliceWidth);
            h = randNumGen.nextInt(maxSliceHeight);
            s.subtract(new Area(new Rectangle(0, height-h, w, h)));
        }
        
        return s;
    }
    
    public static void shift(Area a, int x, int y) {
        AffineTransform t = new AffineTransform();
        t.translate(x, y);
        a.transform(t);
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
        t.translate(p.getX() - r.x, p.getY() - r.y - r.height);
        a.transform(t);
    }

    private static void moveNW(Area a, Point2D p) {
        // move area's northwest corner to given point
        AffineTransform t = new AffineTransform();
        Rectangle r = a.getBounds();
        t.translate(p.getX() - r.x, p.getY() - r.y);
        a.transform(t);
    }
    
    private static void moveNE(Area a, Point2D p) {
        // move area's northeast corner to given point
        AffineTransform t = new AffineTransform();
        Rectangle r = a.getBounds();
        t.translate(p.getX() - r.x - r.width, p.getY() - r.y);
        a.transform(t);
    }

    private static void moveSE(Area a, Point2D p) {
        // move area's southeast corner to given point
        AffineTransform t = new AffineTransform();
        Rectangle r = a.getBounds();
        t.translate(p.getX() - r.x - r.width, p.getY() - r.y - r.height);
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
    
    public boolean overlapsBaseCircle(Area a) {
        if (a.intersects(baseCircle.getBounds2D())) {
            float[] point = new float[2];
            
            PathIterator p = a.getPathIterator(null);
            for (; !p.isDone(); p.next()) {
                p.currentSegment(point);
                if (origin.distance(point[0], point[1]) <= layoutBaseRadius) {
                    return true;
                }
            }
            
            p = baseCircle.getPathIterator(null);
            point = new float[6];
            for (; !p.isDone(); p.next()) {
                int type = p.currentSegment(point);
                switch (type) {
                    case SEG_MOVETO:
                    case SEG_LINETO:
                        if (a.contains(point[0], point[1])) {
                            return true;
                        }
                        break;
                    case SEG_QUADTO:
                        if (a.contains(point[0], point[1]) || a.contains(point[2], point[3])) {
                            return true;
                        }
                        break;
                    case SEG_CUBICTO:
                        if (a.contains(point[0], point[1]) || a.contains(point[4], point[5])) {
                            return true;
                        }
                        break;
                    case SEG_CLOSE:
                        break;
                }
            }
        }
        
        return false;
    }
    
    public static boolean hasOverlap(Area a1, Area a2) {
        if (a1.intersects(a2.getBounds2D())) {
            float[] point = new float[2];
            
            PathIterator p = a2.getPathIterator(null);
            for (; !p.isDone(); p.next()) {
                p.currentSegment(point);
                if (a1.contains(point[0], point[1])) {
                    return true;
                }
            }
            
            p = a1.getPathIterator(null);
            for (; !p.isDone(); p.next()) {
                p.currentSegment(point);
                if (a2.contains(point[0], point[1])) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    public static boolean hasOverlap(Area a, ArrayDeque<Area> others) {
        for (Area other : others) {
            if (hasOverlap(a, other)) {
                return true;
            }
        }
        
        return false;
    }
    
    public static boolean hasSimpleOverlap(Area a, ArrayDeque<Area> others) {
        for (Area other : others) {
            if (hasSimpleOverlap(a, other)) {
                return true;
            }
        }
        
        return false;        
    }
    
    public static boolean hasOverlap(Area a, ArrayList<ArrayDeque<Area>> layers) {
        for (ArrayDeque<Area> others : layers) {
            if (hasOverlap(a, others)) {
                return true;
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
    
    public static double maxDistance(Area a, Point2D p) {
        double maxDistance = 0;
        
        PathIterator itr = a.getPathIterator(null);
        double[] point = new double[2];
        for (; !itr.isDone(); itr.next()) {
            itr.currentSegment(point);
            maxDistance = Math.max(maxDistance, p.distance(point[0], point[1]));
        }
            
        return maxDistance;
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
    
    private int layoutHelper(int lastQuadrant, Area a, int currentRadius, ArrayDeque<Area> ringMembers, boolean inchBack) {
        Area lastArea = ringMembers.getLast();
        
        switch (lastQuadrant) {
            case 1:
                if (isMoreThanHalfQ1(lastArea)) {
                    if (isVertical(a)) {
                        rotateForward(a);
                    }
                }
                else {
                    if (isHorizontal(a)) {
                        rotateForward(a);
                    }                        
                }
                layoutQ1(a, currentRadius, ringMembers, inchBack);
                if (inQuadrant2(a)) {
                    lastQuadrant = 2;
                }
                break;
            case 2:
                if (isMoreThanHalfQ2(lastArea)) {
                    if (isHorizontal(a)) {
                        rotateForward(a);
                    }
                }
                else {
                    if (isVertical(a)) {
                        rotateForward(a);
                    }                        
                }
                layoutQ2(a, currentRadius, ringMembers, inchBack);
                if (inQuadrant3(a)) {
                    lastQuadrant = 3;
                }                        
                break;
            case 3:
                if (isMoreThanHalfQ3(lastArea)) {
                    if (isVertical(a)) {
                        rotateForward(a);
                    }
                }
                else {
                    if (isHorizontal(a)) {
                        rotateForward(a);
                    }                        
                }
                layoutQ3(a, currentRadius, ringMembers, inchBack);
                if (inQuadrant4(a)) {
                    lastQuadrant = 4;
                }
                break;
            case 4:
                if (isMoreThanHalfQ4(lastArea)) {
                    if (isHorizontal(a)) {
                        rotateForward(a);
                    }
                }
                else {
                    if (isVertical(a)) {
                        rotateForward(a);
                    }                        
                }
                layoutQ4(a, currentRadius, ringMembers, inchBack);
                if (inQuadrant1(a)) {
                    lastQuadrant = 1;
                }
                break;
        }
        
        return lastQuadrant;
    }
    
    private void slideTowardsOrigin(Area a, ArrayDeque<Area> ringMembers) {
        int q = getQuadrant(a);
        
        switch (q) {
            case 1:
                if (isMoreThanHalfQ1(a)) {
                    while (true) {
                        int dx = moveLeftSimple(a, ringMembers);
                        int dy = moveDownSimple(a, ringMembers);

                        if (dx >= 0 && dy <= 0) {
                            break;
                        }
                    }

                    while (true) {
                        int dx = moveLeft(a, ringMembers);
                        int dy = moveDown(a, ringMembers);

                        if (dx >= 0 && dy <= 0) {
                            break;
                        }
                    }
                }
                else {                
                    while (true) {
                        int dy = moveDownSimple(a, ringMembers);
                        int dx = moveLeftSimple(a, ringMembers);

                        if (dx >= 0 && dy <= 0) {
                            break;
                        }
                    }

                    while (true) {
                        int dy = moveDown(a, ringMembers);
                        int dx = moveLeft(a, ringMembers);

                        if (dx >= 0 && dy <= 0) {
                            break;
                        }
                    }
                }
                break;
            case 2:
                if (isMoreThanHalfQ2(a)) {
                    while (true) {
                        int dy = moveUpSimple(a, ringMembers);
                        int dx = moveLeftSimple(a, ringMembers);

                        if (dy >= 0 && dx >= 0) {
                            break;
                        }
                    }

                    while (true) {
                        int dy = moveUp(a, ringMembers);
                        int dx = moveLeft(a, ringMembers);

                        if (dy >= 0 && dx >= 0) {
                            break;
                        }
                    }
                }
                else {
                    while (true) {
                        int dx = moveLeftSimple(a, ringMembers);
                        int dy = moveUpSimple(a, ringMembers);

                        if (dy >= 0 && dx >= 0) {
                            break;
                        }                
                    }

                    while (true) {
                        int dx = moveLeft(a, ringMembers);
                        int dy = moveUp(a, ringMembers);

                        if (dy >= 0 && dx >= 0) {
                            break;
                        }                
                    }
                }
                break;
            case 3:
                if (isMoreThanHalfQ3(a)) {
                    while (true) {
                        int dx = moveRightSimple(a, ringMembers);
                        int dy = moveUpSimple(a, ringMembers);

                        if (dx <= 0 && dy >= 0) {
                            break;
                        }
                    }

                    while (true) {
                        int dx = moveRight(a, ringMembers);
                        int dy = moveUp(a, ringMembers);

                        if (dx <= 0 && dy >= 0) {
                            break;
                        }
                    }
                }
                else {
                    while (true) {
                        int dy = moveUpSimple(a, ringMembers);
                        int dx = moveRightSimple(a, ringMembers);

                        if (dx <= 0 && dy >= 0) {
                            break;
                        }
                    }

                    while (true) {
                        int dy = moveUp(a, ringMembers);
                        int dx = moveRight(a, ringMembers);

                        if (dx <= 0 && dy >= 0) {
                            break;
                        }
                    }
                }
                break;
            case 4:
                if (isMoreThanHalfQ4(a)) {
                    while (true) {
                        int dy = moveDownSimple(a, ringMembers);
                        int dx = moveRightSimple(a, ringMembers);

                        if (dy <= 0 && dx <= 0) {
                            break;
                        }
                    }

                    while (true) {
                        int dy = moveDown(a, ringMembers);
                        int dx = moveRight(a, ringMembers);

                        if (dy <= 0 && dx <= 0) {
                            break;
                        }
                    }
                }
                else {
                    while (true) {
                        int dx = moveRightSimple(a, ringMembers);
                        int dy = moveDownSimple(a, ringMembers);

                        if (dx <= 0 && dy <= 0) {
                            break;
                        }
                    }

                    while (true) {
                        int dx = moveRight(a, ringMembers);
                        int dy = moveDown(a, ringMembers);

                        if (dx <= 0 && dy <= 0) {
                            break;
                        }
                    }
                }
                break;
        }
    }
    
    public void layout2() {
        int steps = 64;
        
        double[] angles = new double[steps];
        for (int i=0; i<steps; ++i) {
            angles[i] = 2 * Math.PI * i / steps;
        }
        
        ArrayDeque<Area> ringMembers = new ArrayDeque<>();
        int currentRadius = this.layoutBaseRadius;
        int ox = (int) origin.getX();
        int oy = (int) origin.getY();
        
        int numShapes = shapes.length;
        for (int i=0; i<numShapes; ++i) {
            System.out.println(i);
            
            Area a = shapes[i];
            double diag = diagonal(a);
            
            double bestDistance = Double.POSITIVE_INFINITY;
            Point2D bestCoord = null;
            
            while (bestCoord == null) {
                for (double angle : angles) {
                    int x = (int) Math.ceil(ox + (currentRadius + diag) * Math.sin(angle));
                    int y = (int) Math.ceil(oy + (currentRadius + diag) * Math.cos(angle));
                    Point2D p = new Point2D.Double(x, y);

                    // move to starting coord
                    moveNW(a, p);

                    if (!overlapsBaseCircle(a) && !hasOverlap(a, ringMembers)) {
                        // shape did not overlap at starting coord 

                        // slide towards origin
                        slideTowardsOrigin(a, ringMembers);

                        // calculate distance to origin
                        double d = distance(a, origin);
                        if (d < bestDistance) {
                            bestDistance = d;
                            Rectangle2D bound = a.getBounds();
                            bestCoord = new Point2D.Double(bound.getX(), bound.getY());
                        }
                    }
                }
                
                if (bestCoord == null) {
                    currentRadius += diag;
                }
            }
            
            // move to closest point
            moveNW(a, bestCoord);
            ringMembers.add(a);
        }
    }
    
    /*
        Q1        Q2        Q3        Q4
        
         ^ _       o---->    <----o      _  ^
         ||_|      |\_          _/|     |_| |
         |/        ||_|        |_||        \|
         o---->    v              v    <----o
    */
    public void layout() {
        // layout areas around the circle

        ArrayDeque<Area> ringMembers = new ArrayDeque<>();
        
        int currentRadius = this.layoutBaseRadius;
        int lastQuadrant = 1;
        Area a = shapes[0];
        if (isHorizontal(a)) {
            rotateForward(a);
        }  
        layoutQ1(a, currentRadius, ringMembers, false);
        
        ringMembers.add(a);
        double maxAreaDistance = maxDistance(a, origin);
        
        int numAreas = shapes.length;
        for (int i=1; i<numAreas; ++i) {
            System.out.println(i);
            
            a = shapes[i];
            
            int q = layoutHelper(lastQuadrant, a, currentRadius, ringMembers, true);
            
            if (hasOverlap(a, ringMembers)) {
                currentRadius = (int) maxAreaDistance + gap;
 
                q = layoutHelper(q, a, currentRadius, ringMembers, false);
                
                maxAreaDistance = 0;
            }

            lastQuadrant = q;
            ringMembers.add(a);
            maxAreaDistance = Math.max(maxAreaDistance, maxDistance(a, origin));
        }
    }
    
    /*
        Q1
        
         ^ _
         ||_|
         |/
         o---->
    */
    private void layoutQ1(Area a, int radius, ArrayDeque<Area> placedAreas, boolean inchBack) {
        if (!inchBack || placedAreas.isEmpty()) {
            Point2D p = new Point2D.Double(origin.getX(), origin.getY() - radius);
            moveSW(a, p);
        }
        else {
            Area lastArea = placedAreas.getLast();
            
            Rectangle lastAreaBound = lastArea.getBounds();
            Rectangle thisAreaBound = a.getBounds();
            double ox = origin.getX();
            double oy = origin.getY();
            double radiusSquare = Math.pow(radius, 2);
            
            // Option A: put area NEXT to the previous area
            double x = lastAreaBound.getMaxX() + gap;
            double y = oy - Math.sqrt(radiusSquare - Math.pow(Math.abs(x-ox), 2));
            Point2D p1 = new Point2D.Double(x, y);
            
            // Option B: put area UNDER the previous area
            y = lastAreaBound.getMaxY() + thisAreaBound.height + gap;
            if (y >= oy) {
                x = ox + radius;
            }
            else {
                x = ox + Math.sqrt(radiusSquare - Math.pow(Math.abs(oy-y), 2));
            }
            Point2D p2 = new Point2D.Double(x, y);
            
            // move to the closest option
            if (distance(lastArea, p1) <= distance(lastArea, p2)) {
                moveSW(a, p1);

                /*
                // inch back towards last area
                Point2D lastPosition = p1;
                x = lastAreaBound.getMaxX() + gap;
                while (x >= ox) {
                    --x;
                    y = oy - Math.sqrt(radiusSquare - Math.pow(Math.abs(x-ox), 2));
                    p1 = new Point2D.Double(x, y);
                    moveSW(a, p1);
                    
                    if (hasOverlap(a, placedAreas)) {
                        x = lastPosition.getX() + gap;
                        y = oy - Math.sqrt(radiusSquare - Math.pow(Math.abs(x-ox), 2));
                        lastPosition = new Point2D.Double(x, y);
                        
                        moveSW(a, lastPosition);
                        break;
                    }
                    
                    lastPosition = p1;
                }
                */
            }
            else {
                moveSW(a, p2);
                
                /*
                // inch back towards last area
                Point2D lastPosition = p2;
                y = lastAreaBound.getMaxY() + thisAreaBound.height + gap;
                while (y >= oy - radius) {
                    --y;
                    if (y >= oy) {
                        x = ox + radius;
                    }
                    else {
                        x = ox + Math.sqrt(radiusSquare - Math.pow(Math.abs(oy-y), 2));
                    }
                    p2 = new Point2D.Double(x, y);
                    moveSW(a, p2);
                    
                    if (hasOverlap(a, placedAreas)) {
                        y = lastPosition.getY() + gap;
                        if (y >= oy) {
                            x = ox + radius;
                        }
                        else {
                            x = ox + Math.sqrt(radiusSquare - Math.pow(Math.abs(oy-y), 2));
                        }
                        lastPosition = new Point2D.Double(x, y);
                        
                        moveSW(a, lastPosition);
                        break;
                    }
                    
                    lastPosition = p2;
                }
                */
            }
        }
        
        if (isMoreThanHalfQ1(a)) {
            while (true) {
                int dx = moveLeft(a, placedAreas);
                int dy = moveDown(a, placedAreas);
                
                if (dx >= 0 && dy <= 0) {
                    break;
                }
            }
        }
        else {
            while (true) {
                int dy = moveDown(a, placedAreas);
                int dx = moveLeft(a, placedAreas);
                
                if (dx >= 0 && dy <= 0) {
                    break;
                }
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
    private void layoutQ2(Area a, int radius, ArrayDeque<Area> placedAreas, boolean inchBack) {
        if (!inchBack || placedAreas.isEmpty()) {
            Point2D p = new Point2D.Double(origin.getX() + radius, origin.getY());
            moveNW(a, p);
        }
        else {
            Area lastArea = placedAreas.getLast();
            Rectangle lastAreaBound = lastArea.getBounds();
            Rectangle thisAreaBound = a.getBounds();
            double ox = origin.getX();
            double oy = origin.getY();
            double radiusSquare = Math.pow(radius, 2);
            
            // Option A: put area UNDER the previous area
            double y = lastAreaBound.getMaxY() + gap;
            double x = ox + Math.sqrt(radiusSquare - Math.pow(Math.abs(y-oy), 2));
            Point2D p1 = new Point2D.Double(x, y);
            
            // Option B: put area NEXT to the previous area
            x = lastAreaBound.x - thisAreaBound.width - gap;
            if (x <= ox) {
                y = oy + radius;
            }
            else {
                y = oy + Math.sqrt(radiusSquare - Math.pow(Math.abs(x-oy), 2));
            }
            Point2D p2 = new Point2D.Double(x, y);
            
            // move to the closest option
            if (distance(lastArea, p1) <= distance(lastArea, p2)) {
                moveNW(a, p1);
                /*
                // inch back towards last area
                Point2D lastPosition = p1;
                y = lastAreaBound.getMaxY() + gap;
                while (y >= oy) {
                    --y;
                    x = ox + Math.sqrt(radiusSquare - Math.pow(Math.abs(y-oy), 2));
                    p1 = new Point2D.Double(x, y);
                    moveNW(a, p1);
                    
                    if (hasOverlap(a, placedAreas)) {
                        y = lastPosition.getY() + gap;
                        x = ox + Math.sqrt(radiusSquare - Math.pow(Math.abs(y-oy), 2));
                        lastPosition = new Point2D.Double(x, y);
                        
                        moveNW(a, lastPosition);
                        break;
                    }
                    
                    lastPosition = p1;
                }
                */
            }
            else {
                moveNW(a, p2);
                
                /*
                // inch back towards last area
                Point2D lastPosition = p2;
                x = lastAreaBound.x - thisAreaBound.width - gap;
                while (x >= ox) {
                    ++x;
                    if (x <= ox) {
                        y = oy + radius;
                    }
                    else {
                        y = oy + Math.sqrt(radiusSquare - Math.pow(Math.abs(x-oy), 2));
                    }
                    p2 = new Point2D.Double(x, y);
                    moveNW(a, p2);
                    
                    if (hasOverlap(a, placedAreas)) {
                        x = lastPosition.getX() - gap;
                        if (x <= ox) {
                            y = oy + radius;
                        }
                        else {
                            y = oy + Math.sqrt(radiusSquare - Math.pow(Math.abs(x-oy), 2));
                        }
                        lastPosition = new Point2D.Double(x, y);
                        
                        moveNW(a, lastPosition);
                        break;
                    }
                    
                    lastPosition = p2;
                }
                */
            }
        }
        
        if (isMoreThanHalfQ2(a)) {
            while (true) {
                int dy = moveUp(a, placedAreas);
                int dx = moveLeft(a, placedAreas);
            
                if (dy >= 0 && dx >= 0) {
                    break;
                }
            }
        }
        else {
            while (true) {
                int dx = moveLeft(a, placedAreas);
                int dy = moveUp(a, placedAreas);

                if (dy >= 0 && dx >= 0) {
                    break;
                }                
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
    private void layoutQ3(Area a, int radius, ArrayDeque<Area> placedAreas, boolean inchBack){
        if (!inchBack || placedAreas.isEmpty()) {
            Point2D p = new Point2D.Double(origin.getX(), origin.getY() + radius);
            moveNE(a, p);
        }
        else {
            Area lastArea = placedAreas.getLast();
            Rectangle lastAreaBound = lastArea.getBounds();
            Rectangle thisAreaBound = a.getBounds();
            double ox = origin.getX();
            double oy = origin.getY();
            double radiusSquare = Math.pow(radius, 2);
            
            // Option A: put area NEXT to the previous area
            
            double x = lastAreaBound.x - gap;
            double y = oy + Math.sqrt(radiusSquare - Math.pow(Math.abs(ox-x), 2));
            Point2D p1 = new Point2D.Double(x, y);
            
            // Option B: put area ABOVE the previous area
            y = lastAreaBound.y - thisAreaBound.height - gap;
            if (y <= oy) { 
                x = ox - radius;
            }
            else {
                x = ox - Math.sqrt(radiusSquare - Math.pow(Math.abs(y-oy), 2));
            }
            Point2D p2 = new Point2D.Double(x, y);
            
            // move to the closest option
            if (distance(lastArea, p1) <= distance(lastArea, p2)) {
                moveNE(a, p1);
                /*
                // inch back towards last area
                Point2D lastPosition = p1;
                x = lastAreaBound.x - gap;
                while (x <= ox) {
                    ++x;
                    y = oy + Math.sqrt(radiusSquare - Math.pow(Math.abs(ox-x), 2));
                    p1 = new Point2D.Double(x, y);
                    moveNE(a, p1);
                    
                    if (hasOverlap(a, placedAreas)) {
                        x = lastPosition.getX() - gap;
                        y = oy + Math.sqrt(radiusSquare - Math.pow(Math.abs(ox-x), 2));
                        lastPosition = new Point2D.Double(x, y);
                        
                        moveNE(a, lastPosition);
                        break;
                    }
                    
                    lastPosition = p1;
                }
                */
            }
            else {
                moveNE(a, p2);
                /*
                // inch back towards last area
                Point2D lastPosition = p2;
                y = lastAreaBound.y - thisAreaBound.height - gap;
                while (y >= oy) {
                    ++y;
                    if (y <= oy) { 
                        x = ox - radius;
                    }
                    else {
                        x = ox - Math.sqrt(radiusSquare - Math.pow(Math.abs(y-oy), 2));
                    }
                    p2 = new Point2D.Double(x, y);
                    moveNE(a, p2);
                    
                    if (hasOverlap(a, placedAreas)) {
                        y = lastPosition.getY() - gap;
                        if (y <= oy) { 
                            x = ox - radius;
                        }
                        else {
                            x = ox - Math.sqrt(radiusSquare - Math.pow(Math.abs(y-oy), 2));
                        }
                        lastPosition = new Point2D.Double(x, y);
                        
                        moveNE(a, lastPosition);
                        break;
                    }
                    
                    lastPosition = p2;
                }
                */
            }
        }
        
        if (isMoreThanHalfQ3(a)) {
            while (true) {
                int dx = moveRight(a, placedAreas);
                int dy = moveUp(a, placedAreas);
                
                if (dx <= 0 && dy >= 0) {
                    break;
                }
            }
        }
        else {
            while (true) {
                int dy = moveUp(a, placedAreas);
                int dx = moveRight(a, placedAreas);
                
                if (dx <= 0 && dy >= 0) {
                    break;
                }
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
    private void layoutQ4(Area a, int radius, ArrayDeque<Area> placedAreas, boolean inchBack){
        if (!inchBack || placedAreas.isEmpty()) {
            Point2D p = new Point2D.Double(origin.getX()-radius, origin.getY());
            moveSE(a, p);
        }
        else {
            Area lastArea = placedAreas.getLast();
            Rectangle lastAreaBound = lastArea.getBounds();
            Rectangle thisAreaBound = a.getBounds();
            double ox = origin.getX();
            double oy = origin.getY();
            double radiusSquare = Math.pow(radius, 2);
            
            // Option A: put area ABOVE the previous area
            double y = lastAreaBound.y - gap;
            double x = origin.getX() - Math.sqrt(radiusSquare - Math.pow(Math.abs(oy-y), 2));
            Point2D p1 = new Point2D.Double(x, y);
            
            // Option B: put area NEXT to the previous area
            x = lastAreaBound.getMaxX() + thisAreaBound.width + gap;
            if (x >= ox) {
                y = oy - radius;
            }
            else {
                y = origin.getY() - Math.sqrt(radiusSquare - Math.pow(Math.abs(ox-x), 2));
            }
            Point2D p2 = new Point2D.Double(x, y);
            
            // move to the closest option
            if (distance(lastArea, p1) <= distance(lastArea, p2)) {
                moveSE(a, p1);
                /*
                // inch back towards last area
                Point2D lastPosition = p1;
                y = lastAreaBound.y - gap;
                while (y <= oy) {
                    ++y;
                    x = origin.getX() - Math.sqrt(radiusSquare - Math.pow(Math.abs(oy-y), 2));
                    p1 = new Point2D.Double(x, y);
                    moveSE(a, p1);
                    
                    if (hasOverlap(a, placedAreas)) {
                        y = lastPosition.getY() - gap;
                        x = origin.getX() - Math.sqrt(radiusSquare - Math.pow(Math.abs(oy-y), 2));
                        lastPosition = new Point2D.Double(x, y);                        
                        
                        moveSE(a, lastPosition);
                        break;
                    }
                    
                    lastPosition = p1;
                }
                */
            }
            else {
                moveSE(a, p2);
                /*
                // inch back towards last area
                Point2D lastPosition = p2;
                x = lastAreaBound.getMaxX() + thisAreaBound.width + gap;
                while (x <= ox) {
                    --x;
                    if (x >= ox) {
                        y = oy - radius;
                    }
                    else {
                        y = origin.getY() - Math.sqrt(radiusSquare - Math.pow(Math.abs(ox-x), 2));
                    }
                    p2 = new Point2D.Double(x, y);
                    moveSE(a, p2);
                    
                    if (hasOverlap(a, placedAreas)) {
                        x = lastPosition.getX() + gap;
                        if (x >= ox) {
                            y = oy - radius;
                        }
                        else {
                            y = origin.getY() - Math.sqrt(radiusSquare - Math.pow(Math.abs(ox-x), 2));
                        }
                        lastPosition = new Point2D.Double(x, y);
                        
                        moveSE(a, lastPosition);
                        break;
                    }
                    
                    lastPosition = p2;
                }
                */
            }
        }
        
        if (isMoreThanHalfQ4(a)) {
            while (true) {
                int dy = moveDown(a, placedAreas);
                int dx = moveRight(a, placedAreas);
                
                if (dy <= 0 && dx <= 0) {
                    break;
                }
            }
        }
        else {
            while (true) {
                int dx = moveRight(a, placedAreas);
                int dy = moveDown(a, placedAreas);
                
                if (dx <= 0 && dy <= 0) {
                    break;
                }
            }
        }
    }
    
    private int moveDown(Area a, ArrayDeque<Area> ringMembers) {        
        Rectangle bounds = a.getBounds();
        
        int x = bounds.x;
        int y = bounds.y;
        
        double yMax = origin.getY();
        
        while (y < yMax) {
            ++y;
            Point2D p = new Point2D.Double(x, y);
            moveNW(a, p);
            
            if (overlapsBaseCircle(a) || hasOverlap(a, ringMembers)) {
                moveNW(a, new Point2D.Double(x, y-1));
                break;
            }
        }
        
        return a.getBounds().y - bounds.y;
    }
    
    private int moveLeft(Area a, ArrayDeque<Area> ringMembers) {
        Rectangle bounds = a.getBounds();
        
        int x = bounds.x;
        int y = bounds.y;
        
        double xMin = origin.getX() - bounds.width;
        
        while (x > xMin) {
            --x;
            Point2D p = new Point2D.Double(x, y);
            moveNW(a, p);
            
            if (overlapsBaseCircle(a) || hasOverlap(a, ringMembers)) {
                moveNW(a, new Point2D.Double(x+1, y));
                break;
            }
        }
        
        return a.getBounds().x - bounds.x;
    }
    
    private int moveUp(Area a, ArrayDeque<Area> ringMembers) {        
        Rectangle bounds = a.getBounds();
        
        int x = bounds.x;
        int y = bounds.y;
        
        double yMin = origin.getY() - bounds.height;
        
        while (y > yMin) {
            --y;
            Point2D p = new Point2D.Double(x, y);
            moveNW(a, p);
            
            if (overlapsBaseCircle(a) || hasOverlap(a, ringMembers)) {
                moveNW(a, new Point2D.Double(x, y+1));
                break;
            }
        }
        
        return a.getBounds().y - bounds.y;
    }
    
    private int moveRight(Area a, ArrayDeque<Area> ringMembers) {
        Rectangle bounds = a.getBounds();
        
        int x = bounds.x;
        int y = bounds.y;
        
        double xMax = origin.getX();
        
        while (x < xMax) {
            ++x;
            Point2D p = new Point2D.Double(x, y);
            moveNW(a, p);
            
            if (overlapsBaseCircle(a) || hasOverlap(a, ringMembers)) {
                moveNW(a, new Point2D.Double(x-1, y));
                break;
            }
        }
        
        return a.getBounds().x - bounds.x;
    }
    
    private int moveDownSimple(Area a, ArrayDeque<Area> ringMembers) {        
        Rectangle bounds = a.getBounds();
        
        int x = bounds.x;
        int y = bounds.y;
        
        double yMax = origin.getY() - bounds.height;
        
        while (y < yMax) {
            ++y;
            Point2D p = new Point2D.Double(x, y);
            moveNW(a, p);
            
            if (overlapsBaseCircle(a) || hasSimpleOverlap(a, ringMembers)) {
                moveNW(a, new Point2D.Double(x, y-1));
                break;
            }
        }
        
        return a.getBounds().y - bounds.y;
    }
    
    private int moveLeftSimple(Area a, ArrayDeque<Area> ringMembers) {
        Rectangle bounds = a.getBounds();
        
        int x = bounds.x;
        int y = bounds.y;
        
        double xMin = origin.getX();
        
        while (x > xMin) {
            --x;
            Point2D p = new Point2D.Double(x, y);
            moveNW(a, p);
            
            if (overlapsBaseCircle(a) || hasSimpleOverlap(a, ringMembers)) {
                moveNW(a, new Point2D.Double(x+1, y));
                break;
            }
        }
        
        return a.getBounds().x - bounds.x;
    }
    
    private int moveUpSimple(Area a, ArrayDeque<Area> ringMembers) {        
        Rectangle bounds = a.getBounds();
        
        int x = bounds.x;
        int y = bounds.y;
        
        double yMin = origin.getY();
        
        while (y > yMin) {
            --y;
            Point2D p = new Point2D.Double(x, y);
            moveNW(a, p);
            
            if (overlapsBaseCircle(a) || hasSimpleOverlap(a, ringMembers)) {
                moveNW(a, new Point2D.Double(x, y+1));
                break;
            }
        }
        
        return a.getBounds().y - bounds.y;
    }
    
    private int moveRightSimple(Area a, ArrayDeque<Area> ringMembers) {
        Rectangle bounds = a.getBounds();
        
        int x = bounds.x;
        int y = bounds.y;
        
        double xMax = origin.getX() + bounds.width;
        
        while (x < xMax) {
            ++x;
            Point2D p = new Point2D.Double(x, y);
            moveNW(a, p);
            
            if (overlapsBaseCircle(a) || hasSimpleOverlap(a, ringMembers)) {
                moveNW(a, new Point2D.Double(x-1, y));
                break;
            }
        }
        
        return a.getBounds().x - bounds.x;
    }
    
    private static boolean isVertical(Area a) {
        Rectangle b = a.getBounds();
        return b.height > b.width;
    }
    
    private static boolean isHorizontal(Area a) {
        Rectangle b = a.getBounds();
        return b.width > b.height;
    }
    
    private int getQuadrant(double x, double y) {
        double ox = origin.getX();
        double oy = origin.getY();

        if (y <= oy && x >= ox) {
            return 1;
        }

        if (y >= oy && x >= ox) {
            return 2;
        }

        if (y >= oy && x <= ox) {
            return 3;
        }
        
        if (y <= oy && x <= ox) {
            return 4;
        }

        return -1;
    }
    
    private int getQuadrant(Area a) {
        Rectangle b = a.getBounds();
        return getQuadrant(b.getCenterX(), b.getCenterY());
    }
    
    private boolean inQuadrant1(Area a) {
        double ox = origin.getX();
        double oy = origin.getY();
        Rectangle b = a.getBounds();
        
        double x = b.x;
        double y = b.y;
        double maxX = b.getMaxX();
        double maxY = b.getMaxY();
        
        return (y <= oy && x >= ox) || (y <= oy && maxX >= ox) || (maxY <= oy && maxX >= ox) || (maxY <= oy && x >= ox); 
    }
    
    private boolean isMoreThanHalfQ1(Area a) {
        double ox = origin.getX();
        double oy = origin.getY();
        Rectangle b = a.getBounds();
        double x = b.x;
        double maxY = b.getMaxY();
        
        return (oy - maxY) < (x - ox);
    }
    
    private boolean inQuadrant2(Area a) {
        double ox = origin.getX();
        double oy = origin.getY();
        Rectangle b = a.getBounds();
        
        double x = b.x;
        double y = b.y;
        double maxX = b.getMaxX();
        double maxY = b.getMaxY();
        
        return (y >= oy && x >= ox) || (y >= oy && maxX >= ox) || (maxY >= oy && maxX >= ox) || (maxY >= oy && x >= ox);
    }
    
    private boolean isMoreThanHalfQ2(Area a) {
        double ox = origin.getX();
        double oy = origin.getY();
        Rectangle b = a.getBounds();
        double x = b.x;
        double y = b.y;
        
        return (x - ox) < (y - oy);
    }
    
    private boolean inQuadrant3(Area a) {
        double ox = origin.getX();
        double oy = origin.getY();
        Rectangle b = a.getBounds();
        
        double x = b.x;
        double y = b.y;
        double maxX = b.getMaxX();
        double maxY = b.getMaxY();
        
        return (y >= oy && x <= ox) || (y >= oy && maxX <= ox) || (maxY >= oy && maxX <= ox) || (maxY >= oy && x <= ox);
    }

    private boolean isMoreThanHalfQ3(Area a) {
        double ox = origin.getX();
        double oy = origin.getY();
        Rectangle b = a.getBounds();
        double y = b.y;
        double maxX = b.getMaxX();
        
        return (y - oy) < (ox - maxX);
    }
    
    private boolean inQuadrant4(Area a) {
        double ox = origin.getX();
        double oy = origin.getY();
        Rectangle b = a.getBounds();
        
        double x = b.x;
        double y = b.y;
        double maxX = b.getMaxX();
        double maxY = b.getMaxY();
        
        return (y <= oy && x <= ox) || (y <= oy && maxX <= ox) || (maxY <= oy && maxX <= ox) || (maxY <= oy && x <= ox);
    }
    
    private boolean isMoreThanHalfQ4(Area a) {
        double ox = origin.getX();
        double oy = origin.getY();
        Rectangle b = a.getBounds();
        double maxX = b.getMaxX();
        double maxY = b.getMaxY();
        
        return (ox - maxX) < (oy - maxY);
    }
    
    public int checkQuadrant(Point2D p) {
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
        
    public static Area[] neighbor(Area[] areas, Point2D origin, int radius, int gap) {        
        int num = areas.length;
        
        Area[] clone = new Area[num];
        for (int i=0; i<num; ++i) {
            clone[i] = (Area) areas[i].clone();
        }

        if (randNumGen.nextInt(2) == 0) {
            // rotate a random area
            int a = randNumGen.nextInt(num);
            rotateForward(clone[a]);
        }
        else {
            // switch random pair of areas
            int a = randNumGen.nextInt(num);
            int b = randNumGen.nextInt(num);

            Area tmp = clone[a];
            clone[a] = clone[b];
            clone[b] = tmp;
        }
        
        //layout(clone, origin, radius, gap);
        
        return clone;
    }
    
    public static Area[] simulatedAnnealing(Area[] areas, Point2D origin, int radius, int steps, int gap) {
        /*
            Let s = s0
            For k = 0 through kmax (exclusive):
                T ← temperature(k ∕ kmax)
                Pick a random neighbour, snew ← neighbour(s)
                If P(E(s), E(snew), T) ≥ random(0, 1):
                    s ← snew
            Output: the final state s
        */
        
        //layout(areas, origin, radius, gap);
        Area[] bestState = areas;
        double bestEnergy = energy(bestState, origin);
        
//        Area[] currentState = bestState;
//        double currentEnergy = bestEnergy;
        
        for (int i=0; i<steps; ++i) {
            System.out.println(i);
            
            Area[] newState = neighbor(bestState, origin, radius, gap);
            double newEnergy = energy(newState, origin);
            
            if (newEnergy < bestEnergy) {
                bestEnergy = newEnergy;
                bestState = newState;
            }
        }
        
        return bestState;
    }
    
    public static class AreaComparator implements Comparator<Area> {

        @Override
        public int compare(Area a, Area b) {
            Rectangle ar = a.getBounds();
            Rectangle br = b.getBounds();
            return ar.height * ar.width - br.height * br.width;
        }
    }
    
    public static class AreaComparatorReversed implements Comparator<Area> {

        @Override
        public int compare(Area a, Area b) {
            Rectangle ar = b.getBounds();
            Rectangle br = a.getBounds();
            return ar.height * ar.width - br.height * br.width;
        }
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // max dimensions of each shape
        int maxShapeWidth = 200;
        int maxShapeHeight = 200;
        int maxShapeDiagonal = (int) Math.ceil(Math.sqrt(Math.pow(maxShapeWidth, 2) + Math.pow(maxShapeHeight, 2)));
        
        int reshapeIterations = 2;
        int gap = 2;
        
        int layoutBaseRadius = 10;
        int layers = 10;
        int maxWidth = layoutBaseRadius * 2 + 2 * layers * maxShapeDiagonal + (layers - 1) * gap;
        int maxHeight = layoutBaseRadius * 2 + 2 * layers * maxShapeDiagonal + (layers - 1) * gap;
        
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
        
        Arrays.sort(shapes, new AreaComparatorReversed());
        
        // layout shapes in circular manner
        Point2D origin = new Point2D.Double(maxWidth/2, maxHeight/2);
        CircleOfLife life = new CircleOfLife(shapes, origin, layoutBaseRadius, gap);
        life.layout2();
        
        // find min x, max x, min y, max y
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (Area a : shapes) {
            Rectangle2D r = a.getBounds2D();
            minX = (int) Math.min(minX, r.getMinX());
            minY = (int) Math.min(minY, r.getMinY());
            maxX = (int) Math.max(maxX, r.getMaxX());
            maxY = (int) Math.max(maxY, r.getMaxY());
        }
        
        // shift towards top left corner
        for (Area a : shapes) {
            shift(a, -minX, -minY);
        }
        
        // shift origin
        origin = new Point2D.Double(origin.getX()-minX, origin.getY()-minY);
        
        // new image dimensions
        int newWidth = maxX - minX + 1;
        int newHeight = maxY - minY + 1;
        
        // output to an image
        BufferedImage bi = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D ig2 = bi.createGraphics();
        ig2.setPaint(Color.black);
                
        // fill all areas
        for (Area s : shapes) {
            ig2.fill(s);
        }
        
        // draw outlines for all areas
        ig2.setPaint(Color.MAGENTA);
        for (Area s : shapes) {
            ig2.draw(s);
        }
        
        Font font = ig2.getFont();
        font = new Font(font.getName(), font.getStyle(), 30);
        ig2.setFont(font);
        
        for (int i=0; i<numShapes; ++i) {
            Rectangle r = shapes[i].getBounds();
            int x = (int) r.getCenterX();
            int y = (int) r.getCenterY();
            ig2.drawString(Integer.toString(i+1), x, y);
        }
        
        // draw layout circle
        Shape circle = new Ellipse2D.Double(origin.getX()-layoutBaseRadius, origin.getY()-layoutBaseRadius, 2*layoutBaseRadius, 2*layoutBaseRadius);
        ig2.setPaint(Color.CYAN);
        ig2.fill(circle);
        
        // write image
        try {
            ImageIO.write(bi, "PNG", new File(System.getProperty("user.home") + "/sandbox/circleOfLife.png"));
        } catch (IOException ex) {
            Logger.getLogger(CircleOfLife.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
}
