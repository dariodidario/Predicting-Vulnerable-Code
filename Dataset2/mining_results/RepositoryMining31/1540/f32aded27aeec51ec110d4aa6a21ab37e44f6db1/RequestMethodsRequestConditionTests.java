/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.servlet.mvc.condition;

import java.util.Collections;

import javax.servlet.http.HttpServletRequest;

import org.junit.Test;

import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.web.bind.annotation.RequestMethod;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.HEAD;
import static org.springframework.web.bind.annotation.RequestMethod.OPTIONS;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

/**
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 */
public class RequestMethodsRequestConditionTests {

	@Test
	public void getMatchingCondition() {
		testMatch(new RequestMethodsRequestCondition(GET), GET);
		testMatch(new RequestMethodsRequestCondition(GET, POST), GET);
		testNoMatch(new RequestMethodsRequestCondition(GET), POST);
	}

	@Test
	public void getMatchingConditionWithHttpHead() {
		testMatch(new RequestMethodsRequestCondition(HEAD), HEAD);
		testMatch(new RequestMethodsRequestCondition(GET), HEAD);
		testNoMatch(new RequestMethodsRequestCondition(POST), HEAD);
	}

	@Test
	public void getMatchingConditionWithEmptyConditions() {
		RequestMethodsRequestCondition condition = new RequestMethodsRequestCondition();
		for (RequestMethod method : RequestMethod.values()) {
			if (!OPTIONS.equals(method)) {
				HttpServletRequest request = new MockHttpServletRequest(method.name(), "");
				assertNotNull(condition.getMatchingCondition(request));
			}
		}
		testNoMatch(condition, OPTIONS);
	}

	@Test
	public void getMatchingConditionWithCustomMethod() {
		HttpServletRequest request = new MockHttpServletRequest("PROPFIND", "");
		assertNotNull(new RequestMethodsRequestCondition().getMatchingCondition(request));
		assertNull(new RequestMethodsRequestCondition(GET, POST).getMatchingCondition(request));
	}

	@Test
	public void compareTo() {
		RequestMethodsRequestCondition c1 = new RequestMethodsRequestCondition(GET, HEAD);
		RequestMethodsRequestCondition c2 = new RequestMethodsRequestCondition(POST);
		RequestMethodsRequestCondition c3 = new RequestMethodsRequestCondition();

		MockHttpServletRequest request = new MockHttpServletRequest();

		int result = c1.compareTo(c2, request);
		assertTrue("Invalid comparison result: " + result, result < 0);

		result = c2.compareTo(c1, request);
		assertTrue("Invalid comparison result: " + result, result > 0);

		result = c2.compareTo(c3, request);
		assertTrue("Invalid comparison result: " + result, result < 0);

		result = c1.compareTo(c1, request);
		assertEquals("Invalid comparison result ", 0, result);
	}

	@Test
	public void combine() {
		RequestMethodsRequestCondition condition1 = new RequestMethodsRequestCondition(GET);
		RequestMethodsRequestCondition condition2 = new RequestMethodsRequestCondition(POST);

		RequestMethodsRequestCondition result = condition1.combine(condition2);
		assertEquals(2, result.getContent().size());
	}


	private void testMatch(RequestMethodsRequestCondition condition, RequestMethod method) {
		MockHttpServletRequest request = new MockHttpServletRequest(method.name(), "");
		RequestMethodsRequestCondition actual = condition.getMatchingCondition(request);
		assertNotNull(actual);
		assertEquals(Collections.singleton(method), actual.getContent());
	}

	private void testNoMatch(RequestMethodsRequestCondition condition, RequestMethod method) {
		MockHttpServletRequest request = new MockHttpServletRequest(method.name(), "");
		assertNull(condition.getMatchingCondition(request));
	}

}
