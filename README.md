GroovySPARQL
============

GroovySPARQL provides a simple API for Groovy developers to interact with SPARQL endpoints and RDF.  SPARQL endpoints such as DBPedia - the project that offers Wikipedia data as a structured set of semantic services - are everywhere.  One of the motivations for this project is to be able to write small Groovy programs that use Grapes and grab the Groovy SPARQL jar, and start writing queries and iterating over results.

The design is straight forward - provide idiomatic Groovy APIs over Apache Jena, and where possible provide a simple and framework agnostic view of a SPARQL endpoint.  GroovySparql is based on Apache Jena, and there are hooks to use Jena specific APIs, but in general a user can use GroovySparql without knowing anything of Jena.

## Simple example

	@Grab('com.github.albaker:GroovySparql:0.7.2')
	import groovy.sparql.*
 
	// SPARQL 1.0 or 1.1 endpoint
    def sparql = new Sparql(endpoint:"http://localhost:1234/testdb/query", user:"user", pass:"pass")
		
	def query = "SELECT ?s ?p ?o WHERE { ?s ?p ?o } LIMIT 4"
		
	// sparql result variables projected into the closure delegate
	sparql.each query, { 
		println "${s} : ${p} : ${o}"
    }
	
	// Run an update query
    def updateQuery = """
    PREFIX dc: <http://purl.org/dc/elements/1.1/>
    INSERT { <http://example/egbook> dc:title  "This is an example title5" } WHERE {}
    """
    sparql.update(updateQuery)
	

## RDF Builder

GroovySparql does include a way to generate RDF:

    def output = builder.turtle {
			defaultNamespace "urn:test"
			namespace ns1:"urn:test1"
			subject("#joe") {
			   predicate "ns1:name":"joe"	
			}
			
	}


## License

Licensed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)  






