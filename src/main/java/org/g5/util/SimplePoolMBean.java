package org.f8s.g5.util;


public interface SimplePoolMBean {

    int getPoolSize();
    
    int getMaxPoolSize();
    
    int getLeasedInstanceCount();
    
    String getAvailableServiceConnections();
    
    void clear();
}
