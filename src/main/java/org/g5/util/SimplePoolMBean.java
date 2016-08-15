package org.g5.util;


public interface SimplePoolMBean {

    int getPoolSize();
    
    int getMaxPoolSize();
    
    int getLeasedInstanceCount();
    
    String getAvailableServiceConnections();
    
    void clear();
}
