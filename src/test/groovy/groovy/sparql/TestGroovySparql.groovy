/* Copyright (C) 2011 Al Baker
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
 * @author ajb
 *
 */
class TestGroovySparql {

	def sparql
	
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
	}
	
	@Test
	public void testConstruction() {
		sparql = new Sparql("http://dbpedia.org/sparql")
		assertNotNull(sparql.endpoint)
		def query = """
		SELECT ?abstract 
		WHERE {  
			<http://dbpedia.org/resource/Groovy_%28programming_language%29> 
			<http://dbpedia.org/ontology/abstract> 
			?abstract 
		} LIMIT 5
		"""
		
		sparql.eachRow query, { row ->
			assertTrue(row.abstract.startsWith("Groovy"))
		}
	}
	
	@Test
	public void testParameterBinding() {
		sparql = new Sparql(ModelFactory.createDefaultModel())
		assertNotNull(sparql.model)
		def query = """
		SELECT ?abstract  
		WHERE {    
			SERVICE <http://dbpedia.org/sparql> {
				?uri ?predicate ?abstract
			}
		} LIMIT 5
		"""
		def predicate = new URI("http://dbpedia.org/ontology/abstract")
		def found = false
		sparql.eachRow( query, [ uri:new URI("http://dbpedia.org/resource/Groovy_%28programming_language%29"), predicate:predicate], { row ->
			assertTrue(row.abstract.startsWith("Groovy"))
			found = true
		})
		
		assertTrue(found)
		found = false
		sparql = new Sparql("http://dbpedia.org/sparql")
		sparql.eachRow( query, [ uri:new URI("http://dbpedia.org/resource/Groovy_%28programming_language%29"), predicate:predicate], { row ->
			assertTrue(row.abstract.startsWith("Groovy"))
			found = true
		})
		assertTrue(found)
		
	}
	
	@Test
	public void testObjectBindings() {
		
		def obj = new GroovyWiki()
		def map = [groovy:obj]
		def query = """
		SELECT ?abstract
		WHERE {    
			SERVICE <http://dbpedia.org/sparql> {
				?groovySubject ?groovyPredicate ?abstract
			}
		} LIMIT 5
		"""
		def found = false
		sparql = new Sparql("http://dbpedia.org/sparql")
		sparql.eachRow( query, map, { row ->
			assertTrue(row.abstract.startsWith("Groovy"))
			found = true
		})
		assertTrue(found)
	}

}
