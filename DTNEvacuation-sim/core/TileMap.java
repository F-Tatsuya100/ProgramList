/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package core;

import java.util.Arrays;


import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

public class TileMap {
    private int maxX;
    private int maxY;
    private int[][] tileMap;
    private int id;
    private int SAD;
    private List<Coord> routeList;
    private Map<Integer,Coord> coordList;

    public void init(int maxX, int maxY) {
        this.maxX = maxX;
        this.maxY = maxY;
        this.tileMap = new int[maxX][maxY];
        this.routeList = new ArrayList<Coord>();
        this.coordList = new HashMap<Integer,Coord>();
        
        createCoordList();
    }

    public void put(int x, int y) {
        this.tileMap[x][y] = 1;
    }
    
    public void put(int flag, int x, int y) {
        this.tileMap[x][y] = flag;
    }

    public void put(int flag, int areaTileSizeX, int areaTileSizeY, int x, int y, int dx, int dy) {
        this.tileMap[x][y] = flag;

        if (flag == 0) {
            x = dx + areaTileSizeX;
            y = dy + areaTileSizeY;
            Coord coord = new Coord(x,y);
            this.routeList.add(coord);
        }
    }

    public int get(int x, int y) {
        return this.tileMap[x][y];
    }

    public int getMaxX() {
        return this.maxX;
    }

    public int getMaxY() {
        return this.maxY;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return this.id;
    }

    public void setSAD(int SAD) {
        this.SAD = SAD;
    }

    public int getSAD() {
        return this.SAD;
    }

    public void setRouteList(List<Coord> routeList) {
        this.routeList = new ArrayList<Coord>(routeList);
    }

    public List<Coord> getRouteList() {
        return this.routeList;
    }

    public void createCoordList() {
        int areaNumber = 0;
        for (int y = 0; y < this.maxY; y++) {
            for (int x = 0; x < this.maxX; x++) {
                Coord coord = new Coord(x,y);
                this.coordList.put(areaNumber,coord);
                areaNumber++;
            }
        }
    }

    public Coord getCoord(int areaNumber) {
        return this.coordList.get(areaNumber);
    }
    
    public void print() {
        int max = (this.maxX * 2) - 1;
        for (int i = 0; i < max; i++) {
            System.out.print("-");
        }

        System.out.println("");
        for (int y = 0; y < this.maxY; y++) {
            for (int x = 0; x < this.maxX; x++) {
                System.out.print(this.tileMap[x][y] + " ");
            }
            System.out.println("");
        }
        
        for (int i = 0; i < max; i++) {
            System.out.print("-");
        }
        System.out.println("");
    }

    public void print(int[][] tileMap, int areaTileSizeX, int areaTileSizeY) {
        int max = (areaTileSizeX * 2) - 1;
        for (int i = 0; i < max; i++) {
            System.out.print("-");
        }

        System.out.println("");
        for (int y = 0; y < areaTileSizeY; y++) {
            for (int x = 0; x < areaTileSizeX; x++) {
                System.out.print(tileMap[x][y] + " ");
            }
            System.out.println("");
        }
        
        for (int i = 0; i < max; i++) {
            System.out.print("-");
        }
        System.out.println("");
    }
}
