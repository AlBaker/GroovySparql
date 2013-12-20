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
package groovy.sparql

import com.hp.hpl.jena.rdf.model.Literal
import com.hp.hpl.jena.rdf.model.Model
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;


import groovy.util.BuilderSupport;
import groovyx.gpars.GParsExecutorsPool


/**
 * RDFBuilder
 * 
 * Builder style DSL for creating RDF models
 * 
 * 
 * @author Al Baker
 *
 */
class RDFBuilder extends BuilderSupport {

    def defaultNamespace
    def ns = [:]
	def statements = []
    def model
	
	// Placeholders for keeping trace of
	// previous resource/predicates for doing
	// resource - predicate - resource triples
	def lastResource
	def lastPredicate
    
	// list of closures to run in nodeComplete on the root node
	def hooks = []
	
	// selected output format
    def format
	
	// Map of root closure types to jena formats
	def jenaFormats = [xml:"RDF/XML", xmlabbrev:"RDF/XML-ABBREV", ntriple:"N-TRIPLE", n3:"N3", turtle:"TURTLE"]
	
	// Output targets
	def writer
	def os
	def outputModel
    
    def RDFBuilder() {
       initInternal()
    }
	
	def RDFBuilder(Writer w) { 
		initInternal()
		this.writer = w
		hooks << { processModel ->
			model.write(writer, format)
		}
	}
	
	/**
	 * Constructor
	 * @param out output stream to write the RDF
	 */
	def RDFBuilder(OutputStream out) { 
		initInternal()
		os = out
		hooks << { processModel ->
			model.write(os, format)
		}
	}
	
	def RDFBuilder(Model output) { 
		initInternal()
		this.outputModel = output
		hooks << { processModel ->
			def originalList = []
			def iter = processModel.listStatements()
			while (iter.hasNext()) { 
				originalList << iter.nextStatement()
			}
			// Use the jena bulk interface to add all 
			// statements at once
			outputModel.add(originalList)
		}
	}
	
	void registerOutputHook(Closure c) { 
		hooks << c
	}
	
	def initInternal() { 
		model = ModelFactory.createDefaultModel()
	}
	
	void runOutputHooks() { 
		// This may be ridiculous overkill, but for a 0.1
		// project, let's have fun with GPars!
		// .. also, who knows how big some RDF documents may get
		// and you might want to send their contents multiple places
		if (hooks.size > 1) { 
			GParsExecutorsPool.withPool(hooks.size) {
				hooks.each { 
					it.callAsync(model)
				}
			}
		} else { 
			hooks.each { it.call(model) } 
		}
	}
	
	
	def handleNamespace(String input) {
	
		// TODO - better way for URI scheme resolution
		// for now 8 string compares will have to do
		if (input.startsWith("http:") ||
			input.startsWith("urn:") ||
			input.startsWith("https:") ||
			input.startsWith("sip:") ||
			input.startsWith("tel:") ||
			input.startsWith("file:") ||
			input.startsWith("xmpp:") ||
			input.startsWith("mailto:")) {
			return input
		}
		
		def returnNs
		ns.each { nsk, nsv -> 
			
			if (input.startsWith(nsk)) { 
			
				def newNs
				if (nsv[-1] == "#" || nsv[-1] == "/" ) { 
					newNs = nsv + (input - (nsk + ":"))
				} else if (nsv.startsWith("urn")) {
					newNs =  nsv + "#"+ (input - (nsk + ":"))
				} else {
					newNs =  nsv + "/"+ (input - (nsk + ":"))
				}
				if (!returnNs) { 
					returnNs = newNs
				}
				
			}
		} 
		
		if (returnNs) {
			return returnNs
		}
		
		if (input.startsWith("#") || input.startsWith("/")) { 
			return defaultNamespace + input
		} else { 
			return defaultNamespace + "/" + input
		}
	}
	
    def createNode(name) { 
		if (jenaFormats.containsKey(name)) {
			format = jenaFormats.get(name)
		}
		
        return model
    }
    
    def createNode(name, value) { 
        if (name == "defaultNamespace") { 
            defaultNamespace = value
        }
				
		if (name == "subject") { 
			
			def uri = handleNamespace(value)
			
			Resource r = model.createResource(uri)
			if (getCurrent() == "predicate") { 
				if (lastPredicate) { 
					def fullPredicate = handleNamespace(lastPredicate)
					lastResource.addProperty(model.createProperty(fullPredicate), r)
					lastPredicate = null
				}
			}
			
			lastResource = r
			return r
		}
		
		if (name == "predicate") { 
			lastPredicate = value
		}
    
        return name
    }
	    
    def createNode(name, Map attributes) {
		
        if (name == "namespace") {
            attributes.each { k, v -> 
				this.ns."${k}" = v 
			} 
        }
		
		if (getCurrent() instanceof Resource) { 
			predicate(attributes)
		}
	    return name
    }
    
	def predicate(attributes) { 
		def resource = getCurrent()
		
		attributes.each { prop, object ->
			
			def fullPredicate = handleNamespace(prop)
			
			if (object.class == String) {
				resource.addProperty( model.createProperty(fullPredicate), object) 
			}  else if (object.class == URI) {
				resource.addProperty(model.createProperty(fullPredicate), model.createResource(object.toString()))
			} else if (object instanceof Resource) {
				resource.addProperty(model.createProperty(fullPredicate), object)
			} else {
				resource.addProperty(model.createProperty(fullPredicate), model.createTypedLiteral(object))
			}
		}
	}
	
    def createNode(name, Map attributes, value) {
		
        return name
    }
    
    void setParent(parent, child) {

    }
    
    void nodeCompleted(parent, node) {
		if (parent == null) { 
			runOutputHooks()
		}
    }
	
	def generateLiteral(obj) {
		def lit = null
		if (obj.class == String) {
			lit = obj
		} else if (obj.class == URI) {
			lit = model.createResource(obj.toString())
		} else if (obj instanceof Resource) { 
			lit = obj
		} else if (obj instanceof Literal) {
			lit = obj
		}
		return lit;
	}
	
	void insert(List input) {

		def subject = input[0]
		
		if (subject.class == java.util.ArrayList.class) { 
			
			input.each { arr ->
				println arr
				insert(arr)
			}
			return
		}
		
		def property = input[1]
		def object = input[2]
		
		if (!subject | !property || !object) {
			return null
		}
		
		model.add(model.createStatement(
			model.createResource(subject),
			model.createProperty(property),
			generateLiteral(object)))
		
	}
	
	void leftShift(input) {

		insert(input)
		
		
	}
	
}
