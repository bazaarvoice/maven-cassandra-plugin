package org.codehaus.mojo.cassandra;

import org.apache.cassandra.thrift.Cassandra;

public abstract class ThriftApiOperation {
  
  private String keyspace;
  private final String rpcAddress;
  private final int rpcPort;

  public ThriftApiOperation(String rpcAddress, int rpcPort)
  {
      this.rpcAddress = rpcAddress;
      this.rpcPort = rpcPort;
  }
  
  public abstract void executeOperation(Cassandra.Client client) throws ThriftApiExecutionException;

  public String getKeyspace()
  {
      return keyspace;
  }

  public void setKeyspace(String keyspace)
  {
      this.keyspace = keyspace;
  }

  public String getRpcAddress()
  {
      return rpcAddress;
  }


  public int getRpcPort()
  {
      return rpcPort;
  }



}
