GroovySPARQL
============

GroovySPARQL provides a simple API for Groovy developers to interact with SPARQL endpoints and RDF.  SPARQL endpoints such as DBPedia - the project that offers Wikipedia data as a structured set of semantic services - are everywhere.  One of the motivations for this project is to be able to write small Groovy programs for ETL scripts, sample programs, teaching others SPARQL, etc.

The design is straight forward - provide idiomatic Groovy APIs over Apache Jena, and where possible provide a simple and framework agnostic view of a SPARQL endpoint.  GroovySparql is based on Apache Jena, and there are hooks to use Jena specific APIs, but in general a user can use GroovySparql without knowing anything of Jena.

## Install

GroovySparql is available on [Maven Central](http://search.maven.org/#search|gav|1|g%3A%22com.github.albaker%22%20AND%20a%3A%22GroovySparql%22) under the groupId `com.github.albaker` and the artifactId `GroovySparql`.  

Just add it to your favorite build tool with `com.github.albaker:GroovySparql:0.9.0`.  This works well with the Groovy `Grab` annotation, Gradle, or other environments that can pull maven dependencies.  


## Simple example

```groovy
@Grab('com.github.albaker:GroovySparql:0.9.0')
import groovy.sparql.*
 
// SPARQL 1.0 or 1.1 endpoint
def sparql = new Sparql(endpoint:"http://localhost:1234/testdb/query", user:"user", pass:"pass")
		
def query = "SELECT ?s ?p ?o WHERE { ?s ?p ?o } LIMIT 4"
		
// sparql result variables projected into the closure delegate
sparql.each query, { 
	println "${s} : ${p} : ${o}"
}
```

SPARQL Update is also supported, so you can insert/delete data.

```groovy	
// Run an update query
def updateQuery = """
PREFIX dc: <http://purl.org/dc/elements/1.1/>
INSERT { <http://example/egbook> dc:title  "This is an example title5" } WHERE {}
"""
sparql.update(updateQuery)
```

As of GroovySparql version 0.9.0, the Jena CSV support is availalbe, for example:

```groovy
		def sparql = Sparql.fromCsvFile("src/test/resources/test.csv")
		def query = """
				PREFIX : <src/test/resources/test.csv#>
				SELECT ?x ?cityName  WHERE 
					{ ?x :city ?cityName . }
		"""
		def r = []
		def result = sparql.eachRow( query, { row ->
			
			r << row.cityName
		})
		assertEquals(["A City", "B City", "C City"], r)
```

## RDF Builder

GroovySparql does include a way to generate RDF:

```groovy
    def output = builder.turtle {
			defaultNamespace "urn:test"
			namespace ns1:"urn:test1"
			subject("#joe") {
			   predicate "ns1:name":"joe"	
			}
			
	}
```

If you inspect the `output` variable above, you'll notice that it is a Jena model.  This is interesting as you can now use it in the construction of the `Sparql` object.

```groovy

		 def builder = new RDFBuilder(model)
        //[xml:"RDF/XML", xmlabbrev:"RDF/XML-ABBREV", ntriple:"N-TRIPLE", n3:"N3", turtle:"TURTLE"]
        def output = builder.model {
            defaultNamespace "urn:test"
            namespace ns1:"urn:test1"
            subject("#joe") {
                predicate "ns1:name":"joe"
            }

        }

        sparql = new Sparql(output)

        String dbQuery = """
            ASK { <urn:test#joe> ?p ?o }
        """

        def result = sparql.ask(dbQuery)
        assertTrue(result)
```


## License

Copyright 2014 Al Baker

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

* [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)  

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.







