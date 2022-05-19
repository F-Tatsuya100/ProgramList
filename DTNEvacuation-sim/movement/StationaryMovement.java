/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package movement;

import core.Coord;
import core.Settings;

import core.SimScenario;
import java.util.List;
import java.util.ArrayList;
import java.lang.Math;


/**
 * A dummy stationary "movement" model where nodes do not move.
 * Might be useful for simulations with only external connection events. 
 */
public class StationaryMovement extends MovementModel {
    /** movement model class -setting id ({@value})*/
    public static final String MOVEMENT_MODEL_NS = "movementModel";
    /** Per node group setting for setting the location ({@value}) */
    public static final String LOCATION_S = "nodeLocation";
    /** namespace for host group settings ({@value})*/
    public static final String GROUP_NS = "Group";
    /** number of hosts in the group -setting id ({@value})*/
    public static final String NROF_HOSTS_S = "nrofHosts";
    private Coord loc; /** The location of the nodes */
    private String gid;
    private static int CMCount = 0;
    private static List<Coord> CMCoordList;
    
    /**
     * Creates a new movement model based on a Settings object's settings.
     * @param s The Settings object where the settings are read from
     */
    public StationaryMovement(Settings s) {
        super(s);
        int coords[];
		Coord location;
		int x;
		int y;
		assert rng != null : "MovementModel not initialized!";
                
        coords = s.getCsvInts(LOCATION_S, 2);
        this.loc = new Coord(coords[0],coords[1]);

		s = new Settings(GROUP_NS+4);
		int nrofCMHosts = s.getInt(NROF_HOSTS_S);

		CMCoordList = new ArrayList<Coord>();
		s.setNameSpace(MovementModel.MOVEMENT_MODEL_NS);
		int [] worldSize = s.getCsvInts(MovementModel.WORLD_SIZE, 2);
		int worldTileSizeX = worldSize[0]/2;
		int worldTileSizeY = worldSize[1]/2;
		int tileSize = (int)Math.sqrt((double)nrofCMHosts);
		double randRange = 0.1;
		if (tileSize > 2) {
		    randRange = (tileSize - 2) * 0.05;
		}
		double randX = 0;
		double randY = 0;
		for (int i = 0 ; i < tileSize ; i++) {
		    for (int j = 0 ; j < tileSize ; j++) {
			while (true) {
			    randX = rng.nextDouble();
			    if (!(randX < randRange || (1 - randRange) < randX)) {
				break;
			    }
			}
			x = (int)(((double)worldSize[0] / (double)tileSize) * (i + randX));
			while (true) {
			    randY = rng.nextDouble();
			    if (!(randY < randRange || (1 - randRange) < randY)) {
				break;
			    }
			}
			y = (int)(((double)worldSize[1] / (double)tileSize) * (j + randY));
			location = new Coord(x,y);
			CMCoordList.add(location);
		    }
		}
    }
        
    /**
     * Copy constructor. 
     * @param sm The StationaryMovement prototype
     */
    public StationaryMovement(StationaryMovement sm) {
        super(sm);
        this.loc = sm.loc;
    }

    /**
     * Returns the only location of this movement model
     * @return the only location of this movement model
     */
    @Override
    public Coord getInitialLocation() {
		double x = 0;
        double y = 0;
		
		if(this.gid.equals("CM")){

		    int i = CMCount;
		    x = CMCoordList.get(i).getX();
		    y = CMCoordList.get(i).getY();
			Coord c = new Coord(x,y);
			CMCount++;
			return c;
		}
		return loc;
    }
        
    /**
     * Returns a single coordinate path (using the only possible coordinate)
     * @return a single coordinate path
     */
    @Override
    public Path getPath() {
        Path p = new Path(0);
        p.addWaypoint(loc);
        return p;
    }
        
    @Override
    public double nextPathAvailable() {
        return Double.MAX_VALUE;        // no new paths available
    }
        
    @Override
    public StationaryMovement replicate() {
        return new StationaryMovement(this);
    }
    public void setGroupId(String gid) {
        this.gid = gid;
    }

}
