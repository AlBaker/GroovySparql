/* Copyright (C) 2013 Al Baker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package groovy.sparql;

import static org.junit.Assert.*;
import groovy.sparql.Sparql;

import org.junit.Test;
import org.junit.Before;

import com.hp.hpl.jena.rdf.model.ModelFactory;

/**
 * @author Al Baker
 *
 */
class TestGroovySparql {

	def sparql
	def model
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		
		model = ModelFactory.createDefaultModel()
		def builder = new RDFBuilder(model)
		//builder.registerOutputHook { m -> println "Finished building, model size = " + m.size() }
		//[xml:"RDF/XML", xmlabbrev:"RDF/XML-ABBREV", ntriple:"N-TRIPLE", n3:"N3", turtle:"TURTLE"]
		builder.model {
			defaultNamespace "urn:test"
			namespace ns1:"urn:test1"
			subject("#joe") {
			   predicate "ns1:name":"joe"
			}
			
		}
		
	}
	
	@Test
	public void testEach() {
		sparql = new Sparql("http://dbpedia.org/sparql") 
		assertNotNull(sparql.endpoint)
		def query = """
			SELECT ?abstract 
				WHERE {  
					<http://dbpedia.org/resource/Groovy_%28programming_language%29> <http://dbpedia.org/ontology/abstract> ?abstract 
			} LIMIT 5
		"""
		
		sparql.eachRow query, { row ->
			assertTrue(row.abstract.startsWith("Groovy"))
			assertNotNull(row.abstract)
		}
	}
	
	@Test
	public void testModel() {
		sparql = new Sparql(model)
		assertNotNull(sparql.model)
		def query = """
			SELECT ?s ?p ?o
			WHERE {  
				?s ?p ?o
			} LIMIT 5
		"""
		def results = []
		sparql.eachRow query, { row ->
			assertNotNull(row.s)
			results << row
		}
		assertTrue(results.size() == 1)
	}
	
	@Test
	public void testParameterBinding() {
		
		sparql = new Sparql(model)
		assertNotNull(sparql.model)
		def query = """
			SELECT ?uri ?predicate ?name
			WHERE {  
				?uri ?predicate ?name
			} LIMIT 5
		"""
		def results = []
		sparql.eachRow query, { row ->
			assertNotNull(row.uri)
			results << row
		}
		assertTrue(results.size() == 1)
		
		
		def predicate = new URI("urn:test1#name")
		def results2
		sparql.eachRow( query, [ uri:new URI("urn:test#joe"), predicate:predicate], { row ->
			assertTrue(row.name.startsWith("joe"))
			results2 = row.name
		})
		
		assertTrue(results2.equals("joe"))
		
	}
	
	@Test
	public void testObjectBindings() {
		
		def obj = new GroovyWiki()
		def map = [groovy:obj]
		def query = """
		SELECT ?name
		WHERE {    
				?subject ?predicate ?name
		} LIMIT 5
		"""
		def name
		sparql = new Sparql(model)
		sparql.eachRow( query, map, { row ->
			assertTrue(row.name.startsWith("joe"))
			name = row.name
		})
		assertNotNull(name)
	}

	@Test
	public void testConstruct() {

		sparql = new Sparql("http://dbpedia.org/sparql")

		String dbQuery = """
            CONSTRUCT { 
                <http://dbpedia.org/resource/Groovy_%28programming_language%29> <http://dbpedia.org/ontology/abstract> ?b
            } wHERE { 
                <http://dbpedia.org/resource/Groovy_%28programming_language%29> <http://dbpedia.org/ontology/abstract> ?b
            } 
        """
		def result = sparql.construct(dbQuery)
		// DBpedia appears down
		//assertEquals(result.size(), 1)
	}
	
}
