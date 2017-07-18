/*-
 * ========================LICENSE_START=================================
 * LUCON Data Flow Policy Engine
 * %%
 * Copyright (C) 2017 Fraunhofer AISEC
 * %%
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
 * =========================LICENSE_END==================================
 */
package de.fhg.ids.dataflowcontrol.lucon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import alice.tuprolog.InvalidTheoryException;
import alice.tuprolog.MalformedGoalException;
import alice.tuprolog.NoMoreSolutionException;
import alice.tuprolog.NoSolutionException;
import alice.tuprolog.SolveInfo;
import de.fhg.aisec.ids.api.policy.DecisionRequest;
import de.fhg.aisec.ids.api.policy.Obligation;
import de.fhg.aisec.ids.api.policy.PolicyDecision;
import de.fhg.aisec.ids.api.policy.PolicyDecision.Decision;
import de.fhg.aisec.ids.api.policy.ServiceNode;
import de.fhg.aisec.ids.api.policy.TransformationDecision;
import de.fhg.ids.dataflowcontrol.PolicyDecisionPoint;

/**
 * Unit tests for the LUCON policy engine.
 * 
 * @author Julian Schuette (julian.schuette@aisec.fraunhofer.de)
 *
 */
public class LuconEngineTest {
	// Solving Towers of Hanoi in only two lines. Prolog FTW!
	private final static String HANOI_THEORY = 	"move(1,X,Y,_) :-  write('Move top disk from '), write(X), write(' to '), write(Y), nl. \n" +
											"move(N,X,Y,Z) :- N>1, M is N-1, move(M,X,Z,Y), move(1,X,Y,_), move(M,Z,Y,X). ";

	// A random but syntactically correct policy.
	private final static String EXAMPLE_POLICY = 
			"%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n" + 
			"%   Prolog representation of a data flow policy\n" + 
			"%   \n" + 
			"%   Source: test2\n" + 
			"% 	\n" + 
			"%	Do not edit this file, it has been generated automatically\n" + 
			"% 	by XText/Xtend.\n" + 
			"%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n" + 
			"%\n" + 
			"% Only required for SWI-Prolog\n" + 
			"% Allow the following predicates to be scattered around the prolog file.\n" + 
			"% Otherwise Prolog will issue a warning if they are not stated in subsequent lines.		\n" + 
			"%:- discontiguous service/1.\n" + 
			"%:- discontiguous rule/1.\n" + 
			"%:- discontiguous has_capability/2.\n" + 
			"%:- discontiguous has_property/3.\n" + 
			"%:- discontiguous has_target/2.\n" + 
			"%:- discontiguous requires_prerequisites/2.\n" + 
			"%:- discontiguous has_alternativedecision/2.\n" + 
			"%:- discontiguous has_obligation/2.\n" + 
			"%:- discontiguous receives_label/2.		\n" + 
			"regex(A,B,C) :- class(\"java.util.regex.Pattern\") <- matches(A,B) returns C.\n" +
			"%%%%%%%% Rules %%%%%%%%%%%%\n" + 
			"rule(deleteAfterOneMonth).\n" + 
			"has_target(deleteAfterOneMonth, service78096644).\n" + 
			"service(service78096644).\n" + 
			"has_endpoint(service78096644, \"hdfs.*\").\n" + 
			"receives_label(deleteAfterOneMonth,private).\n" + 
			"has_obligation(deleteAfterOneMonth, obl1709554620).\n" + 
			"requires_prerequisite(obl1709554620, delete_after_days(30)).\n" + 
			"has_alternativedecision(obl1709554620, drop).\n" + 
			"rule(anotherRule).\n" + 
			"has_target(anotherRule, hiveMqttBroker). \n" + 
			"receives_label(anotherRule,private).\n" + 
			"has_decision(anotherRule, drop).\n" + 
			"\n" + 
			"%%%%% Services %%%%%%%%%%%%\n" + 
			"service(anonymizer).\n" + 
			"has_endpoint(anonymizer, \".*anonymizer.*\").\n" + 
			"has_property(anonymizer,myProp,anonymize('surname', 'name')).\n" + 
			"service(hiveMqttBroker).\n" + 
			"creates_label(hiveMqttBroker, labelone).\n" + 
			"removes_label(hiveMqttBroker, labeltwo).\n" + 
			"has_endpoint(hiveMqttBroker, \"^paho:.*?tcp://broker.hivemq.com:1883.*\").\n" + 
			"has_property(hiveMqttBroker,type,public).\n" + 
			"service(testQueue).\n" + 
			"has_endpoint(testQueue, \"^amqp:.*?:test\").\n" + 
			"service(hadoopClusters).\n" + 
			"has_endpoint(hadoopClusters, \"hdfs://.*\").\n" + 
			"has_capability(hadoopClusters,deletion).\n" + 
			"has_property(hadoopClusters,anonymizes,anonymize('surname', 'name')).\n";
	
	/**
	 * Loading a valid Prolog theory should not fail.
	 * 
	 * @throws InvalidTheoryException
	 * @throws IOException
	 */
	@Test
	public void testLoadingTheoryGood() throws InvalidTheoryException, IOException {
		LuconEngine e = new LuconEngine(null);
		e.loadPolicy(new ByteArrayInputStream(HANOI_THEORY.getBytes()));
		String json = e.getTheoryAsJSON();
		assertTrue(json.startsWith("{\"theory\":\"move(1,X,Y,"));
		String prolog = e.getTheory();
		assertTrue(prolog.trim().startsWith("move(1,X,Y"));
	}

	/**
	 * Loading an invalid Prolog theory is expected to throw an exception.
	 * 
	 * @throws InvalidTheoryException
	 * @throws IOException
	 */
	@Test
	public void testLoadingTheoryNotGood() throws InvalidTheoryException, IOException {
		LuconEngine e = new LuconEngine(System.out);
		try {
			e.loadPolicy(new ByteArrayInputStream("This is invalid".getBytes()));
		} catch (InvalidTheoryException ex) {
			return;	// Expected
		}
		fail("Could load invalid theory without exception");
	}
	
	/**
	 * Solve a simple Prolog puzzle.
	 * 
	 * @throws InvalidTheoryException
	 * @throws IOException
	 * @throws NoMoreSolutionException
	 */
	@Test
	public void testSolve1() throws InvalidTheoryException, IOException, NoMoreSolutionException {
		LuconEngine e = new LuconEngine(System.out);
		e.loadPolicy(new ByteArrayInputStream(HANOI_THEORY.getBytes()));
		try {
			List<SolveInfo> solutions = e.query("move(3,left,right,center). ", true);
			assertTrue(solutions.size()==1);
			for (SolveInfo solution : solutions) {
				System.out.println(solution.getSolution().toString());
				System.out.println(solution.hasOpenAlternatives());
				
				System.out.println(solution.isSuccess());
			}
		} catch (MalformedGoalException | NoSolutionException e1) {
			e1.printStackTrace();
			fail(e1.getMessage());
		}
	}
	
	/**
	 * Run some simple queries against an actual policy.
	 * 
	 * @throws InvalidTheoryException
	 * @throws IOException
	 * @throws NoMoreSolutionException
	 */
	@Test
	public void testSolve2() throws InvalidTheoryException, IOException, NoMoreSolutionException {
		LuconEngine e = new LuconEngine(System.out);
		e.loadPolicy(new ByteArrayInputStream(EXAMPLE_POLICY.getBytes()));
		try {
			List<SolveInfo> solutions = e.query("has_endpoint(X,Y),regex(Y, \"hdfs://myendpoint\",C),C.", true);
			assertNotNull(solutions);
			assertEquals(2,solutions.size());
			for (SolveInfo solution : solutions) {
				System.out.println(solution.getSolution().toString());
				System.out.println(solution.hasOpenAlternatives());				
				System.out.println(solution.isSuccess());
			}
		} catch (MalformedGoalException | NoSolutionException e1) {
			e1.printStackTrace();
			fail(e1.getMessage());
		}
	}
	
	/**
	 * Test if the correct policy decisions are taken.
	 */
	@Test
	public void testPolicyDecision() {
		PolicyDecisionPoint pdp = new PolicyDecisionPoint();
		pdp.activate(null);
		pdp.loadPolicy(new ByteArrayInputStream(EXAMPLE_POLICY.getBytes()));
		
		// Simple message context with nonsense attributes
		Map<String, Object> attributes = new HashMap<>();
		attributes.put("some_message_key", "some_message_value");
		
		// Simple source and dest nodes
		ServiceNode source = new ServiceNode("seda:test_source", null, null);
		ServiceNode dest= new ServiceNode("hdfs://some_url", null, null);
		
		PolicyDecision dec = pdp.requestDecision(new DecisionRequest(source, dest, attributes, null));
		assertEquals(Decision.ALLOW, dec.getDecision());
		
		// Check obligation
		Obligation obl = dec.getObligation();
		assertEquals("delete_after_days(30)", obl.getAction());
	}

	/**
	 * List all rules of the currently loaded policy.
	 */
	@Test
	public void testListRules() {
		PolicyDecisionPoint pdp = new PolicyDecisionPoint();
		pdp.activate(null);
		
		// Without any policy, we expect an empty list of rules
		List<String> emptyList = pdp.listRules();
		assertNotNull(emptyList);
		assertTrue(emptyList.isEmpty());
				
		// Load a policy
		pdp.loadPolicy(new ByteArrayInputStream(EXAMPLE_POLICY.getBytes()));
		
		// We now expect 2 rules
		List<String> rules = pdp.listRules();
		assertNotNull(rules);
		assertEquals(2, rules.size());
		assertTrue(rules.contains("deleteAfterOneMonth"));
		assertTrue(rules.contains("anotherRule"));
	}
	
	@Test
	public void testTransformations() {
		PolicyDecisionPoint pdp = new PolicyDecisionPoint();
		pdp.activate(null);
		pdp.loadPolicy(new ByteArrayInputStream(EXAMPLE_POLICY.getBytes()));
		ServiceNode node = new ServiceNode("paho:tcp://broker.hivemq.com:1883/blablubb", null, null);
		TransformationDecision trans = pdp.requestTranformations(node);
		
		assertNotNull(trans);
		assertNotNull(trans.getLabelsToAdd());
		assertNotNull(trans.getLabelsToRemove());
		
		assertEquals(1, trans.getLabelsToAdd().size());
		assertEquals(1, trans.getLabelsToRemove().size());

		assertTrue(trans.getLabelsToAdd().contains("labelone"));
		assertTrue(trans.getLabelsToRemove().contains("labeltwo"));
	}
}
