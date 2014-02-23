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

import groovy.util.logging.*


import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.client.CredentialsProvider
import org.apache.http.client.protocol.ClientContext
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.protocol.BasicHttpContext
import org.apache.http.protocol.HttpContext

import com.hp.hpl.jena.query.Query
import com.hp.hpl.jena.query.QueryExecution
import com.hp.hpl.jena.query.QueryExecutionFactory
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP
import com.hp.hpl.jena.query.ARQ
import com.hp.hpl.jena.query.QueryFactory
import com.hp.hpl.jena.query.QuerySolution
import com.hp.hpl.jena.query.QuerySolutionMap
import com.hp.hpl.jena.query.ResultSet
import com.hp.hpl.jena.query.ResultSetFormatter
import com.hp.hpl.jena.query.Syntax
import com.hp.hpl.jena.sparql.modify.UpdateProcessRemote
import com.hp.hpl.jena.sparql.modify.UpdateProcessRemoteForm
import com.hp.hpl.jena.update.UpdateExecutionFactory
import com.hp.hpl.jena.update.UpdateFactory
import com.hp.hpl.jena.update.UpdateProcessor
import com.hp.hpl.jena.update.UpdateRequest
import com.hp.hpl.jena.rdf.model.Model
import com.hp.hpl.jena.rdf.model.ModelFactory
import com.hp.hpl.jena.rdf.model.RDFNode



/**
 * SPARQL
 * 
 * Primary class for working with a SPARQL endpoint
 * 
 * Can be initialized with an endpoint, an update endpoint (for SPARQL 1.1 Update),
 * or an Apache Jena model
 * 
 * @author Al Baker
 *
 */
@Slf4j
class Sparql {

	String endpoint
	String updateEndpoint
	Model model
	String user
	String pass
	
	// Apache Jena config parameter for setting HTTP timeout
	private final String timeoutParam = 'timeout'

	def config = [:]

	/**
	 * Static Factory 
	 * @param url sparql endpoint URL
	 * @return instance of Sparql
	 */
	public static Sparql newInstance(String url) {
		new Sparql(endpoint:url)
	}

	/**
	 * Static factory
	 * @param model Apache Jena model
	 * @return instance of Sparql
	 */
	public static Sparql newInstance(Model model) {
		new Sparql(model:model)
	}

	/**
	 * Constructor
	 * Construct the Sparql object with an Apache Jena model
	 * @param model
	 */
	Sparql(Model model) { this.model = model }

	/**
	 * Constructor
	 * Endpoint can be a query endpoint
	 * If updateEndpoint is not set, this parameter will be used for SPARQL update
	 * @param endpoint
	 */
	Sparql(String endpoint) { this.endpoint = endpoint }

	/**
	 * Constructor
	 * 
	 * Add configuration via a map, used for credentials for the HTTP Layer
	 * 
	 * @param model
	 * @param config
	 */
	Sparql(Model model, Map config) {
		this.model = model
		this.config = config
	}

	/**
	 * Constructor
	 * @param endpoint
	 * @param config
	 */
	Sparql(String endpoint, Map config) {
		this.endpoint = endpoint
		this.config = config
	}

	/**
	 * Empty constructor
	 * The properties of the Sparql class are public so this can be used if property injection happens eleswhere
	 * This allows this class to be easliy used in dependency injection frameworks, where you can either do
	 * constructor injection or property injection post-construction
	 */
	Sparql() { }

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
				((QueryEngineHTTP)qe).addParam(timeoutParam, config.timeout as String)
			}
			if (user) {
				((QueryEngineHTTP)qe).setBasicAuthentication(user, pass?.toCharArray())
			}
		}

		/**
		 * 
		 Per https://issues.apache.org/jira/browse/JENA-56 is fixed
		 */
		if (config.timeout) {
			qe.setConnectTimeout( config?.timeout?.toLong() )
		}


		try {
			for (ResultSet rs = qe.execSelect(); rs.hasNext() ; ) {
				QuerySolution sol = rs.nextSolution()
				Map<String, Object> row = [:]
				for (Iterator<String> varNames = sol.varNames(); varNames.hasNext(); ) {
					String varName = varNames.next()
					RDFNode varNode = sol.get(varName)
					row.put(varName, (varNode.isLiteral() ? varNode.asLiteral().getValue() : varNode.toString()))
				}
				closure.call(row)
			}
		} finally {
			qe.close();
		}
	}

	void each(String sparql, Closure closure) {
		Query query = QueryFactory.create(sparql, Syntax.syntaxARQ)
		QueryExecution qe = null

		/**
		 * Some explanation here - ARQ can provide a QE based on a pure
		 * SPARQL service endpoint, or a Jena model, plus you can still
		 * do remote queries with the model using the in-SPARQL "service"
		 * keyword.
		 */
		if (model) {
			qe = QueryExecutionFactory.create(query, model)
		} else {
			if (!endpoint)
				return
			qe = QueryExecutionFactory.sparqlService(endpoint, query)
			if (config.timeout) {
				((QueryEngineHTTP)qe).addParam(timeoutParam, config.timeout as String)
			}
			if (user) {
				((QueryEngineHTTP)qe).setBasicAuthentication(user, pass?.toCharArray())
			}
		}

		try {
			for (ResultSet rs = qe.execSelect(); rs.hasNext() ; ) {
				QuerySolution sol = rs.nextSolution();

				Map<String, Object> row = [:]
				for (Iterator<String> varNames = sol.varNames(); varNames.hasNext(); ) {
					String varName = varNames.next()
					RDFNode varNode = sol.get(varName)
					row.put(varName, (varNode.isLiteral() ? varNode.asLiteral().getValue() : varNode.toString()))
				}
				closure.delegate = row
				closure.call()
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
		QuerySolutionMap initialBindings = new QuerySolutionMap()

		Model tmpModel

		if (!model) {
			tmpModel = ModelFactory.createDefaultModel()
		} else {
			tmpModel = model
		}

		def processArgs =  {  key, value ->
			if (value.class == java.net.URI) {
				// this apparently works for both Resource and Property instances, in Jena
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
				QuerySolution sol = rs.nextSolution()
				Map<String, Object> row = [:]
				for (Iterator<String> varNames = sol.varNames(); varNames.hasNext(); ) {
					String varName = varNames.next()
					RDFNode varNode = sol.get(varName)
					row.put(varName, (varNode.isLiteral() ? varNode.asLiteral().getValue() : varNode.toString()))
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

			if (model) {
				qe = QueryExecutionFactory.create(sparql, model)
			} else {
				if (!endpoint)
					return
				qe = QueryExecutionFactory.sparqlService(endpoint, sparql)
				if (config.timeout) {
					((QueryEngineHTTP)qe).addParam(timeoutParam, config.timeout as String)
				}
				if (user) {
					((QueryEngineHTTP)qe).setBasicAuthentication(user, pass)
				}
			}

			m = qe.execConstruct()
		} catch (Exception e) {
			log.error "Error executing construct with ${sparql}", e
		} finally {
			if (qe) {
				qe.close();
			}
		}
		return m;
	}

	/**
	 * <code>update</code>
	 * @param query - SPARQL Update query
	 * 
	 * This method will attempt to use the updateEndpoint, and default to endpoint
	 * 
	 */
	void update(String query) {
		try {
			HttpContext httpContext = new BasicHttpContext()
			CredentialsProvider provider = new BasicCredentialsProvider()
			provider.setCredentials(new AuthScope(AuthScope.ANY_HOST,
					AuthScope.ANY_PORT), new UsernamePasswordCredentials(user, pass))
			httpContext.setAttribute(ClientContext.CREDS_PROVIDER, provider)

			UpdateRequest request = UpdateFactory.create() 

			request.add(query)

			def ep = (updateEndpoint != null) ? updateEndpoint: endpoint

			UpdateProcessor processor = UpdateExecutionFactory
					.createRemoteForm(request, ep)
			((UpdateProcessRemoteForm)processor).setHttpContext(httpContext)
			processor.execute()
			
		} catch (Exception e) {
			log.error "Error executing update with ${query}", e
			throw new RuntimeException(e)
		}

	}


}
