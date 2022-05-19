/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package core;

public class RouteEntry {
    private int address;
    private boolean safetyFlag;

    public int getAddress(){
        return this.address;
    }

    public boolean getSafetyFlag(){
        return this.safetyFlag;
    }
    
    public void setAddress(int address){
        this.address = address;
    }
    
    public void setSafetyFlag(boolean safetyFlag){
        this.safetyFlag = safetyFlag;
    }
}
