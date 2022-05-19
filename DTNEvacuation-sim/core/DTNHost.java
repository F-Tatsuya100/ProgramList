/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.Iterator;
import java.util.stream.Collectors;

import gui.playfield.PlayField;

import movement.MovementModel;
import movement.Path;
import routing.MessageRouter;
import routing.util.RoutingInfo;


/**
 * A DTN capable host.
 */
public class DTNHost implements Comparable<DTNHost> {
    private enum State {
        go,
        back,
        stop,
        stay
    };

    private static int nextAddress = 0;
    private int address;

    private Coord location;         // where is the host
    private Coord destination;      // where is it going
    private Coord currentTileCoord;
    private static Coord APlocation;
    private int nextTile;

    private MessageRouter router;
    private MovementModel movement;
    private Path path;
    private double speed;
    private double nextTimeToMove;
    private String name;
    private List<MessageListener> msgListeners;
    private List<MovementListener> movListeners;
    private List<NetworkInterface> net;
    private List<Coord> locationList;
    private List<Coord> selectedTileList;
    private Map<Integer,List<Coord>> evacuationRouteList;
    private Map<Integer,Coord> arrivalCMList;
    private Map<RouteEntry,List<Coord>> routeList;
    private Map<Integer,TileMap> areaTileMapList;
    private ModuleCommunicationBus comBus;

    private Map<Integer,Integer> CMevacuationUpdateCount;

    public static final String SCENARIO_NS = "Scenario";
    public static final String EVACUATION_MODE_S = "evacuationMode";
    public static final String TILE_SIZEX_S = "tileSizeX";
    public static final String TILE_SIZEY_S = "tileSizeY";
    public static final String TEMPLATE_TILE_SIZEX_S = "templateTileSizeX";
    public static final String TEMPLATE_TILE_SIZEY_S = "templateTileSizeY";
    public static final String AREA_TILE_SIZEX_S = "areaTileSizeX";
    public static final String AREA_TILE_SIZEY_S = "areaTileSizeY";
    public static final String START_FLY_TIME_S = "startFlyTime";
        
    private String evacuationMode;
    private int tileSizeX;
    private int tileSizeY;
    private int templateTileSizeX;
    private int templateTileSizeY;
    private int areaTileSizeX;
    private int areaTileSizeY;
    private double startFlyTime;
    private boolean isFlying;
    private int nrofTiles;
    private int selectedHostAddress; 
    private boolean safetyFlag;
    private RouteEntry routeEntry;
    private TileMap tileMap;
    private TileMap templateTileMap;
    private TileMap areaTileMap;
    private DTNHost droneHost;
    private static  DTNHost apHost;
    private State state;
    private int initialNumber;
    private int DfromKey;

    public String gid;
    public boolean AParrival;
    public int evacuationUpdateCount;

    private PlayField playField;

    public List<Integer> checkedCMaddressList;
    public int waitCMaddress;


    public static int evacuNum = 0;
    public static int DroneAddress;
    private boolean test = false;

    static {
        DTNSim.registerForReset(DTNHost.class.getCanonicalName());
        reset();
    }
    /**
     * Creates a new DTNHost.
     * @param msgLs Message listeners
     * @param movLs Movement listeners
     * @param groupId GroupID of this host
     * @param interf List of NetworkInterfaces for the class
     * @param comBus Module communication bus object
     * @param mmProto Prototype of the movement model of this host
     * @param mRouterProto Prototype of the message router of this host
     */
    public DTNHost(List<MessageListener> msgLs,
                   List<MovementListener> movLs,
                   String groupId, List<NetworkInterface> interf,
                   ModuleCommunicationBus comBus, 
                   MovementModel mmProto, MessageRouter mRouterProto) {
        Settings s = new Settings(SCENARIO_NS);
        this.evacuationMode = s.valueFillString(s.getSetting(EVACUATION_MODE_S));
        this.tileSizeX = s.getInt(TILE_SIZEX_S);
        this.tileSizeY = s.getInt(TILE_SIZEY_S);
        this.templateTileSizeX = s.getInt(TEMPLATE_TILE_SIZEX_S);
        this.templateTileSizeY = s.getInt(TEMPLATE_TILE_SIZEY_S);
        this.areaTileSizeX = s.getInt(AREA_TILE_SIZEX_S);
        this.areaTileSizeY = s.getInt(AREA_TILE_SIZEY_S);
        this.startFlyTime = s.getInt(START_FLY_TIME_S);
        this.isFlying = false;
        this.comBus = comBus;
        this.location = new Coord(0,0);
        this.address = getNextAddress();
        this.name = groupId+address;
        this.net = new ArrayList<NetworkInterface>();
        this.locationList = new ArrayList<Coord>();
        this.selectedTileList = new ArrayList<Coord>();
        this.evacuationRouteList = new HashMap<Integer,List<Coord>>();
        this.arrivalCMList = new HashMap<Integer,Coord>();
        this.routeList = new HashMap<RouteEntry,List<Coord>>();
        this.areaTileMapList = new HashMap<Integer,TileMap>();
        this.nrofTiles = 0;
        this.selectedHostAddress = 0;
        this.AParrival = false;
        this.DfromKey = -1;
        this.evacuationUpdateCount = 0;
        this.CMevacuationUpdateCount = new HashMap<Integer,Integer>();

        this.checkedCMaddressList = new ArrayList<Integer>();

    
                
        for (NetworkInterface i : interf) {
            NetworkInterface ni = i.replicate();
            ni.setHost(this);
            net.add(ni);
        }       

        // TODO - think about the names of the interfaces and the nodes
        //this.name = groupId + ((NetworkInterface)net.get(1)).getAddress();

        this.msgListeners = msgLs;
        this.movListeners = movLs;

        // create instances by replicating the prototypes
        this.movement = mmProto.replicate();
        this.movement.setComBus(comBus);
        this.movement.setHost(this);
        setRouter(mRouterProto.replicate());

        movement.setGroupId(groupId);
        this.location = movement.getInitialLocation();
        int tileX = (int)this.location.getX() / this.tileSizeX;
        int tileY = (int)this.location.getY() / this.tileSizeY;
        int maxTileX = (this.movement.getMaxX() / this.tileSizeX) - 1;
        int maxTileY = (this.movement.getMaxY() / this.tileSizeY) - 1;
        if (tileX > maxTileX) {
            tileX -= 1;
        }
        if (tileY > maxTileY) {
            tileY -= 1;
        }
        int x = (tileX * this.tileSizeX) + (tileSizeX / 2);
        int y = (tileY * this.tileSizeY) + (tileSizeY / 2);
        Coord currentLocation = new Coord(x,y);
        this.location = currentLocation;
        this.currentTileCoord = new Coord(tileX,tileY);
        if (groupId.equals("AP")) {
	    apHost = this;
            this.APlocation = this.currentTileCoord;
        } else {
            this.locationList.add(this.locationList.size(), this.currentTileCoord);
        }

        if (isGid(groupId) == true) {
            this.tileMap = new TileMap();
            this.tileMap.init(maxTileX+1,maxTileY+1);
            this.tileMap.put(tileX,tileY);
                    
            this.templateTileMap = new TileMap();
            this.templateTileMap.init(this.templateTileSizeX,this.templateTileSizeY);

            int maxAreaTileX = (maxTileX + 1) / this.areaTileSizeX;
            int maxAreaTileY = (maxTileY + 1) / this.areaTileSizeY;
            this.areaTileMap = new TileMap();
            this.areaTileMap.init(maxAreaTileX,maxAreaTileY);
        } else if (groupId.equals("D")) {
            this.state = State.stop;
	    this.waitCMaddress = -1;
	    DroneAddress = this.address;
        }

        this.nextTimeToMove = movement.nextPathAvailable();
        this.path = null;

        if (movLs != null) { // inform movement listeners about the location
            for (MovementListener l : movLs) {
                l.initialLocation(this, this.location);
            }
        }
    }
        
    /**
     * Returns a new network interface address and increments the address for
     * subsequent calls.
     * @return The next address.
     */
    private synchronized static int getNextAddress() {
        return nextAddress++;   
    }

    /**
     * Reset the host and its interfaces
     */
    public static void reset() {
        nextAddress = 0;
    }

    /**
     * Returns true if this node is actively moving (false if not)
     * @return true if this node is actively moving (false if not)
     */
    public boolean isMovementActive() {
        return this.movement.isActive();
    }
        
    /**
     * Returns true if this node's radio is active (false if not)
     * @return true if this node's radio is active (false if not)
     */
    public boolean isRadioActive() {
        /* TODO: make this work for multiple interfaces */
        return this.getInterface(1).isActive();
    }

    /**
     * Set a router for this host
     * @param router The router to set
     */
    private void setRouter(MessageRouter router) {
        router.init(this, msgListeners);
        this.router = router;
    }

    /**
     * Returns the router of this host
     * @return the router of this host
     */
    public MessageRouter getRouter() {
        return this.router;
    }

    /**
     * Returns the network-layer address of this host.
     */
    public int getAddress() {
        return this.address;
    }
        
    /**
     * Returns this hosts's ModuleCommunicationBus
     * @return this hosts's ModuleCommunicationBus
     */
    public ModuleCommunicationBus getComBus() {
        return this.comBus;
    }
        
    /**
     * Informs the router of this host about state change in a connection
     * object.
     * @param con  The connection object whose state changed
     */
    public void connectionUp(Connection con) {
        this.router.changedConnection(con);
    }

    public void connectionDown(Connection con) {
        this.router.changedConnection(con);
    }

    /**
     * Returns a copy of the list of connections this host has with other hosts
     * @return a copy of the list of connections this host has with other hosts
     */
    public List<Connection> getConnections() {
        List<Connection> lc = new ArrayList<Connection>();

        for (NetworkInterface i : net) {
            lc.addAll(i.getConnections());
        }

        return lc;
    }

    /**
     * Returns the current location of this host. 
     * @return The location
     */
    public Coord getLocation() {
        return this.location;
    }

    /**
     * Returns the Path this node is currently traveling or null if no
     * path is in use at the moment.
     * @return The path this node is traveling
     */
    public Path getPath() {
        return this.path;
    }


    /**
     * Sets the Node's location overriding any location set by movement model
     * @param location The location to set
     */
    public void setLocation(Coord location) {
        this.location = location.clone();
    }

    /**
     * Sets the Node's name overriding the default name (groupId + netAddress)
     * @param name The name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the messages in a collection.
     * @return Messages in a collection
     */
    public Collection<Message> getMessageCollection() {
        return this.router.getMessageCollection();
    }

    /**
     * Returns the number of messages this node is carrying.
     * @return How many messages the node is carrying currently.
     */
    public int getNrofMessages() {
        return this.router.getNrofMessages();
    }

    /**
     * Returns the buffer occupancy percentage. Occupancy is 0 for empty
     * buffer but can be over 100 if a created message is bigger than buffer
     * space that could be freed.
     * @return Buffer occupancy percentage
     */
    public double getBufferOccupancy() {
        double bSize = router.getBufferSize();
        double freeBuffer = router.getFreeBufferSize();
        return 100*((bSize-freeBuffer)/bSize);
    }

    /**
     * Returns routing info of this host's router.
     * @return The routing info.
     */
    public RoutingInfo getRoutingInfo() {
        return this.router.getRoutingInfo();
    }

    /**
     * Returns the interface objects of the node
     */
    public List<NetworkInterface> getInterfaces() {
        return net;
    }

    /**
     * Find the network interface based on the index
     */
    public NetworkInterface getInterface(int interfaceNo) {
        NetworkInterface ni = null;
        try {
            ni = net.get(interfaceNo-1);
        } catch (IndexOutOfBoundsException ex) {
            throw new SimError("No such interface: "+interfaceNo + 
                               " at " + this);
        }
        return ni;
    }

    /**
     * Find the network interface based on the interfacetype
     */
    protected NetworkInterface getInterface(String interfacetype) {
        for (NetworkInterface ni : net) {
            if (ni.getInterfaceType().equals(interfacetype)) {
                return ni;
            }
        }
        return null;    
    }

    /**
     * Force a connection event
     */
    public void forceConnection(DTNHost anotherHost, String interfaceId, 
                                boolean up) {
        NetworkInterface ni;
        NetworkInterface no;

        if (interfaceId != null) {
            ni = getInterface(interfaceId);
            no = anotherHost.getInterface(interfaceId);

            assert (ni != null) : "Tried to use a nonexisting interfacetype "+interfaceId;
            assert (no != null) : "Tried to use a nonexisting interfacetype "+interfaceId;
        } else {
            ni = getInterface(1);
            no = anotherHost.getInterface(1);
                        
            assert (ni.getInterfaceType().equals(no.getInterfaceType())) : 
            "Interface types do not match.  Please specify interface type explicitly";
        }
                
        if (up) {
            ni.createConnection(no);
        } else {
            ni.destroyConnection(no);
        }
    }

    /**
     * for tests only --- do not use!!!
     */
    public void connect(DTNHost h) {
        Debug.p("WARNING: using deprecated DTNHost.connect(DTNHost)" +
                "Use DTNHost.forceConnection(DTNHost,null,true) instead");
        forceConnection(h,null,true);
    }

    /**
     * Updates node's network layer and router.
     * @param simulateConnections Should network layer be updated too
     */
    public void update(boolean simulateConnections) {
        if (!isRadioActive()) {
            // Make sure inactive nodes don't have connections
            tearDownAllConnections();
            return;
        }
                
        if (simulateConnections) {
            for (NetworkInterface i : net) {
                i.update();
            }
        }

        if (this.evacuationMode.equals("Proposed")) {
            if (this.gid.equals("AP") && SimClock.getTime() >= this.startFlyTime && this.isFlying == false && this.droneHost != null) {
                int maxTileX = this.movement.getMaxX() / this.tileSizeX;
                int maxTileY = this.movement.getMaxY() / this.tileSizeY;
                int nrofAreas = (maxTileX / this.areaTileSizeX) * (maxTileY / this.areaTileSizeY);
                    
                for (int areaNumber = 0; areaNumber < nrofAreas; areaNumber++) {
                    TileMap areaTileMap = new TileMap();
                    areaTileMap.init(this.areaTileSizeX,this.areaTileSizeY);

                    int areaTileX = (int)(this.areaTileMap.getCoord(areaNumber).getX()) * this.areaTileSizeX;
                    int areaTileY = (int)(this.areaTileMap.getCoord(areaNumber).getY()) * this.areaTileSizeY;

                    for (int y = 0; y < this.areaTileSizeY; y++) {
                        for (int x = 0; x < this.areaTileSizeX; x++) {
                            int tileX = x + areaTileX;
                            int tileY = y + areaTileY;
                            int flag = this.tileMap.get(tileX,tileY);
                            areaTileMap.put(flag,x,y);
                        }
                    }
                        
                    this.areaTileMapList.put(areaNumber,areaTileMap);
                }

                //flyDrone();
                //this.droneHost.setApHost(this);
                //this.isFlying = true;
            }
        }
        /*if (this.gid.equals("AP")) {
            System.out.println("DTNHost510 : " + "arrivalCM  = " + this.arrivalCMList);
            System.out.println("DTNHost510 : " + "checked = " + this.checkedCMaddressList);
        }*/
		
	/*if (this.gid.equals("AP")) {
	    System.out.println("DTNHost507:" + this.arrivalCMList);
	    }else if (this.gid.equals("D")) {
	    System.out.println("DTNHost509:" + this.selectedTileList);
	    System.out.println("DroneState = " + this.state + "   " + this.nextTile);
	    }*/

        this.router.update();
    }
        
    /** 
     * Tears down all connections for this host.
     */
    private void tearDownAllConnections() {
        for (NetworkInterface i : net) {
            // Get all connections for the interface
            List<Connection> conns = i.getConnections();
            if (conns.size() == 0) continue;
                        
            // Destroy all connections
            List<NetworkInterface> removeList =
                new ArrayList<NetworkInterface>(conns.size());
            for (Connection con : conns) {
                removeList.add(con.getOtherInterface(i));
            }
            for (NetworkInterface inf : removeList) {
                i.destroyConnection(inf);
            }
        }
    }

    /**
     * Moves the node towards the next waypoint or waits if it is
     * not time to move yet
     * @param timeIncrement How long time the node moves
     */
    public void move(double timeIncrement) {                
        double possibleMovement;
        double distance;
        double dx, dy;

        if (!isMovementActive() || SimClock.getTime() < this.nextTimeToMove) {
            return; 
        }
	
        
        if (this.destination == null) {
            if (!setNextWaypoint()) {
                return;
            }
        }

        possibleMovement = timeIncrement * speed;
        distance = this.location.distance(this.destination);


        if (this.AParrival == false && !(this.gid.equals("D") || this.gid.equals("CM"))) {
            int x = (int)(APlocation.getX() * this.tileSizeX) + (this.tileSizeX / 2);
            int y = (int)(APlocation.getY() * this.tileSizeY) + (this.tileSizeY / 2);
            Coord APLocation = new Coord(x,y);
            double APdistance = this.location.distance(APLocation);
	    
            if (APdistance < 100) {
		evacuNum++;
                System.out.println(SimClock.getTime() + " " + this.name + " " + evacuNum + " " + apHost.checkedCMaddressList.size());
                this.AParrival = true;
            }
        }

        /*if (nextTile == 1 && this.evacuationRouteList.size () > 0 && this.AParrival == false && !(this.gid.equals("D") || this.gid.equals("CM"))) {
            System.out.println(SimClock.getTime()/60 + " " + this.name);
            this.AParrival = true;
	    }*/
                
        while (possibleMovement >= distance) {
            // node can move past its next destination
            this.location.setLocation(this.destination); // snap to destination
            possibleMovement -= distance;
            if (!setNextWaypoint()) { // get a new waypoint
                return; // no more waypoints left
            }
            distance = this.location.distance(this.destination);

            if (distance == 0) {
                if (this.gid.equals("P") && this.AParrival == false){
                    this.evacuationRouteList.clear();
                    path = null;
                    boolean set = setNextWaypoint(); //new destination set
                }
                return;
            }
        }
        
        // move towards the point for possibleMovement amount
        dx = (possibleMovement/distance) * (this.destination.getX() -
                                            this.location.getX());
        dy = (possibleMovement/distance) * (this.destination.getY() -
                                            this.location.getY());
        this.location.translate(dx, dy);
        int tileX = (int)this.location.getX() / this.tileSizeX;
        int tileY = (int)this.location.getY() / this.tileSizeY;
        int maxTileX = (this.movement.getMaxX() / this.tileSizeX) - 1;
        int maxTileY = (this.movement.getMaxY() / this.tileSizeY) - 1;
        if (tileX > maxTileX) {
            tileX -= 1;
        }
        if (tileY > maxTileY) {
            tileY -= 1;
        }
        Coord currentTile = new Coord(tileX,tileY);
        this.currentTileCoord = currentTile;

        if(!this.gid.equals("D")) {
            this.locationList.add(this.locationList.size(), currentTile);
            this.locationList = this.locationList.stream().distinct().collect(Collectors.toList());
        }

	
    }       

    /**
     * Sets the next destination and speed to correspond the next waypoint
     * on the path.
     * @return True if there was a next waypoint to set, false if node still
     * should wait
     */
    private boolean setNextWaypoint() {
        if (path == null) {
            path = movement.getPath();
        }

        if (path == null || !path.hasNext()) {
            this.nextTimeToMove = movement.nextPathAvailable();
            this.path = null;
            return false;
        }

        if (this.evacuationMode.equals("Proposed") && this.evacuationRouteList.size () > 0 && this.gid.equals("P")) {
            if (!this.AParrival) {
                int tileX = (int)this.selectedTileList.get(this.nextTile).getX();
                int tileY = (int)this.selectedTileList.get(this.nextTile).getY();
                int maxTileX = (this.movement.getMaxX() / this.tileSizeX) - 1;
                int maxTileY = (this.movement.getMaxY() / this.tileSizeY) - 1;
                if (tileX > maxTileX) {
                    tileX -= 1;
                }
                if (tileY > maxTileY) {
                    tileY -= 1;
                }
                int x = (tileX * this.tileSizeX) + (tileSizeX / 2);
                int y = (tileY * this.tileSizeY) + (tileSizeY / 2);
	    
                Coord currentLocation = new Coord(x,y);
                this.destination = currentLocation;

                if (this.nextTile > 0) {
                    selectedTileList.remove(this.nextTile);
                    this.nextTile -= 1;
                }
            } else {
                this.destination = this.location;
		
            }

            
        } else if (this.evacuationMode.equals("Proposed") && this.gid.equals("D")) {

            if (this.selectedTileList.size() > 0) {
                int tileX = (int)this.selectedTileList.get(this.nextTile).getX();
                int tileY = (int)this.selectedTileList.get(this.nextTile).getY();
                int maxTileX = (this.movement.getMaxX() / this.tileSizeX) - 1;
                int maxTileY = (this.movement.getMaxY() / this.tileSizeY) - 1;
                if (tileX > maxTileX) {
                    tileX -= 1;
                }
                if (tileY > maxTileY) {
                    tileY -= 1;
                }
                int x = (tileX * this.tileSizeX) + (this.tileSizeX / 2);
                int y = (tileY * this.tileSizeY) + (this.tileSizeY / 2);
                Coord currentLocation = new Coord(x,y);
                this.destination = currentLocation;
		
                if (this.state == State.go) {
                    List<Coord> routeList = new ArrayList<Coord>(this.evacuationRouteList.get(this.address));
                    if (nextTile > 1) {
                        routeList.add(this.selectedTileList.get(this.nextTile-1));
                        this.evacuationRouteList.put(this.address,routeList);
                    }

                    this.nextTile += 1;

                    if (this.nextTile == (this.selectedTileList.size() - 1)) {
                        int currentLocationX = (int)this.location.getX();
                        int currentLocationY = (int)this.location.getY();
                        int destinationX = (int)this.destination.getX();
                        int destinationY = (int)this.destination.getY();
                    
                        //if (this.nextTile == this.selectedTileList.size() && currentLocationX == destinationX && currentLocationY == destinationY) {
                        this.state = State.stay;
					
                        //this.apHost.setFlag();
                        //this.apHost.flyDrone();
                        //}
                    }
                    
                } else if (this.state == State.back) {
                    int currentLocationX = (int)this.location.getX();
                    int currentLocationY = (int)this.location.getY();
                    int destinationX = (int)this.destination.getX();
                    int destinationY = (int)this.destination.getY(); 
		    
                    
                            
                    if (this.nextTile == 0 /*&& currentLocationX == destinationX && currentLocationY == destinationY*/) {
                        this.state = State.stay;
                                
                        //int nrofAreas = this.apHost.getAreaTileMapList().size();
                        //if (nrofAreas > 0) {
                        //this.apHost.setFlag();
                        //this.apHost.flyDrone();
                        //}
                    }

		    if (this.nextTile > 0) {
                        this.nextTile -= 1;
                    }

                    List<Coord> routeList = new ArrayList<Coord>(this.evacuationRouteList.get(this.address));
                    if (this.initialNumber > this.nextTile && this.nextTile >= 0 && routeList.size() > 1) {
                        routeList.remove(this.nextTile+1);
                        this.evacuationRouteList.put(this.address,routeList);
                    }
                                
                    
                }
            } else {
                this.destination = this.location;
            }
        } else if (!this.AParrival) {
            this.destination = path.getNextWaypoint();
            int tileX = (int)this.destination.getX() / this.tileSizeX;
            int tileY = (int)this.destination.getY() / this.tileSizeY;
            int maxTileX = (this.movement.getMaxX() / this.tileSizeX) - 1;
            int maxTileY = (this.movement.getMaxY() / this.tileSizeY) - 1;
            if (tileX > maxTileX) {
                tileX -= 1;
            }
            if (tileY > maxTileY) {
                tileY -= 1;
            }
            int x = (tileX * this.tileSizeX) + (tileSizeX / 2);
            int y = (tileY * this.tileSizeY) + (tileSizeY / 2);
            Coord currentLocation = new Coord(x,y);
            this.destination = currentLocation;

            if (this.evacuationMode.equals("Traditional") && this.AParrival == true) {
                this.destination = this.location;
            }
        } else {
            this.destination = this.location;
        }
        this.speed = path.getSpeed();

        if (this.movListeners != null) {
            for (MovementListener l : this.movListeners) {
                l.newDestination(this, this.destination, this.speed);
            }
        }

        return true;
    }

    /**
     * Sends a message from this host to another host
     * @param id Identifier of the message
     * @param to Host the message should be sent to
     */
    public void sendMessage(String id, DTNHost to) {
        this.router.sendMessage(id, to);
    }

    /**
     * Start receiving a message from another host
     * @param m The message
     * @param from Who the message is from
     * @return The value returned by 
     * {@link MessageRouter#receiveMessage(Message, DTNHost)}
     */
    public int receiveMessage(Message m, DTNHost from) {
        int retVal = this.router.receiveMessage(m, from); 
        Map<RouteEntry,List<Coord>> routeList = m.getRouteList ();

        if (this.evacuationMode.equals("Proposed")) {
            if (this.gid.equals("P")) {
                //-----------------------new Process-------------------------//
                // make P.arrivalCMList  : List of CM found be P
                if (from.gid.equals("CM") && !this.arrivalCMList.containsKey(from.getAddress())) {
                    this.arrivalCMList.put(from.getAddress(),from.currentTileCoord);
                }

                if (from.gid.equals("P") && from.arrivalCMList.size() > 0) {
                    for (Map.Entry<Integer,Coord> entry : from.arrivalCMList.entrySet()) {
                        if (!this.arrivalCMList.containsKey(entry.getKey())) {
                            this.arrivalCMList.put(entry.getKey(),entry.getValue());
                        }
                    }
                }

                if (m.getEvacuationRouteList().size() > 0 && !from.gid.equals("D")) {
                    
                                                            
                    int tileX = (int)this.location.getX()/this.tileSizeX;
                    int tileY = (int)this.location.getY()/this.tileSizeY;
                    int maxTileX = (this.movement.getMaxX() / this.tileSizeX) - 1;
                    int maxTileY = (this.movement.getMaxY() / this.tileSizeY) - 1;
                    if (tileX > maxTileX) {
                        tileX -= 1;
                    }
                    if (tileY > maxTileY) {
                        tileY -= 1;
                    }
                    Coord currentTile = new Coord(tileX,tileY);
                    List<Coord> tileList = new ArrayList<Coord>(m.getEvacuationRouteList().get(from.getAddress()));
                    tileList.add(currentTile);
                    tileList = tileList.stream().distinct().collect(Collectors.toList());

                    int nrofTiles = this.nextTile + 1;
                    Boolean selectFlag = false;
                        
                    if (this.evacuationRouteList.size() == 0 || this.evacuationRouteList.containsKey(from.getAddress()) != true || (this.evacuationRouteList.size() > 0 && nrofTiles > tileList.size ())) {
                        this.evacuationRouteList.put(from.getAddress(),tileList);
                        selectFlag = true;
                    }

                    if (selectFlag == true) {
                        List<Coord> selectedTileList = new ArrayList<Coord>();
                        int selectedHostAddress = 0;
                        nrofTiles = 0;

                        for (Map.Entry<Integer,List<Coord>> entry : this.evacuationRouteList.entrySet()) {
                            tileList = new ArrayList<Coord>(entry.getValue());
                            
                            if (nrofTiles == 0 || nrofTiles > tileList.size()) {
                                nrofTiles = tileList.size();
                                selectedTileList = new ArrayList<Coord>(tileList);
                                selectedHostAddress = entry.getKey();
                            }
                        }
                            
                        if (this.nrofTiles == 0 || this.nrofTiles > selectedTileList.size()) {
                            this.nextTile = selectedTileList.size() - 1;
                            this.nrofTiles = selectedTileList.size();
                            this.selectedTileList = new ArrayList<Coord>(selectedTileList);
                            this.selectedHostAddress = selectedHostAddress;

                            Path path = new Path();
                            for (Coord coord : this.selectedTileList) {
                                int x = (int)(coord.getX() * this.tileSizeX) + (this.tileSizeX / 2);
                                int y = (int)(coord.getY() * this.tileSizeY) + (this.tileSizeY / 2);
                                coord = new Coord(x,y);
                                path.addWaypoint(coord);
                            }

                            this.playField.addPath(path);
                        }
                    }
                }
            } else if (this.gid.equals("AP")) {
                //if (m.getEvacuationRouteList().size() > 0) {
                if (from.gid.equals("P")) {
                    
                    /*------------------new format (P -> AP) ---------------------*/
                    //  make AP.arrivalCMList : List of CM whose location is known
                    if (from.arrivalCMList.size() > 0) {
                        for (Map.Entry<Integer,Coord> entry : from.arrivalCMList.entrySet()) {
                            if (!this.arrivalCMList.containsKey(entry.getKey()) && !this.checkedCMaddressList.contains(entry.getKey())) {
                                if (!entry.getValue().equals(this.currentTileCoord)) {
                                    this.arrivalCMList.put(entry.getKey(),entry.getValue());
                                }
                            }
                        }
                    }
                    
                    /*-------------------old format--------------------------------*/ 
                    for (Map.Entry<Integer,List<Coord>> entry : m.getEvacuationRouteList().entrySet()) {
                        List<Coord> tileList = new ArrayList<Coord>(entry.getValue());

                        for (Coord coord : tileList) {
                            int tileX = (int)coord.getX();
                            int tileY = (int)coord.getY();
                            this.tileMap.put(tileX,tileY);
                        }
                    }
                } else if (from.gid.equals("D")){
                    //System.out.println("DTNHost887 : " + from.state);
                    if(from.state == State.stay) {
                        /*------------------new format (D -> AP) ---------------------*/
                        /*-------6-------*/
                        
                        if (from.checkedCMaddressList.contains(this.waitCMaddress) && from.waitCMaddress == -1) {
                            this.checkedCMaddressList.add(this.waitCMaddress);
                            this.arrivalCMList.remove(this.waitCMaddress);
                        }
                        from.state = State.stop;
                    }
                } else if (from.gid.equals("CM")) {
                    if (!this.checkedCMaddressList.contains(from.getAddress())) {
                        this.checkedCMaddressList.add(from.getAddress());
                        if (this.arrivalCMList.containsKey(from.getAddress())){
                            this.arrivalCMList.remove(from.getAddress());
                        }
                    }
                }
                //System.out.println("DTNHost886 :  " + this.arrivalCMList);
                // }
            } else if (this.gid.equals("CM")) {
                /*------------------new format (D -> CM) ---------------------*/
                /*------3-------*/
               
                if (from.gid.equals("D") && from.state == State.stay) {
                    if(from.waitCMaddress == this.getAddress() && !this.evacuationRouteList.containsKey(from.getAddress())) {
                        this.evacuationRouteList.put(this.getAddress(),m.getEvacuationRouteList().get(from.getAddress()));
                        from.state = State.stop;
                    }
                    
                } else if (from.gid.equals("P")) {

                    /*-------------------old format--------------------------------*/
                    /*
                      if (from.locationList.size() > 0) {
                      double distance;
                      double minDistance = 9999;
                      int locationSize = this.locationList.size();
                      Coord shortestLocation = new Coord(0,0);
                    
                      this.locationList.addAll(from.locationList);
                      this.locationList = this.locationList.stream().distinct().collect(Collectors.toList());
                      if (this.locationList.size() != locationSize) {
                      if (this.APlocation != null && this.locationList.size() != locationSize) {
                      for (Coord loc : this.locationList){
                      distance = distance(loc,APlocation);
                      if (minDistance > distance) {
                      shortestLocation = loc;
                      minDistance = distance;
                      }
                      }
                      this.evacuationRouteList.clear();
                      this.evacuationRouteList.put(this.getAddress(),createRoute(shortestLocation,this.locationList.get(0)));
                      this.evacuationUpdateCount++;
                      }
                      }
                      }*/
                }
            } else if (this.gid.equals("D")) {
                //System.out.println("DTNHost937 : " + from + "    " + this.state);
                if (from.gid.equals("CM") && this.state == State.stop) {
                    /*------4------*/
                    if (this.waitCMaddress == from.getAddress() && !this.checkedCMaddressList.contains(from.getAddress())) {
                        this.checkedCMaddressList.add(this.waitCMaddress);
                        this.waitCMaddress = -1;
                        this.state = State.back;
                    }
                    
                } else if (from.gid.equals("AP")) {
                    /*----1-----*/
                    if (this.apHost == null) {
                        this.apHost = from;
                    }
                    
                    //System.out.println("   ahuahu   " + from.arrivalCMList + "    " + this.state);
                    if (from.arrivalCMList.size() > 0 && this.state == State.stop && this.waitCMaddress == -1) {
					    
                        int nearCM = -1;
                        for (Map.Entry<Integer,Coord> entry : from.arrivalCMList.entrySet()) {
                            if (!from.checkedCMaddressList.contains(entry.getKey())) {
                                nearCM = entry.getKey();
                            }
                        }
                        if (nearCM != -1) {
                            this.waitCMaddress = nearCM;
                            from.waitCMaddress = nearCM;
                            Coord nextCMLocation = new Coord(from.arrivalCMList.get(nearCM).getX(),from.arrivalCMList.get(nearCM).getY());
                            this.createTileList(nextCMLocation);
                            this.isFlying = true;
                        }
                    }
                }
            } 

        } else {
            /*------------evacuationMode != Proposed -----------------*/
            DTNHost fromHost = m.getFrom();
            DTNHost toHost = m.getTo();
            int nrofHops = m.getHopCount();
            if (fromHost.getGroupId().equals("AP") && this.name.equals(toHost.toString()) && nrofHops == 0 && this.AParrival == false) {
                System.out.println(SimClock.getTime()/60 + " " + this.name);
                this.AParrival = true;
	    


                Path path = new Path();
                double currentLocationX = fromHost.getLocation().getX();
                double currentLocationY = fromHost.getLocation().getY();
                Coord currentLocation = new Coord(currentLocationX,currentLocationY);
                path.addWaypoint(currentLocation);

                double destinationX = toHost.getLocation().getX();
                double destinationY = toHost.getLocation().getY();
                Coord destination = new Coord(destinationX,destinationY);
                path.addWaypoint(destination);

                this.playField.addPath(path);
            }
        }
                
        /*for (Map.Entry<RouteEntry,List<Coord>> entry : routeList.entrySet()) {
          RouteEntry routeEntry = entry.getKey();
          List<Coord> locationList = new ArrayList<Coord>(entry.getValue());
          this.routeList.put(routeEntry,locationList);
          }*/
                
        if (retVal == MessageRouter.RCV_OK) {
            m.addNodeOnPath(this);      // add this node on the messages path
        }

        return retVal;
    }

    public boolean duplicateCoordCheck(List<Coord> A, List<Coord> B) {
        int sizeA = A.size();
        int sizeB = B.size();

        for (int i = 0 ; i < sizeA ; i ++) {
            for (int j = 0 ; j < sizeB ; j++) {
                if (A.get(i).equals(B.get(j))) {
                    return true;
                }
            }
        }
        return false;
    }


    /**
     * Requests for deliverable message from this host to be sent trough a
     * connection.
     * @param con The connection to send the messages trough
     * @return True if this host started a transfer, false if not
     */
    public boolean requestDeliverableMessages(Connection con) {
        return this.router.requestDeliverableMessages(con);
    }

    /**
     * Informs the host that a message was successfully transferred.
     * @param id Identifier of the message
     * @param from From who the message was from
     */
    public void messageTransferred(String id, DTNHost from) {
        this.router.messageTransferred(id, from);
    }

    /**
     * Informs the host that a message transfer was aborted.
     * @param id Identifier of the message
     * @param from From who the message was from
     * @param bytesRemaining Nrof bytes that were left before the transfer
     * would have been ready; or -1 if the number of bytes is not known
     */
    public void messageAborted(String id, DTNHost from, int bytesRemaining) {
        this.router.messageAborted(id, from, bytesRemaining);
    }

    /**
     * Creates a new message to this host's router
     * @param m The message to create
     */
    public void createNewMessage(Message m) {
        if (this.evacuationMode.equals("Proposed")) {
            if (isGid(this.gid) == true || this.gid.equals("D") || this.gid.equals("CM")) {
                m.setEvacuationRouteList(this.evacuationRouteList);
            }
            else {
                Map<Integer,List<Coord>> evacuationRouteList = new HashMap<Integer,List<Coord>>();
                int to = m.getTo().getAddress();

                if (this.selectedTileList.size() > 0 && to != this.selectedHostAddress) {
                    List<Coord> tileList = new ArrayList<Coord>(this.selectedTileList);
                    evacuationRouteList.put(this.address,tileList);
                }

                m.setEvacuationRouteList(evacuationRouteList);
            }
        }
            
        m.setRouteList(this.routeList);
        m.setlocationList(this.locationList);
        this.router.createNewMessage(m);
        
    }

    /**
     * Deletes a message from this host
     * @param id Identifier of the message
     * @param drop True if the message is deleted because of "dropping"
     * (e.g. buffer is full) or false if it was deleted for some other reason
     * (e.g. the message got delivered to final destination). This effects the
     * way the removing is reported to the message listeners.
     */
    public void deleteMessage(String id, boolean drop) {
        this.router.deleteMessage(id, drop);
    }

    /**
     * Returns a string presentation of the host.
     * @return Host's name
     */
    public String toString() {
        return name;
    }

    /**
     * Checks if a host is the same as this host by comparing the object
     * reference
     * @param otherHost The other host
     * @return True if the hosts objects are the same object
     */
    public boolean equals(DTNHost otherHost) {
        return this == otherHost;
    }

    /**
     * Compares two DTNHosts by their addresses.
     * @see Comparable#compareTo(Object)
     */
    public int compareTo(DTNHost h) {
        return this.getAddress() - h.getAddress();
    }
    public boolean isGid(String gid) {
        if (gid.equals("AP")) {
            return true;
        }
        return false;
    }
    public Map<Integer,TileMap> selectArea(Map<Integer,TileMap> tileMapList) {
        boolean selectedFlag = false;
        int selectedArea = 0;
        int min = 0;

        for (Map.Entry<Integer,TileMap> entry : tileMapList.entrySet()) {
            int areaNumber = entry.getKey();
            TileMap areaTileMap = entry.getValue();
            int templateTileSizeX = this.templateTileMap.getMaxX();
            int templateTileSizeY = this.templateTileMap.getMaxY();
            int areaTileX = (int)(this.areaTileMap.getCoord(areaNumber).getX()) * this.areaTileSizeX;
            int areaTileY = (int)(this.areaTileMap.getCoord(areaNumber).getY()) * this.areaTileSizeY;
            int maxX = templateTileSizeX;
            int maxY = templateTileSizeY;
            int dx = 0;
            int sum = 0;
            int id = 0;

            TreeMap SADList = new TreeMap<Integer,List<TileMap>>();
            for (int dy = 0; dy < areaTileMap.getMaxY(); dy += templateTileSizeY, maxY += templateTileSizeY) {
                for (int i = 0; i < (areaTileMap.getMaxX() / templateTileSizeX); i++) {
                    TileMap smallAreaTileMap = new TileMap();
                    smallAreaTileMap.init(templateTileSizeX,templateTileSizeY);
                    smallAreaTileMap.setId(id);
                    sum += calcSAD(areaTileMap,smallAreaTileMap,areaTileX,areaTileY,dx,dy,maxX,maxY);
                    id++;

                    int SAD = smallAreaTileMap.getSAD();
                    List<TileMap> smallAreaTileMapList = new ArrayList<TileMap>();
                    if (SADList.containsKey(SAD)) {
                        smallAreaTileMapList = new ArrayList((List<TileMap>)SADList.get(SAD));
                        smallAreaTileMapList.add(smallAreaTileMap);
                    } else {
                        smallAreaTileMapList.add(smallAreaTileMap);
                    }
                    SADList.put(SAD,smallAreaTileMapList);

                    if (i != (areaTileMap.getMaxX() / templateTileSizeX) - 1) {
                        int cnt = dy / templateTileSizeY;
                        if (cnt == 0 || (cnt % 2) == 0) {
                            dx += templateTileSizeX;
                            maxX += templateTileSizeX;
                        } else {
                            dx -= templateTileSizeX;
                            maxX -= templateTileSizeX;
                        }
                    }
                }
            }

            List<Coord> areaRouteList = new ArrayList<Coord>();
            Iterator<Integer> itr = SADList.keySet().iterator();
            while (itr.hasNext()) {
                Integer key = itr.next();
                    
                for (TileMap smallAreaTileMap : (List<TileMap>)SADList.get(key)) {
                    List<Coord> routeList = new ArrayList<Coord>(smallAreaTileMap.getRouteList());
                    areaRouteList.addAll(routeList);
                }
            }
                
            areaTileMap.setRouteList(areaRouteList);
            tileMapList.put(areaNumber,areaTileMap);

            if (selectedFlag == false || min > sum) {
                selectedArea = areaNumber;
                min = sum;
                selectedFlag = true;
            }
        }

        TileMap selectedAreaTileMap = tileMapList.get(selectedArea);
        this.selectedTileList = new ArrayList<Coord>(selectedAreaTileMap.getRouteList());
        tileMapList.remove(selectedArea);
            
        return tileMapList;
    }

    public int calcSAD(TileMap areaTileMap, TileMap smallAreaTileMap, int areaTileX, int areaTileY, int dx, int dy, int maxX, int maxY) {
        int sum = 0;
        int x = 0;

        for (int y = 0; dy < maxY; dy++, y++) {
            for (int i = 0; i < this.templateTileSizeX; i++) {
                int abusoluteDifference =  (int)Math.abs(areaTileMap.get(dx,dy) - this.templateTileMap.get(x,y));
                int flag = areaTileMap.get(dx,dy);
                smallAreaTileMap.put(flag,areaTileX,areaTileY,x,y,dx,dy);
                sum += abusoluteDifference;

                if (i != (this.templateTileSizeX - 1)) {
                    if (y == 0 || (y % 2) == 0) {
                        dx += 1;
                        x += 1;
                    } else {
                        dx -= 1;
                        x -= 1;
                    }
                }
            }
        }

        smallAreaTileMap.setSAD(sum);

        return sum;
    }
        
    public void createTileList() {
            
        List<Coord> tileList = new ArrayList<Coord>();
        int currentTileX = (int)(this.location.getX() - (this.tileSizeX / 2)) / this.tileSizeX;
        int currentTileY = (int)(this.location.getY() - (this.tileSizeY / 2)) / this.tileSizeY;
        Coord currentCoord = new Coord(currentTileX, currentTileY);
        /*List<Coord> CMCoordsList = new ArrayList<Coord>();
          for (int i = 0 ; i < CMCoords.size() ; i++) {
          int CMX = (int)(CMCoords.get(i).getX() - (this.tileSizeX / 2)) / this.tileSizeX;
          int CMY = (int)(CMCoords.get(i).getY() - (this.tileSizeY / 2)) / this.tileSizeY;
          Coord CMloc = new Coord(CMX, CMY);
          CMCoordsList.add(CMloc);
          }
          tileList.addAll(createRoute(currentCoord, CMCoordsList.get(0)));
          this.initialNumber = tileList.size();
              
          for (int j = 0 ; j < CMCoords.size() - 2 ; j++) {
          tileList.addAll(createRoute(CMCoordsList.get(j), CMCoordsList.get(j + 1)));
          }

          tileList.addAll(createRoute(CMCoordsList.get(CMCoords.size()-1), currentCoord));
          tileList.add(currentCoord);*/

        //tileList.addAll(createRoute([this.destinationCMCoord],currentCoord));
              
        this.selectedTileList = new ArrayList<Coord>(tileList);
        this.nextTile = 1;
        this.state = State.go;

        tileList = new ArrayList<Coord>();
        currentTileX = (int)(this.location.getX() - (this.tileSizeX / 2)) / this.tileSizeX;
        currentTileY = (int)(this.location.getY() - (this.tileSizeY / 2)) / this.tileSizeY;
        Coord coord = new Coord(currentTileX,currentTileY);
        tileList.add(coord);
        this.evacuationRouteList.put(this.address,tileList);
    }

    public void createTileList(Coord nextCMLocation) {
        List<Coord> tileList = new ArrayList<Coord>();
        int currentTileX = (int)(this.location.getX() - (this.tileSizeX / 2)) / this.tileSizeX;
        int currentTileY = (int)(this.location.getY() - (this.tileSizeY / 2)) / this.tileSizeY;
        Coord currentCoord = new Coord(currentTileX, currentTileY);

        tileList.addAll(createRoute(currentCoord,nextCMLocation));

        this.selectedTileList = new ArrayList<Coord>(tileList);
        this.nextTile = 0;
        this.state = State.go;

        tileList = new ArrayList<Coord>();
        currentTileX = (int)(this.location.getX() - (this.tileSizeX / 2)) / this.tileSizeX;
        currentTileY = (int)(this.location.getY() - (this.tileSizeY / 2)) / this.tileSizeY;
        Coord coord = new Coord(currentTileX,currentTileY);
        tileList.add(coord);
        this.evacuationRouteList.put(this.address,tileList);
    }

        
    public List<Coord> createRoute(Coord currentloc, Coord destinationloc) {
        List<Coord> pathTileList = new ArrayList<Coord>();
        int destinationTileX = (int)destinationloc.getX();
        int destinationTileY = (int)destinationloc.getY();
        int currentTileX = (int)currentloc.getX();
        int currentTileY = (int)currentloc.getY();
        int dx = destinationTileX - currentTileX;
        int dy = destinationTileY - currentTileY;

        Coord coord = new Coord(currentTileX,currentTileY);
        pathTileList.add(coord);
	pathTileList.add(coord);

        while (!(currentTileX == destinationTileX && currentTileY == destinationTileY)) {
            if (currentTileX != destinationTileX) {
                currentTileX += (dx / Math.abs(dx));
            }

            if (currentTileY != destinationTileY) {
                currentTileY += (dy / Math.abs(dy));
            }

            if (currentTileX == destinationTileX && currentTileY == destinationTileY) {
                continue;
            } else {
                coord = new Coord(currentTileX,currentTileY);
                pathTileList.add(coord);
            }
        }
        pathTileList.add(destinationloc);
	pathTileList.add(destinationloc);
	
        return pathTileList;
    }
        
        
    public void flyDrone() {
        //this.areaTileMapList = new HashMap<Integer,TileMap>(selectArea(this.areaTileMapList));
        this.droneHost.setTileList(this.selectedTileList);
        this.droneHost.createTileList();
    }
        
    public void setFlag() {
        for (Map.Entry<Integer,TileMap> entry : this.areaTileMapList.entrySet()) {
            int areaNumber = entry.getKey();
            TileMap areaTileMap = entry.getValue();
            int areaTileX = (int)(this.areaTileMap.getCoord(areaNumber).getX()) * this.areaTileSizeX;
            int areaTileY = (int)(this.areaTileMap.getCoord(areaNumber).getY()) * this.areaTileSizeY;
                
            for (int y = 0; y < this.areaTileSizeY; y++) {
                for (int x = 0; x < this.areaTileSizeX; x++) {
                    int tileX = x + areaTileX;
                    int tileY = y + areaTileY;
                    int flag = this.tileMap.get(tileX,tileY);
                    areaTileMap.put(flag,x,y);
                }
            }
                
            this.areaTileMapList.put(areaNumber,areaTileMap);
        }
    }
    public void setSafetyFlag(boolean safetyFlag) {
        this.safetyFlag = safetyFlag;
    }
    public void setGroupId(String gid) {
        this.gid = gid;
    }
    public String getGroupId() {
        return this.gid;
    }
        
    public Map<Integer,List<Coord>> getEvacuationRouteList() {
        return this.evacuationRouteList;
    }
        
    public void setRouteEntry() {
        RouteEntry routeEntry = new RouteEntry ();
        routeEntry.setAddress(this.address);
        routeEntry.setSafetyFlag(this.safetyFlag);
        this.routeEntry = routeEntry;
        this.routeList.put(routeEntry,this.locationList);

        if (this.safetyFlag == true) {
            int tileX = (int)this.location.getX() / this.tileSizeX;
            int tileY = (int)this.location.getY() / this.tileSizeY;
            int maxTileX = (this.movement.getMaxX() / this.tileSizeX) - 1;
            int maxTileY = (this.movement.getMaxY() / this.tileSizeY) - 1;
            if (tileX > maxTileX) {
                tileX -= 1;
            }
            if (tileY > maxTileY) {
                tileY -= 1;
            }
            Coord currentTile = new Coord(tileX,tileY);
            List<Coord> tileList = new ArrayList<Coord>();
            tileList.add(currentTile);
            this.evacuationRouteList.put(this.address,tileList);
        }
    }

        
    public double distance(Coord start, Coord destination) {
		double dx = start.getX() - destination.getX();
		double dy = start.getY() - destination.getY();
		
		return Math.sqrt(dx*dx + dy*dy);
	}
        
    public int getTileSizeX() {
        return this.tileSizeX;
    }
    public int getTileSizeY() {
        return this.tileSizeY;
    }
    public DTNHost getDroneHost() {
        return this.droneHost;
    }
    public void setDroneHost(DTNHost droneHost) {
        this.droneHost = droneHost;
    }
    public DTNHost getApHost() {
        return this.apHost;
    }
    public void setApHost(DTNHost apHost) {
        this.apHost = apHost;
    }
    public List<Coord> getTileList() {
        return this.selectedTileList;
    }
    public void setTileList(List<Coord> tileList) {
        this.selectedTileList = new ArrayList<Coord>(tileList);
    }
    public Map<Integer,TileMap> getAreaTileMapList(){
        return this.areaTileMapList;
    }
    public void setAreaTileMapList(Map<Integer,TileMap> areaTileMapList){
        this.areaTileMapList = new HashMap<Integer,TileMap>(areaTileMapList);
    }
    public MovementModel getMovementModel() {
        return this.movement;
    }
    public void setPlayField(PlayField playField) {
        this.playField = playField;
    }

    public boolean getAParrival() {
	return this.AParrival;
    }

    
}
