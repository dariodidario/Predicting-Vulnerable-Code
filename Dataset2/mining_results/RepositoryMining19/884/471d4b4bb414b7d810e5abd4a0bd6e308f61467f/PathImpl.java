// $Id:$
/*
* JBoss, Home of Professional Open Source
* Copyright 2008, Red Hat Middleware LLC, and individual contributors
* by the @authors tag. See the copyright.txt in the distribution for a
* full listing of individual contributors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.hibernate.validation.engine;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.validation.Path;

/**
 * @author Hardy Ferentschik
 */
public class PathImpl implements Path {

	/**
	 * Regular expression used to split a string path into its elements.
	 *
	 * @see <a href="http://www.regexplanet.com/simple/index.jsp">Regular expression tester</a>
	 */
	private static final Pattern pathPattern = Pattern.compile( "(\\w+)(\\[(\\w+)\\])?(\\.(.*))*" );

	private static final String PROPERTY_PATH_SEPERATOR = ".";

	private static final Node ROOT_NODE = new NodeImpl( ( String ) null );

	private final List<Node> nodeList;

	/**
	 * Returns a {@code Path} instance representing the path described by the given string. To create a root node the empty string should be passed.
	 *
	 * @param propertyPath the path as string representation.
	 *
	 * @return a {@code Path} instance representing the path described by the given string.
	 *
	 * @throws IllegalArgumentException in case {@code property == null} or {@code property} cannot be parsed.
	 */
	public static PathImpl createPathFromString(String propertyPath) {
		if ( propertyPath == null ) {
			throw new IllegalArgumentException( "null is not allowed as property path." );
		}

		if ( propertyPath.length() == 0 ) {
			return createNewRootPath();
		}

		return parseProperty( propertyPath );
	}

	public static PathImpl createNewRootPath() {
		return new PathImpl();
	}

	public PathImpl(PathImpl path) {
		this.nodeList = new ArrayList<Node>();
		Iterator<Node> iter = path.iterator();
		while ( iter.hasNext() ) {
			nodeList.add( new NodeImpl( iter.next() ) );
		}
	}

	private PathImpl() {
		nodeList = new ArrayList<Node>();
		nodeList.add( ROOT_NODE );
	}

	private PathImpl(List<Node> nodeList) {
		this.nodeList = new ArrayList<Node>();
		for ( Node node : nodeList ) {
			this.nodeList.add( new NodeImpl( node ) );
		}
	}

	public Path getPathWithoutLeafNode() {
		List<Node> nodes = new ArrayList<Node>( nodeList );
		if ( nodes.size() > 1 ) {
			nodes.remove( nodes.size() - 1 );
		}
		return new PathImpl( nodes );
	}

	public void addNode(Node node) {
		nodeList.add( node );
	}

	public Node removeLeafNode() {
		if ( nodeList.size() == 0 ) {
			throw new IllegalStateException( "No nodes in path!" );
		}
		if ( nodeList.size() == 1 ) {
			throw new IllegalStateException( "Root node cannot be removed!" );
		}
		return nodeList.remove( nodeList.size() - 1 );
	}

	public NodeImpl getLeafNode() {
		if ( nodeList.size() == 0 ) {
			throw new IllegalStateException( "No nodes in path!" );
		}
		return ( NodeImpl ) nodeList.get( nodeList.size() - 1 );
	}

	public Iterator<Path.Node> iterator() {
		return nodeList.iterator();
	}

	public boolean isSubPathOf(Path path) {
		Iterator<Node> pathIter = path.iterator();
		Iterator<Node> thisIter = iterator();
		while ( pathIter.hasNext() ) {
			Node pathNode = pathIter.next();
			if ( !thisIter.hasNext() ) {
				return false;
			}
			Node thisNode = thisIter.next();
			if ( !thisNode.equals( pathNode ) ) {
				return false;
			}
		}
		return true;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		Iterator<Path.Node> iter = iterator();
		while ( iter.hasNext() ) {
			Node node = iter.next();
			if ( ROOT_NODE.equals( node ) ) {
				continue;
			}
			builder.append( node.toString() );
			if ( iter.hasNext() ) {
				builder.append( PROPERTY_PATH_SEPERATOR );
			}
		}
		return builder.toString();
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		PathImpl path = ( PathImpl ) o;

		if ( nodeList != null ? !nodeList.equals( path.nodeList ) : path.nodeList != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		return nodeList != null ? nodeList.hashCode() : 0;
	}

	private static PathImpl parseProperty(String property) {
		PathImpl path = new PathImpl();
		String tmp = property;
		do {
			Matcher matcher = pathPattern.matcher( tmp );
			if ( matcher.matches() ) {
				String value = matcher.group( 1 );
				String index = matcher.group( 3 );
				NodeImpl node = new NodeImpl( value );
				if ( index != null ) {
					node.setInIterable( true );
					try {
						Integer i = Integer.parseInt( index );
						node.setIndex( i );
					}
					catch ( NumberFormatException e ) {
						node.setKey( index );
					}
				}
				path.addNode( node );
				tmp = matcher.group( 5 );
			}
			else {
				throw new IllegalArgumentException( "Unable to parse property path " + property );
			}
		} while ( tmp != null );
		return path;
	}
}
