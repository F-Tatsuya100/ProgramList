/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package movement;

import core.Coord;
import core.Settings;

/**
 * Random Walk movement model
 * 
 * @author Frans Ekman
 */
public class RandomWalk extends MovementModel implements SwitchableMovement {

        public static final String MOVEMENT_MODEL_NS = "MovementModel";
            public static final String INITIAL_RANGEX_S = "initialRangeX";
        public static final String INITIAL_RANGEY_S = "initialRangeY";
       public static final String LOCATION_S = "nodeLocation";
        
        private Coord loc;
        private Coord lastWaypoint;
        private double minDistance;
        private double maxDistance;
        private String gid;
        private int initialRangeX;
        private int initialRangeY;
              
        
    public RandomWalk(Settings settings) {
                super(settings);
                int coords[];

                coords = settings.getCsvInts(LOCATION_S, 2);
                this.loc = new Coord(coords[0],coords[1]);

                minDistance = 0;
                maxDistance = 50;
        }
        
        private RandomWalk(RandomWalk rwp) {
                super(rwp);
                minDistance = rwp.minDistance;
                maxDistance = rwp.maxDistance;
                this.loc = rwp.loc;
        }
        
        /**
         * Returns a possible (random) placement for a host
         * @return Random position on the map
         */
        @Override
        public Coord getInitialLocation() {
                Settings s = new Settings(MOVEMENT_MODEL_NS);
                this.initialRangeX = s.getInt(INITIAL_RANGEX_S);
                this.initialRangeY = s.getInt(INITIAL_RANGEY_S);
                assert rng != null : "MovementModel not initialized!";
                double x = 0;
                double y = 0;

                if (this.gid.equals("D")) {
                    x = this.loc.getX();
                    y = this.loc.getY();
                } else {
                    double radiusX = this.initialRangeX / 2;
                    double radiusY = this.initialRangeY / 2;
                    double differenceX = ((getMaxX() / 2) - this.initialRangeX) + radiusX;
                    double differenceY = ((getMaxY() / 2) - this.initialRangeY) + radiusY;
                    x = (rng.nextDouble() * this.initialRangeX) + differenceX;
                    y = (rng.nextDouble() * this.initialRangeY) + differenceY;
                }
                Coord c = new Coord(x,y);

                this.lastWaypoint = c;
                return c;
        }
        
        @Override
        public Path getPath() {
                Path p;
                p = new Path(generateSpeed());
                p.addWaypoint(lastWaypoint.clone());
                double maxX = getMaxX();
                double maxY = getMaxY();
                
                
                Coord c = null;
                while (true) {
                        
                        double angle = rng.nextDouble() * 2 * Math.PI;
                        double distance = minDistance + rng.nextDouble() * 
                                (maxDistance - minDistance);
                        
                        double x = lastWaypoint.getX() + distance * Math.cos(angle);
                        double y = lastWaypoint.getY() + distance * Math.sin(angle);
                
                        c = new Coord(x,y);
                        
                        if (x > 0 && y > 0 && x < maxX && y < maxY) {
                                break;
                        }
                }
                
                p.addWaypoint(c);
                
                this.lastWaypoint = c;
                return p;
        }
        
        @Override
        public RandomWalk replicate() {
                return new RandomWalk(this);
        }

        public Coord getLastLocation() {
                return lastWaypoint;
        }

        public void setLocation(Coord lastWaypoint) {
                this.lastWaypoint = lastWaypoint;
        }

        public boolean isReady() {
                return true;
        }

        public void setGroupId(String gid) {
            this.gid = gid;
        }
}
