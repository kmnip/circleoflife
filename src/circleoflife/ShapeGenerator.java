/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package circleoflife;

import java.awt.Rectangle;
import java.awt.geom.Area;
import java.util.ArrayDeque;
import java.util.concurrent.ThreadLocalRandom;

/**
 *
 * @author gengar
 */
public class ShapeGenerator {
    
    public static Area getRandomShape(int maxWidth, int maxHeight, int reshapeIterations) {
        // randomize width and height
        int width = ThreadLocalRandom.current().nextInt(maxWidth/2, maxWidth + 1);
        int height = ThreadLocalRandom.current().nextInt(maxHeight/2, maxHeight + 1);

        // initial area is a rectangle
        Area s = new Area(new Rectangle(0, 0, width, height));
        
        // create random rectangular slices from corners
        int maxSliceWidth = width/2;
        int maxSliceHeight = height/2;
        int w,h;
        
        for (int i=0; i<reshapeIterations; ++i) {
            // slice from top left corner
            w = ThreadLocalRandom.current().nextInt(maxSliceWidth);
            h = ThreadLocalRandom.current().nextInt(maxSliceHeight);                
            s.subtract(new Area(new Rectangle(0, 0, w, h)));

            // slice from top right corner
            w = ThreadLocalRandom.current().nextInt(maxSliceWidth);
            h = ThreadLocalRandom.current().nextInt(maxSliceHeight);
            s.subtract(new Area(new Rectangle(width-w, 0, w, h)));

            // slice from bottom right corner
            w = ThreadLocalRandom.current().nextInt(maxSliceWidth);
            h = ThreadLocalRandom.current().nextInt(maxSliceHeight);
            s.subtract(new Area(new Rectangle(width-w, height-h, w, h)));

            // slice from bottom left corner
            w = ThreadLocalRandom.current().nextInt(maxSliceWidth);
            h = ThreadLocalRandom.current().nextInt(maxSliceHeight);
            s.subtract(new Area(new Rectangle(0, height-h, w, h)));
        }
        
        return s;
    }
    
    public static String serializeShape(String id, Area a) {
        ArrayDeque<String> items = new ArrayDeque<>();
        items.add(id);
        
        Rectangle r = a.getBounds();
        int width = r.width;
        int height = r.height;
        
        items.add(Integer.toString(width));
        items.add(Integer.toString(height));
        
        for (int y=0; y<height; ++y) {
            // find min X
            for (int x=0; x<width; ++x) {
                if (a.contains(x, y)) {
                    items.add(Integer.toString(x));
                    break;
                }
            }
            
            // find max X
            for (int x=width-1; x>=0; --x) {
                if (a.contains(x, y)) {
                    items.add(Integer.toString(x));
                    break;
                }
            }
        }
        
        for (int x=0; x<width; ++x) {
            // find min Y
            for (int y=0; y<height; ++y) {
                if (a.contains(x, y)) {
                    items.add(Integer.toString(y));
                    break;
                }
            }
            
            // find max Y
            for (int y=height-1; y>=0; --y) {
                if (a.contains(x, y)) {
                    items.add(Integer.toString(y));
                    break;
                }
            }
        }
        
        return String.join("\t", items);
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
//        int maxWidth = 5;
//        int maxHeight = 7;
//        int numShapes = 3;
        
        int maxWidth = Integer.parseInt(args[0]);
        int maxHeight = Integer.parseInt(args[1]);
        int numShapes = Integer.parseInt(args[2]);
        
        for (int i=0; i<numShapes; ++i) {
            Area a = getRandomShape(maxWidth, maxHeight, 2);
            String line = serializeShape(Integer.toString(i), a);
            System.out.println(line);
        }
    }
}
