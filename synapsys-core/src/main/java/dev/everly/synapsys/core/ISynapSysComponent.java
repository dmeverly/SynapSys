package dev.everly.synapsys.core;


public interface ISynapSysComponent {
    String getID();
    String getName();
    void initialize() throws Exception;
    void shutdown() throws Exception;
}
