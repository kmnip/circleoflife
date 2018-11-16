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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 *
 * @author kmnip
 */
public class CircleOfLife {
    
    private final ArrayList<MyShape> shapes;
    private final Point2D origin;
    private final int layoutBaseRadius;
    private final Area baseCircle;
    private final int gap;
    private final boolean nonZeroGap;
    
    public CircleOfLife(ArrayList<MyShape> shapes, Point2D origin, int layoutBaseRadius, int gap) {
        this.shapes = shapes;
        this.origin = origin;
        this.layoutBaseRadius = layoutBaseRadius;
        this.gap = gap;
        this.nonZeroGap = gap > 0;
        this.baseCircle = new Area(new Ellipse2D.Double(origin.getX()-layoutBaseRadius, origin.getY()-layoutBaseRadius, 2*layoutBaseRadius, 2*layoutBaseRadius));
    }
    
    private static class MyShape {
        Area area;
        String id;
        int orientation;
        
        public MyShape(Area area, String id) {
            this.id = id;
            this.area = area;
            this.orientation = 0;
        }
        
        public MyShape(Area area, String id, int orientation) {
            this.id = id;
            this.area = area;
            this.orientation = orientation;
        }
        
        public void rotate() {
            rotateForward(area);
            ++orientation;
            
            if (orientation > 3) {
                orientation = 0;
            }
        }
        
        public void rotate(int orientation) {
            while (this.orientation != orientation) {
                this.rotate();
            }
        }
        
        public Point getPosition() {
            return this.area.getBounds().getLocation();
        }
    }
    
    private static MyShape parseLine(String line) {
        /*
        ID WIDTH HEIGHT X_MIN_1 X_MAX_1 ... X_MIN_H X_MAX_H Y_MIN_1 Y_MAX_1 ... Y_MIN_W Y_MAX_W
        */
        String[] items = line.trim().split("\t");
        
        String id = items[0];
        int width = Integer.parseInt(items[1]);
        int height = Integer.parseInt(items[2]);
        
        int[] rows = new int[height*2];
        int[] cols = new int[width*2];
        
        int base = 3;
        for (int i=0; i<height*2; ++i) {
            rows[i] = Integer.parseInt(items[i+base]);
        }

        base = 3 + height * 2;
        for (int i=0; i<width*2; ++i) {
            cols[i] = Integer.parseInt(items[i+base]);
        }
        
        Area a = createArea(width, height, rows, cols);
        
        return new MyShape(a, id);
    }
    
    private static Area createArea(int width, int height, int[] rows, int[] cols) {
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
    
    private static void shift(Area a, int x, int y) {
        AffineTransform t = new AffineTransform();
        t.translate(x, y);
        a.transform(t);
    }
    
    private static void move(Area a, Point2D p) {
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
    
    private static void rotateBackward(Area a) {
        rotate(a, 3);
    }
    
    private static void rotate(Area a, int numQuadrants) {
        Rectangle r = a.getBounds();
        
        AffineTransform at = new AffineTransform();
        at.quadrantRotate(numQuadrants, r.getCenterX(), r.getCenterY());
        
        a.transform(at);
    }
    
    private static boolean hasSimpleOverlap(Area a1, Area a2) {
        return a1.intersects(a2.getBounds2D());
    }
    
    private boolean overlapsBaseCircle(Area a) {
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
    
    private static boolean hasOverlap(Area a1, Area a2) {
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
    
    private static boolean hasOverlap(Area a, ArrayDeque<Area> others) {
        for (Area other : others) {
            if (hasOverlap(a, other)) {
                return true;
            }
        }
        
        return false;
    }
    
    private static boolean hasSimpleOverlap(Area a, ArrayDeque<Area> others) {
        for (Area other : others) {
            if (hasSimpleOverlap(a, other)) {
                return true;
            }
        }
        
        return false;        
    }
    
    private static boolean hasSimpleGap(Area a, int gap, ArrayDeque<Area> others) {
        // wiggle in all 4 directions and check for overlaps
                
        // up
        shift(a, 0, -gap);
        if (hasSimpleOverlap(a, others)) {
            shift(a, 0, gap);
            return false;
        }
        
        // down
        shift(a, 0, gap);
        if (hasSimpleOverlap(a, others)) {
            shift(a, 0, -gap);
            return false;
        }
        
        // left
        shift(a, -gap, 0);
        if (hasSimpleOverlap(a, others)) {
            shift(a, gap, 0);
            return false;
        }
        
        // right
        shift(a, gap, 0);
        if (hasSimpleOverlap(a, others)) {
            shift(a, -gap, 0);
            return false;
        }
        
        // up-left
        shift(a, -gap, -gap);
        if (hasSimpleOverlap(a, others)) {
            shift(a, gap, gap);
            return false;
        }
        
        // up-right
        shift(a, gap, -gap);
        if (hasSimpleOverlap(a, others)) {
            shift(a, -gap, gap);
            return false;
        }
        
        // down-left
        shift(a, -gap, gap);
        if (hasSimpleOverlap(a, others)) {
            shift(a, gap, -gap);
            return false;
        }
        
        // down-right
        shift(a, gap, gap);
        if (hasSimpleOverlap(a, others)) {
            shift(a, -gap, -gap);
            return false;
        }
        
        return true;
    }
    
    private static boolean hasGap(Area a, int gap, ArrayDeque<Area> others) {
        // wiggle in all 4 directions and check for overlaps
                
        // up
        shift(a, 0, -gap);
        if (hasOverlap(a, others)) {
            shift(a, 0, gap);
            return false;
        }
        
        // down
        shift(a, 0, gap);
        if (hasOverlap(a, others)) {
            shift(a, 0, -gap);
            return false;
        }
        
        // left
        shift(a, -gap, 0);
        if (hasOverlap(a, others)) {
            shift(a, gap, 0);
            return false;
        }
        
        // right
        shift(a, gap, 0);
        if (hasOverlap(a, others)) {
            shift(a, -gap, 0);
            return false;
        }
        
        // up-left
        shift(a, -gap, -gap);
        if (hasOverlap(a, others)) {
            shift(a, gap, gap);
            return false;
        }
        
        // up-right
        shift(a, gap, -gap);
        if (hasOverlap(a, others)) {
            shift(a, -gap, gap);
            return false;
        }
        
        // down-left
        shift(a, -gap, gap);
        if (hasOverlap(a, others)) {
            shift(a, gap, -gap);
            return false;
        }
        
        // down-right
        shift(a, gap, gap);
        if (hasOverlap(a, others)) {
            shift(a, -gap, -gap);
            return false;
        }
        
        return true;
    }
    
    private static int diagonal(Area a) {
        Rectangle b = a.getBounds();
        return (int) Math.ceil(Math.sqrt(b.width*b.width + b.height*b.height));
    }
    
    private static double distance(Area a1, Area a2) {
        // distance between center points of 2 areas
        
        Rectangle b1 = a1.getBounds();
        Rectangle b2 = a2.getBounds();

        Point2D c1 = new Point2D.Double(b1.getCenterX(), b1.getCenterY());
        return c1.distance(b2.getCenterX(), b2.getCenterY());
    }
    
    private static double distance(Area a, Point2D p) {
        // distance between center point of area to point
        
        Rectangle b = a.getBounds();
        Point2D c = new Point2D.Double(b.getCenterX(), b.getCenterY());
        return c.distance(p);
    }
    
    private static double maxDistance(Area a, Point2D p) {
        double maxDistance = 0;
        
        PathIterator itr = a.getPathIterator(null);
        double[] point = new double[2];
        for (; !itr.isDone(); itr.next()) {
            itr.currentSegment(point);
            maxDistance = Math.max(maxDistance, p.distance(point[0], point[1]));
        }
            
        return maxDistance;
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
    
    private void layout2() {
        int steps = 64;
        
        double[] angles = new double[steps];
        for (int i=0; i<steps; ++i) {
            angles[i] = 2 * Math.PI * i / steps;
        }
        
        ArrayDeque<Area> ringMembers = new ArrayDeque<>();
        int currentRadius = this.layoutBaseRadius;
        int ox = (int) origin.getX();
        int oy = (int) origin.getY();
        
        System.out.println(shapes.size() + " input shapes");
        
        int id = 1;
        for (MyShape s : shapes) {
            System.out.println("Placing shape " + id++);
            
            Area a = s.area;
            double diag = diagonal(a);
            
            double bestDistance = Double.POSITIVE_INFINITY;
            int bestOrientation = -1;
            Point2D bestCoord = null;
            
            while (bestCoord == null) {
                for (double angle : angles) {
                    int x = (int) Math.ceil(ox + (currentRadius + diag) * Math.sin(angle));
                    int y = (int) Math.ceil(oy + (currentRadius + diag) * Math.cos(angle));
                    Point2D p = new Point2D.Double(x, y);

                    for (int i=0; i<4; ++i) {
                        s.rotate(i);
                        
                        // move to starting coord
                        moveNW(a, p);

                        if (!overlapsBaseCircle(a) && !hasOverlap(a, ringMembers) && (!nonZeroGap || hasGap(a, gap, ringMembers))) {
                            // shape did not overlap at starting coord 

                            // slide towards origin
                            slideTowardsOrigin(a, ringMembers);

                            // calculate distance to origin
                            double d = distance(a, origin);
                            if (d < bestDistance) {
                                bestDistance = d;
                                bestOrientation = s.orientation;
                                Rectangle2D bound = a.getBounds();
                                bestCoord = new Point2D.Double(bound.getX(), bound.getY());
                            }
                        }
                    }
                }
                
                if (bestCoord == null) {
                    currentRadius += diag;
                }
            }
            
            // move to closest point
            s.rotate(bestOrientation);
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
    private void layout() {
        // layout areas around the circle

        ArrayDeque<Area> ringMembers = new ArrayDeque<>();
        
        int currentRadius = this.layoutBaseRadius;
        int lastQuadrant = 1;
        Area a = shapes.get(0).area;
        if (isHorizontal(a)) {
            rotateForward(a);
        }  
        layoutQ1(a, currentRadius, ringMembers, false);
        
        ringMembers.add(a);
        double maxAreaDistance = maxDistance(a, origin);
        
        int numAreas = shapes.size();
        for (int i=1; i<numAreas; ++i) {
            System.out.println(i);
            
            a = shapes.get(i).area;
            
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
            
            if (overlapsBaseCircle(a) || hasOverlap(a, ringMembers) || (nonZeroGap && !hasGap(a, gap, ringMembers))) {
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
            
            if (overlapsBaseCircle(a) || hasOverlap(a, ringMembers) || (nonZeroGap && !hasGap(a, gap, ringMembers))) {
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
            
            if (overlapsBaseCircle(a) || hasOverlap(a, ringMembers) || (nonZeroGap && !hasGap(a, gap, ringMembers))) {
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
            
            if (overlapsBaseCircle(a) || hasOverlap(a, ringMembers) || (nonZeroGap && !hasGap(a, gap, ringMembers))) {
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
            
            if (overlapsBaseCircle(a) || hasSimpleOverlap(a, ringMembers) || (nonZeroGap && !hasSimpleGap(a, gap, ringMembers))) {
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
            
            if (overlapsBaseCircle(a) || hasSimpleOverlap(a, ringMembers) || (nonZeroGap && !hasSimpleGap(a, gap, ringMembers))) {
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
            
            if (overlapsBaseCircle(a) || hasSimpleOverlap(a, ringMembers) || (nonZeroGap && !hasSimpleGap(a, gap, ringMembers))) {
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
            
            if (overlapsBaseCircle(a) || hasSimpleOverlap(a, ringMembers) || (nonZeroGap && !hasSimpleGap(a, gap, ringMembers))) {
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
    
    private int checkQuadrant(Point2D p) {
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
        
    private static class AreaComparator implements Comparator<Area> {

        @Override
        public int compare(Area a, Area b) {
            Rectangle ar = a.getBounds();
            Rectangle br = b.getBounds();
            return ar.height * ar.width - br.height * br.width;
        }
    }
    
    private static class AreaComparatorReversed implements Comparator<Area> {

        @Override
        public int compare(Area a, Area b) {
            Rectangle ar = b.getBounds();
            Rectangle br = a.getBounds();
            return ar.height * ar.width - br.height * br.width;
        }
    }
    
    private static class MyShapeComparator implements Comparator<MyShape> {
        @Override
        public int compare(MyShape a, MyShape b) {
            Rectangle ar = a.area.getBounds();
            Rectangle br = b.area.getBounds();
            return ar.height * ar.width - br.height * br.width;
        }
    }
    
    private static class MyShapeReversedComparator implements Comparator<MyShape> {
        @Override
        public int compare(MyShape a, MyShape b) {
            Rectangle ar = b.area.getBounds();
            Rectangle br = a.area.getBounds();
            return ar.height * ar.width - br.height * br.width;
        }
    }
    
    private static int estimateLayoutWidth(int baseRadius, int numShapes, int maxDiagonal, int gap) {
        int radius = baseRadius;
        
        for (; numShapes > 0; radius += maxDiagonal + gap) {
            int circumference = (int) Math.floor(2 * Math.PI * radius);
            int numShapesFitted = circumference/(maxDiagonal + gap);
            numShapes -= numShapesFitted;
        }
        
        return 2*radius + 2*gap;
    }
    
    private static class MyConfig {
        final String SEP = "=";
        final String INPUT_SHAPES_DIMENSIONS_PATH = "shapesDimensionsPath";
        final String OUTPUT_LAYOUT_PATH = "outputLayoutPath";
        final String OUTPUT_IMAGE_PATH = "outputImagePath";
        final String LAYOUT_BASE_RADIUS = "layoutBaseRadius";
        final String INTER_SHAPE_GAP_SIZE = "interShapeGapSize";
        
        String shapesDimensionsPath = null;
        String outputLayoutPath = null;
        String outputImagePath = null;
        int layoutBaseRadius = 10;
        int interShapeGapSize = 0;
        
        private void parseFile(String path) throws IOException {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(path)));
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                String[] items = line.split(SEP, 2);
                if (items.length == 2) {
                    String key = items[0].trim();
                    String value = items[1].trim();

                    switch(key) {
                        case INPUT_SHAPES_DIMENSIONS_PATH:
                            shapesDimensionsPath = value;
                            break;
                        case OUTPUT_LAYOUT_PATH:
                            outputLayoutPath = value;
                            break;
                        case OUTPUT_IMAGE_PATH:
                            outputImagePath = value;
                            break;
                        case LAYOUT_BASE_RADIUS:
                            layoutBaseRadius = Integer.parseInt(value);
                            break;
                        case INTER_SHAPE_GAP_SIZE:
                            interShapeGapSize = Integer.parseInt(value);
                            break;
                    }
                }
            }
            br.close();
        }
    }
        
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Please provide the path to your configuration file.");
            System.exit(0);
        }
        
        MyConfig config = new MyConfig();
        try {
            config.parseFile(args[0]);
        }
        catch (IOException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
                
        ArrayList<MyShape> shapes = new ArrayList<>();
        int maxDiagonal = 0;
        
        try {
            System.out.println("Parsing shapes from `" + config.shapesDimensionsPath + "`...");
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(config.shapesDimensionsPath)));
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                MyShape s = parseLine(line);
                shapes.add(s);
                
                maxDiagonal = Math.max(maxDiagonal, diagonal(s.area));
            }
            br.close();
        }
        catch (IOException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
        
        int layoutBaseRadius = config.layoutBaseRadius;
        int gap = config.interShapeGapSize;
        int maxLayoutWidth = estimateLayoutWidth(layoutBaseRadius, shapes.size(), maxDiagonal, gap);
        
        // sort shapes from largest to smallest
        Collections.sort(shapes, new MyShapeReversedComparator());
        
        // layout shapes in circular manner
        Point2D origin = new Point2D.Double(maxLayoutWidth/2, maxLayoutWidth/2);
        CircleOfLife life = new CircleOfLife(shapes, origin, layoutBaseRadius, gap);
        life.layout2();
        
        // find min x, max x, min y, max y
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (MyShape a : shapes) {
            Rectangle2D r = a.area.getBounds2D();
            minX = (int) Math.min(minX, r.getMinX());
            minY = (int) Math.min(minY, r.getMinY());
            maxX = (int) Math.max(maxX, r.getMaxX());
            maxY = (int) Math.max(maxY, r.getMaxY());
        }
        
        // shift towards top left corner
        for (MyShape s : shapes) {
            shift(s.area, -minX, -minY);
        }
        
        // write layout to file
        try {
            System.out.println("Writing layout to `" + config.shapesDimensionsPath + "`...");
            FileWriter writer = new FileWriter(config.outputLayoutPath);
            for (MyShape s : shapes) {
                Point p = s.getPosition();
                writer.write(s.id + '\t' + p.x + '\t' + p.y + '\t' + s.orientation + '\n');
            }
        }
        catch (IOException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
        
        // shift origin
        origin = new Point2D.Double(origin.getX()-minX, origin.getY()-minY);
        
        // new image dimensions
        int newWidth = maxX - minX + 1;
        int newHeight = maxY - minY + 1;
        
        // output to an image
        System.out.println("Drawing layout to `" + config.outputImagePath + "`...");
        BufferedImage bi = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D ig2 = bi.createGraphics();
        ig2.setPaint(Color.black);
                
        // fill all areas
        for (MyShape s : shapes) {
            ig2.fill(s.area);
        }
        
        // draw outlines for all areas
        ig2.setPaint(Color.MAGENTA);
        for (MyShape s : shapes) {
            ig2.draw(s.area);
        }
        
        Font font = ig2.getFont();
        font = new Font(font.getName(), font.getStyle(), 30);
        ig2.setFont(font);
        
        for (MyShape s : shapes) {
            Rectangle r = s.area.getBounds();
            int x = (int) r.getCenterX();
            int y = (int) r.getCenterY();
            ig2.drawString(s.id, x, y);
        }
        
        // draw layout circle
        Shape circle = new Ellipse2D.Double(origin.getX()-layoutBaseRadius, origin.getY()-layoutBaseRadius, 2*layoutBaseRadius, 2*layoutBaseRadius);
        ig2.setPaint(Color.CYAN);
        ig2.fill(circle);
        
        // write image
        try {
            ImageIO.write(bi, "PNG", new File(config.outputImagePath));
        } catch (IOException ex) {
            Logger.getLogger(CircleOfLife.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        System.out.println("Done");
    }
    
}
