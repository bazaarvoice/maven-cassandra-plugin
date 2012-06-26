package org.codehaus.mojo.cassandra;

import org.apache.cassandra.config.ConfigurationException;
import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.db.marshal.TypeParser;
import org.apache.cassandra.thrift.Cassandra.Client;
import org.apache.cassandra.thrift.Column;
import org.apache.cassandra.thrift.Compression;
import org.apache.cassandra.thrift.CqlResult;
import org.apache.cassandra.thrift.CqlRow;
import org.apache.cassandra.utils.ByteBufferUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.IOUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Executes cql statements from maven.
 *
 * @author zznate
 * @goal cql-exec
 * @threadSafe
 * @phase pre-integration-test
 */
public class CqlExecCassandraMojo extends AbstractCassandraMojo {

	/** 
 	 * The CQL version that the provided cqlScript or cqlStatement should be interpreted with 
 	 * 
 	 * @parameter expression="${cql.cqlVersion}" default-value="3.0.0" 
 	 */ 
 	protected String cqlVersion;

    /**
     * The name of a keyspace which, if present, indicates to skip running the cqlStatement or cqlScript
     *
     * @parameter expression="${cql.skipIfKeyspaceIsPresent}"
     */
    protected String skipIfKeyspaceIsPresent;

    /**
     * The CQL script which will be executed
     *
     * @parameter expression="${cassandra.cql.script}" default-value="${basedir}/src/cassandra/cql/exec.cql"
     */
    protected File cqlScript;

    /**
     * The CQL statement to execute singularly
     *
     * @parameter expression="${cql.statement}"
     */
    protected String cqlStatement;

    /**
     * Expected type of the column value
     * @parameter expression="${cql.defaultValidator}"
     */
    protected String defaultValidator = "BytesType";

    /**
     * Expected type of the key
     * @parameter expression="${cql.keyValidator}"
     */
    protected String keyValidator = "BytesType";

    /**
     * Expected type of the column name
     * @parameter expression="${cql.comparator}"
     */
    protected String comparator = "BytesType";

    private AbstractType<?> comparatorVal;
    private AbstractType<?> keyValidatorVal;
    private AbstractType<?> defaultValidatorVal;

    private boolean shouldSkip()
            throws MojoExecutionException {
        // The obvious case
        if (skip) {
            return true;
        }

        if (StringUtils.isNotBlank(skipIfKeyspaceIsPresent)) {
            // A keyspace was specified.  Look to see if it exists.  If so, return true.
            final boolean[] exists = {false};
            Utils.executeThrift(new ThriftApiOperation(rpcAddress, rpcPort) {
                @Override
                void executeOperation(Client client) {
                    try {
                        client.describe_keyspace(skipIfKeyspaceIsPresent);
                        exists[0] = true;
                    } catch (Exception e) {
                        // Assume describe failed because the keyspace doesn't exist
                    }
                }
            });
            if (exists[0]) {
                return true;
            }
        }

        return false;
    }

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
      if (shouldSkip())
      {
          getLog().info("Skipping cassandra: cassandra.skip==true");
          return;
      }
      try
      {
          comparatorVal = TypeParser.parse(comparator);
          keyValidatorVal = TypeParser.parse(keyValidator);
          defaultValidatorVal = TypeParser.parse(defaultValidator);

      } catch (ConfigurationException e)
      {
          throw new MojoExecutionException("Could not parse comparator value: " + comparator, e);
      }
      if (cqlScript != null && cqlScript.isFile())
      {
          FileReader fr = null;
          try
          {
              fr = new FileReader(cqlScript);
              cqlStatement = IOUtil.toString(fr);
          } catch (FileNotFoundException e)
          {
              throw new MojoExecutionException("Cql file '" + cqlScript + "' was deleted before I could read it", e);
          } catch (IOException e)
          {
              throw new MojoExecutionException("Could not parse or load cql file", e);
          } finally
          {
              IOUtil.close(fr);
          }
      }

      if (StringUtils.isBlank(cqlStatement))
      {
          getLog().warn("No CQL provided. Nothing to do.");
      } else
      {
          // TODO accept keyFormat, columnFormat, valueFormat
          // ^ are these relevant on file load?
          List<CqlExecOperationResult> cqlOpResults = doExec(Arrays.asList(StringUtils.split(cqlStatement, ";"))); 
          
          printResults(cqlOpResults);
      }
  }
  
  /*
   * Encapsulate print of CqlResult. Uses specified configuration options to format results
   */
  private void printResults(List<CqlExecOperationResult> cqlOpResults)
  {
      // TODO fix ghetto formatting
      getLog().info("-----------------------------------------------");
      for (CqlExecOperationResult cqlExecOperationResult : cqlOpResults)
      {          
          while ( cqlExecOperationResult.hasNext() )
          {
              CqlRow cqlRow = cqlExecOperationResult.next();
              getLog().info("Row key: "+keyValidatorVal.getString(cqlRow.key));
              getLog().info("-----------------------------------------------");
              for (Column column : cqlRow.getColumns() )
              {
                  getLog().info(" name: "+comparatorVal.getString(column.name));
                  getLog().info(" value: "+defaultValidatorVal.getString(column.value));
                  getLog().info("-----------------------------------------------");
              }

          }            
      }          
  }
  
  /*
   * Encapsulate op execution for file vs. statement
   */
  private List<CqlExecOperationResult> doExec(List<String> cqlStatements) throws MojoExecutionException
  {
      CqlExecOperation cqlOp = new CqlExecOperation(rpcAddress, rpcPort, cqlStatements);
      if ( StringUtils.isNotBlank(keyspace)) {
          getLog().info("setting keyspace: " + keyspace);
          cqlOp.setKeyspace(keyspace);
      }
      try {
          Utils.executeThrift(cqlOp);
      } catch (ThriftApiExecutionException taee) {
          throw new MojoExecutionException(taee.getMessage(), taee);
      }   
      return cqlOp.getCqlExecOperationResults();
  }
  
  class CqlExecOperation extends ThriftApiOperation { 

 	  String cqlVersion; 
 	  List<String> cqlStatements; 
 	  List<CqlExecOperationResult> cqlExecOperationResults = new ArrayList<CqlExecOperationResult>(); 
 	 	
 	 	
 	  public CqlExecOperation(String rpcAddress, int rpcPort, String cqlVersion, List<String> cqlStatements) 
 	  { 
 	      super(rpcAddress, rpcPort); 
 	      this.cqlVersion = cqlVersion; 
 	      this.cqlStatements = cqlStatements; 
 	  } 
 	 	
 	  @Override 
 	  void executeOperation(Client client) throws ThriftApiExecutionException 
 	  { 
 	      try  
 	      { 
 	          if (StringUtils.isNotBlank(cqlVersion)) { 
 	              getLog().debug("Setting CQL Version: " + cqlVersion);
 	              client.set_cql_version(cqlVersion); 
 	          } 
 	  
 	          for (String cql : cqlStatements) { 
 	              if (StringUtils.isNotBlank(cql)){ 
 	                  getLog().debug("Executing CQL: " + cql); 
 	                  CqlResult result = client.execute_cql_query(ByteBufferUtil.bytes(cql), Compression.NONE); 
 	                  cqlExecOperationResults.add(new CqlExecOperationResult(result)); 
 	              } 
 	          } 
 	      } catch (Exception e)  
 	      { 
 	          throw new ThriftApiExecutionException(e); 
 	      } 
 	  } 
 	 	
 	  public List<CqlExecOperationResult> getCqlExecOperationResults() { 
 	      return cqlExecOperationResults; 
 	  } 
  } 
  
  class CqlExecOperationResult implements Iterator<CqlRow> {

      CqlResult result;
      CqlRow current;
      Iterator<CqlRow> rowIter;

      public CqlExecOperationResult(CqlResult result)
      {
          this.result = result;
          this.rowIter = result.getRowsIterator();
      }

      @Override
      public boolean hasNext()
      {
          return rowIter != null && rowIter.hasNext();
      }

      @Override
      public CqlRow next()
      {

          current = rowIter.next();
          return current;
      }

      @Override
      public void remove()
      {
          rowIter.remove();
      }
      
      List<Column> getColumns()
      {
          return current.getColumns();
      }
      
      ByteBuffer getKey() 
      {
          return current.bufferForKey();
      }

  }

}
