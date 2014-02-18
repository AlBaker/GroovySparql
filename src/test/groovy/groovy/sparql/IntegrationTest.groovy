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

import org.junit.Before;
import org.junit.Test;

class IntegrationTest {

	@Before
	public void setUp() throws Exception {
	}

	//@Test
	public void testWithExternal() {
		def sparql = new Sparql(endpoint:"http://localhost:5822/petstore/query", user:"admin", pass:"admin")
		
		def query = "SELECT ?s ?p ?o WHERE { ?s ?p ?o } LIMIT 4"
		
		sparql.eachRow query, { row ->
			println "${row.s} : ${row.p} : ${row.o}"
		}
		
	}
	
	//@Test
	public void testEach() {
		def sparql = new Sparql(endpoint:"http://localhost:5822/petstore/query", user:"admin", pass:"admin")
		
		def query = "SELECT ?s ?p ?o WHERE { ?s ?p ?o } LIMIT 4"
		
		sparql.each query, { 
			println "${s} : ${p} : ${o}"
		}
		
	}
	
	//@Test
	public void testUpdate() {
		def sparql = new Sparql(updateEndpoint:"http://localhost:5820/league/update", user:"admin", pass:"admin")
		def updateQuery = """
PREFIX dc: <http://purl.org/dc/elements/1.1/>
INSERT { <http://example/egbook> dc:title  "This is an example title5" } WHERE {}
		"""
		sparql.update(updateQuery)
		
	}
	
}
