package org.heckcorp.domination;

import java.awt.Point;

public final class Calculator {
    /**
     * @param p1
     * @param p2
     * @return
     * @pre p1 != null
     * @pre p2 != null
     */
    public static int distance(Positionable p1, Positionable p2) {
        return distance(p1.getPosition(), p2.getPosition());
    }

    /**
     * @param p1
     * @param p2
     * @return the distance between p1 and p2.
     * 
     * @post result >= 0
     */
    public static int distance(Point p1, Point p2) {
        int distance = 0;
        
        if (p1 == p2) {
            distance = 0;
        } else if (p1.x == p2.x) {
            distance = p1.y - p2.y;
        } else {
            // even->odd 0/+1
            // odd-even -1/0
            
            int xDiff = (int) Math.abs(p1.x - p2.x);
    
            int minMod = xDiff-1;
            int maxMod = xDiff-1;
            
            if (p1.x % 2 == 0 && p2.x % 2 != 0) {
                minMod = minMod - 1;
            } else if (p1.x % 2 != 0 && p2.x % 2 == 0) {
                maxMod = maxMod - 1;
            }
            
            // By moving diagonally, we can move from p1.y to between
            // p1.y-minMod and p1.y+maxMod.
            int minY = p1.y - minMod;
            int maxY = p1.y + maxMod;
            int yDiff = 0;
            
            if (p2.y < minY || p2.y > maxY) {
                yDiff = (int) Math.min(Math.abs(minY - p2.y), Math.abs(maxY - p2.y));
            }
    
            distance = xDiff + yDiff;
        }
    
        if (distance < 0) {
            distance = distance * -1;
        }
    
        return distance;
    }
}
