[![Build Status](https://travis-ci.org/bazaarvoice/maven-cassandra-plugin.svg?branch=master)](https://travis-ci.org/bazaarvoice/maven-cassandra-plugin)

# Bazaarvoice Cassandra Maven Plugin

This is the Bazaarvoice's fork of the [cassandra-maven-plugin](http://www.mojohaus.org/cassandra-maven-plugin/).

## Releasing

* Make sure `gpg-agent` is running.
* Execute `mvn -B release:prepare release:perform`

For publishing the site do the following:

```
cd target/checkout
mvn verify site site:stage scm-publish:publish-scm
```
