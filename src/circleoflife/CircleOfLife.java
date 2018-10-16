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

    public Area getRandomShape(int maxWidth, int maxHeight) {
        // initial area is a rectangle
        Area s = new Area(new Rectangle(0, 0, maxWidth, maxHeight));
        
        // create random rectangular slices from corners
        int maxSliceWidth = maxWidth/2;
        int maxSliceHeight = maxHeight/2;
        Random r = new Random();
        int w,h;
        int iterations = 2;
        
        for (int i=0; i<iterations; ++i) {
            // top left corner
            w = r.nextInt(maxSliceWidth);
            h = r.nextInt(maxSliceHeight);                
            s.subtract(new Area(new Rectangle(0, 0, w, h)));

            // top right corner
            w = r.nextInt(maxSliceWidth);
            h = r.nextInt(maxSliceHeight);
            s.subtract(new Area(new Rectangle(maxWidth-w, 0, w, h)));

            // bottom right corner
            w = r.nextInt(maxSliceWidth);
            h = r.nextInt(maxSliceHeight);
            s.subtract(new Area(new Rectangle(maxWidth-w, maxHeight-h, w, h)));

            // bottom left corner
            w = r.nextInt(maxSliceWidth);
            h = r.nextInt(maxSliceHeight);
            s.subtract(new Area(new Rectangle(0, maxHeight-h, w, h)));
        }
        
        return s;
    }
    
    public void rotateShapeForward(Area a) {
        rotateShape(a, 1);
    }
    
    public void rotateShapeBackward(Area a) {
        rotateShape(a, 3);
    }
    
    public void rotateShape(Area a, int numQuadrants) {
        Rectangle r = a.getBounds();
        
        AffineTransform at = new AffineTransform();
        at.quadrantRotate(numQuadrants, r.getCenterX(), r.getCenterY());
        
        a.transform(at);
    }
    
    public boolean hasOverlap(Area a1, Area a2) {
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
    
    public double distance(Area a1, Area a2) {
        if (!hasOverlap(a1, a2)) {
            // use the distance between center points for now
            Rectangle b1 = a1.getBounds();
            Rectangle b2 = a2.getBounds();
            
            Point2D c1 = new Point2D.Double(b1.getCenterX(), b1.getCenterY());
            return c1.distance(b2.getCenterX(), b2.getCenterY());
        }
        
        return -1;
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {        
        int maxShapeWidth = 2000;
        int maxShapeHeight = 1000;
        
        int outlineDiameter = 60000;
        int maxWidth = 60000 + 10 * maxShapeWidth;
        int maxHeight = 60000 + 10 * maxShapeHeight;
        
        /**@TODO*/
    }
    
}
