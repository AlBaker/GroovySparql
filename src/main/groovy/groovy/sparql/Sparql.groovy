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

package groovy.sparql

import groovy.util.logging.*

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP
import com.hp.hpl.jena.query.ARQ;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.QuerySolutionMap
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.query.Syntax;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;


/**
 * @author Al Baker
 *
 */
@Slf4j
class Sparql {

	String endpoint
	Model model
	
	def config = [:]

	public static Sparql newInstance(String url) {
		new Sparql(endpoint:url)
	}

	public static Sparql newInstance(Model model) {
		new Sparql(model:model)
	}

	Sparql(Model model) { this.model = model }

	Sparql(String endpoint) { this.endpoint = endpoint }
	
	Sparql(Model model, Map config) { 
		this.model = model
		this.config = config
	}
	
	Sparql(String endpoint, Map config) {
		this.endpoint = endpoint
		this.config = config
	}

	
	/**
	 * <code>eachRow</code>
	 * @param sparql - SPARQL query, SELECT only
	 * @param closure to be called on each result set solution, 
	 *        map of solution variables provided to the closure
	 */
	void eachRow(String sparql, Closure closure) {
		
		Query query = QueryFactory.create(sparql, Syntax.syntaxARQ)
		QueryExecution qe = null
		
		/**
		 * Some explanation here - ARQ can provide a QE based on a pure
		 * SPARQL service endpoint, or a Jena model, plus you can still
		 * do remote queries with the model using the in-SPARQL "service"
		 * keyword.
		 */
		if (model) {
			qe = QueryExecutionFactory.create(query, model);
		} else {
			if (!endpoint)
				return
			qe = QueryExecutionFactory.sparqlService(endpoint, query)
			if (config.timeout) {
				((QueryEngineHTTP)qe).addParam("timeout", config.timeout as String) 
			}
		}
		
		/**
		 * 
		   TODO: Uncomment when https://issues.apache.org/jira/browse/JENA-56 is fixed
		   
		if (config.timeout) {
			qe.setTimeout( config?.timeout?.toLong() )
		}
		
		*/
		
		try {
			for (ResultSet rs = qe.execSelect(); rs.hasNext() ; ) {
				QuerySolution sol = rs.nextSolution();
				Map<String, Object> row = new HashMap<String, Object>();
				for (Iterator<String> varNames = sol.varNames(); varNames.hasNext(); ) {
						String varName = varNames.next();
						RDFNode varNode = sol.get(varName);
						row.put(varName, (varNode.isLiteral() ? varNode.asLiteral().getValue() : varNode.toString()));
				}
				closure.call(row)
			}
		} finally {
			qe.close();
		}
	}

	
	
	
	/**
	 * <code>eachRow</code>
	 * @param sparql - SPARQL query, SELECT only
	 * @param args - MAp of parameters to use in the query
	 * @param closure to be called on each result set solution,
	 *        map of solution variables provided to the closure
	 */
	void eachRow(String sparql, Map args, Closure closure) {
		
		Query query = QueryFactory.create(sparql, Syntax.syntaxARQ)
		QueryExecution qe = null
		QuerySolutionMap initialBindings = new QuerySolutionMap();
		
		Model tmpModel
		
		if (!model) {
			tmpModel = ModelFactory.createDefaultModel();
		} else {
			tmpModel = model
		}
		
		def processArgs =  {  key, value ->	
			if (value.class == java.net.URI) {
				// this apparently works for both Resoruce and Property instances, in Jena
				// terms, however there is a difference between the object of a triple being
				// a resource OR a literal, so Strings = Literals, URIs = Resources
				initialBindings.add(key, tmpModel.createResource(value.toString()))
			} else if (value.class == java.lang.String){
				initialBindings.add(key, tmpModel.createLiteral(value.toString()))
			} else { 
				value.properties.each { propName, propValue ->
					if (propValue.class == java.lang.String || propValue.class == java.net.URI) {
						if (propName == "subject") {
							owner.call("${key}Subject".toString(), propValue)
						} else if (propName == "predicate") {
							owner.call("${key}Predicate".toString(), propValue)
						} else if (propName == "object") {
							owner.call("${key}Object".toString(), propValue)
						} 
					} 	
				}
			}
		}
		
		args.each(processArgs)
		
		/**
		 * Some explanation here - ARQ can provide a QE based on a pure
		 * SPARQL service endpoint, or a Jena model, plus you can still
		 * do remote queries with the model using the in-SPARQL "service"
		 * keyword.
		 */
		if (model) {
			qe = QueryExecutionFactory.create(query, model, initialBindings);
		} else {
			if (!endpoint)
				return
			qe = QueryExecutionFactory.create(query, tmpModel, initialBindings)
		}
				
		try {
			for (ResultSet rs = qe.execSelect(); rs.hasNext() ; ) {
				QuerySolution sol = rs.nextSolution();
				Map<String, Object> row = new HashMap<String, Object>();
				for (Iterator<String> varNames = sol.varNames(); varNames.hasNext(); ) {
						String varName = varNames.next();
						RDFNode varNode = sol.get(varName);
						row.put(varName, (varNode.isLiteral() ? varNode.asLiteral().getValue() : varNode.toString()));
				}
				closure.call(row)
			}
		} finally {
			qe.close();
		}
	}
	
	
	/**
	* <code>execConstruct</code>
	*
	* Template method for executing a CONSTRUCT statement
	* No mapper is used as the mapping is performed in the SPARQL
	*
	* @param sparql
	* @return new Model
	*/
	Model construct(String sparql) {
		Model m = null
		QueryExecution qe = null
		try {
			qe = QueryExecutionFactory.create(QueryFactory.create(sparql), model);
			m = qe.execConstruct();
		} catch (Exception e) {
			log.error "Error executing construct with ${sparql}"
		} finally {
			qe.close();
		}
		return m;
   }


}
