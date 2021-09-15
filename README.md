[![Build Status](https://travis-ci.org/bazaarvoice/maven-cassandra-plugin.svg?branch=master)](https://travis-ci.org/bazaarvoice/maven-cassandra-plugin)

# Bazaarvoice Cassandra Maven Plugin

This is the Bazaarvoice's fork of the [cassandra-maven-plugin](http://www.mojohaus.org/cassandra-maven-plugin/).

## Testing
```bash
  mvn -e clean verify  -Ddependency-check.skip=true -Prun-its
```

## Running
```bash
  mvn clean install

  mvn com.bazaarvoice.maven.plugins:cassandra-maven-plugin:${version}:start -Dcassandra.rpcPort=19160 -Dcassandra.jmxPort=17199 -Dcassandra.storagePort=17000 -Dcassandra.stopPort=18081
```

## Releasing

* Make sure `gpg-agent` is running.
* Execute `mvn -B release:prepare release:perform`

For publishing the site do the following:

```
cd target/checkout
mvn verify site site:stage scm-publish:publish-scm
```
