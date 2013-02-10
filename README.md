GroovySPARQL
============

GroovySPARQL provides a simple API for Groovy developers to interact with SPARQL endpoints and RDF.  SPARQL endpoints such as DBPedia - the project that offers Wikipedia data as a structured set of semantic services - are everywhere.  One of the motiviations for this project is to be able to write small Groovy programs that @Grab the Groovy SPARQL jar, and start writing queries and iterating over results.

The design is straight forward - provide idiomatic Groovy APIs over Apache Jena, and where possible provide a simple and framework agnostic view of a SPARQL endpoint.

## Simple example

	@Grab('com.github.albaker:GroovySparql:0.6')
	import groovy.sparql.*
 
	// SPARQL 1.0 or 1.1 endpoint
        def sparql = new Sparql(endpoint:"http://localhost:1234/testdb/query", user:"user", pass:"pass")
		
	def query = "SELECT ?s ?p ?o WHERE { ?s ?p ?o } LIMIT 4"
		
	// sparql result variables projected into the closure delegate
	sparql.each query, { 
		println "${s} : ${p} : ${o}"
	}


## License

Licensed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)  






