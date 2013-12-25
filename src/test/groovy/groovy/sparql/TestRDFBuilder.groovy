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
import groovy.sparql.RDFBuilder;
import com.hp.hpl.jena.rdf.model.Model
import com.hp.hpl.jena.rdf.model.ModelFactory;

import org.junit.Test;
import org.junit.Before;

import com.hp.hpl.jena.rdf.model.ModelFactory;

/**
* @author Al Baker
*
*/
class TestRDFBuilder {
	
	def sparql
	
	BufferedReader reader
	BufferedOutputStream out
	PipedInputStream pipeInput
	
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		pipeInput = new PipedInputStream();
		reader = new BufferedReader(
				new InputStreamReader(pipeInput));
		out = new BufferedOutputStream(
			new PipedOutputStream(pipeInput));
	}
	
	@Test
	public void testConstruction() {
		def builder = new RDFBuilder()
		builder.registerOutputHook { println "Finished building" }
        def output = builder.n3 { 
			defaultNamespace "urn:test"
			namespace ns1:"urn:test1"
			namespace ns2:"urn:test3"
			namespace ns3:"http://example.org"
			
            subject("#fred") { 
               predicate "ns1:name":"fred"
               predicate "ns1:last":"smith"
			   predicate "ns3:city":"New York City"
			   predicate("ns3:friend") {
				   subject("#roger") {
					   predicate "ns1:name":"roger"
					   predicate "ns1:last":"baker"
					   predicate("ns3:friend") { 
						   subject("#alex") { 
							   predicate "ns1:name":"alex"
							   predicate "ns1:last":"stevens"
						   }
					   }
				   }
			   }
                
            }
            
        }
		assertEquals(output.size(), 9 as Long)
		assertTrue(builder.defaultNamespace == "urn:test")
		assertTrue(builder.ns.containsKey("ns1"))
		assertTrue(builder.ns.containsKey("ns2"))
		assertTrue(builder.ns.containsKey("ns3"))

	}
		
	@Test
	public void testOutputStreamTurtle() { 

		
		def builder = new RDFBuilder(out)
		//[xml:"RDF/XML", xmlabbrev:"RDF/XML-ABBREV", ntriple:"N-TRIPLE", n3:"N3", turtle:"TURTLE"]
		def output = builder.turtle {
			defaultNamespace "urn:test"
			namespace ns1:"urn:test1"
			subject("#joe") {
			   predicate "ns1:name":"joe"	
			}
			
		}
		
		assertTrue(builder.model.contains(builder.model.createResource("urn:test#joe"), builder.model.createProperty("urn:test1#name")))
		
		
	}
	
	@Test
	public void testOutputStreamN3() { 
		def builder = new RDFBuilder(out)
		//[xml:"RDF/XML", xmlabbrev:"RDF/XML-ABBREV", ntriple:"N-TRIPLE", n3:"N3", turtle:"TURTLE"]
		def output = builder.n3 {
			defaultNamespace "urn:test"
			namespace ns1:"urn:test1"
			subject("#joe") {
			   predicate "ns1:name":"joe"
			}
			
		}
		assertTrue(builder.model.contains(builder.model.createResource("urn:test#joe"), builder.model.createProperty("urn:test1#name")))
		
	}
	
	@Test
	public void testOutputStreamXML() {
		def builder = new RDFBuilder(out)
		//[xml:"RDF/XML", xmlabbrev:"RDF/XML-ABBREV", ntriple:"N-TRIPLE", n3:"N3", turtle:"TURTLE"]
		def output = builder.xml {
			defaultNamespace "urn:test"
			namespace ns1:"urn:test1"
			subject("#joe") {
			   predicate "ns1:name":"joe"
			}
			
		}
		assertEquals("<rdf:RDF", reader.readLine());
	}
	
	@Test
	public void testOutputModel() {
		def model = ModelFactory.createDefaultModel()
		def builder = new RDFBuilder(model)
		builder.registerOutputHook { m -> println "Finished building, model size = " + m.size() }
		//[xml:"RDF/XML", xmlabbrev:"RDF/XML-ABBREV", ntriple:"N-TRIPLE", n3:"N3", turtle:"TURTLE"]
		def output = builder.model {
			defaultNamespace "urn:test"
			namespace ns1:"urn:test1"
			subject("#joe") {
			   predicate "ns1:name":"joe"
			}
			
		}
		assertEquals(model.toString(), """<ModelCom   {urn:test#joe @urn:test1#name "joe"} |  [urn:test#joe, urn:test1#name, "joe"]>""")
		assertTrue(builder.model.contains(builder.model.createResource("urn:test#joe"), builder.model.createProperty("urn:test1#name")))
		
	}
	
	@Test
	public void testOutputStreamPrintWriter() {
		def writer = new PrintWriter(out)
		def builder = new RDFBuilder(writer)
		//[xml:"RDF/XML", xmlabbrev:"RDF/XML-ABBREV", ntriple:"N-TRIPLE", n3:"N3", turtle:"TURTLE"]
		def output = builder.xml {
			defaultNamespace "urn:test"
			namespace ns1:"urn:test1"
			subject("#joe") {
			   predicate "ns1:name":"joe"
			}
			
		}
		assertEquals("<rdf:RDF", reader.readLine());
	}
	
	@Test
	public void testInsertMethod() {
		
		def writer = new PrintWriter(out)
		def builder = new RDFBuilder(writer)
		
		builder << ["urn:a", "urn:b", "c"]
		assertTrue(builder.model.contains(builder.model.createResource("urn:a"), builder.model.createProperty("urn:b")))
		
		builder.insert([["urn:d", "urn:e", "d"]])
		assertTrue(builder.model.contains(builder.model.createResource("urn:d"), builder.model.createProperty("urn:e")))
		
	}
	
}
