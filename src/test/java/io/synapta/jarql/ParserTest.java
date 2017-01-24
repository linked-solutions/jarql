/*
 * The MIT License
 *
 * Copyright 2016 user.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.synapta.jarql;


import io.synapta.jarql.JarqlExecutor;
import io.synapta.jarql.JarqlParser;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.ImmutableGraph;
import org.apache.clerezza.commons.rdf.impl.utils.simple.SimpleGraph;
import org.apache.clerezza.rdf.core.serializedform.Parser;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author user
 */
public class ParserTest {

    public ParserTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    static void testFromResource(String baseName) throws Exception {
        final Parser parser = Parser.getInstance();
        final InputStream jsonGraphTurtle = ParserTest.class.getResourceAsStream(baseName+"_raw.ttl");
        final ImmutableGraph expectedJsonGraph = parser.parse(jsonGraphTurtle, SupportedFormat.TURTLE);
        
        final InputStream queryStream = ParserTest.class.getResourceAsStream(baseName+".query");
        String queryString = new BufferedReader(new InputStreamReader(queryStream)).lines().collect(Collectors.joining("\n"));
        
        final InputStream finalGraphTurtle = ParserTest.class.getResourceAsStream(baseName+".ttl");
        final ImmutableGraph expectedFinalGraph = parser.parse(finalGraphTurtle, SupportedFormat.TURTLE);
        
        testFromResource(baseName+".json", expectedJsonGraph, expectedFinalGraph, queryString);
    }
    static void testFromResource(String fileName, ImmutableGraph expectedJsonGRaph, ImmutableGraph expectedFinalGraph, String queryString ) throws Exception {
        final InputStream inJson = ParserTest.class.getResourceAsStream(fileName);
        final Graph graph = new SimpleGraph();
        JarqlParser.parse(inJson, graph);
        final ImmutableGraph result = graph.getImmutableGraph();
        Assert.assertEquals("JsonGraph wrong", expectedJsonGRaph, result);
        
        final InputStream inJsonAgain = ParserTest.class.getResourceAsStream(fileName);
        
        Graph executorResultGraph = JarqlExecutor.execute(inJsonAgain, queryString);
        final ImmutableGraph resultExecutor = executorResultGraph.getImmutableGraph();
        Assert.assertEquals("ResultGraph wrong", expectedFinalGraph, resultExecutor);
    }
    
    @Test
    public void simple() throws Exception {
        testFromResource("paperino");
    }
    
}
