/*
 * The MIT License
 *
 * Copyright 2016 Zazuko GmbH.
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

import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import javax.json.Json;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;
import javax.json.stream.JsonParserFactory;
import org.apache.clerezza.commons.rdf.BlankNode;
import org.apache.clerezza.commons.rdf.BlankNodeOrIRI;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.Literal;
import org.apache.clerezza.commons.rdf.RDFTerm;
import org.apache.clerezza.commons.rdf.Triple;
import org.apache.clerezza.commons.rdf.impl.utils.PlainLiteralImpl;
import org.apache.clerezza.commons.rdf.impl.utils.TripleImpl;
import org.apache.clerezza.commons.rdf.impl.utils.TypedLiteralImpl;
import org.apache.clerezza.rdf.ontologies.RDF;
import org.apache.clerezza.rdf.ontologies.XSD;

/**
 *
 * @author user
 */
public class JarqlParser {
    
    final String prefix = "http://jarql.com/";
    final IRI ROOT = new IRI(prefix+"root");
    final IRI ROOT_TYPE = new IRI(prefix+"Root");



    static void parse(final InputStream in, final OutputStream out) {
        final PrintWriter printWriter;
        try {
            printWriter = new PrintWriter(new OutputStreamWriter(out, "utf-8"), true);
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
        parse(in, new TripleSink() {

            final WeakHashMap<BlankNode, String> node2IdMap = new WeakHashMap<>();
            int idCounter = 1;

            @Override
            public void add(Triple triple) {
                printWriter.println(toNT(triple));
            }

            private String toNT(Triple triple) {
                return toNT(triple.getSubject()) + " " + toNT(triple.getPredicate()) + " " + toNT(triple.getObject()) + ".";
            }

            private String toNT(Literal literal) {
                //TODO real impl
                return literal.toString();
            }

            private String toNT(IRI iri) {
                //TODO real impl
                return iri.toString();
            }

            private String toNT(BlankNode node) {
                String id = node2IdMap.get(node);
                if (id == null) {
                    id = Integer.toString(idCounter);
                    idCounter++;
                    node2IdMap.put(node, id);
                }
                return "_:" + id;
            }

            private String toNT(RDFTerm node) {
                if (node instanceof Literal) {
                    return toNT((Literal) node);
                } else {
                    return toNT((BlankNodeOrIRI) node);
                }
            }

            private String toNT(BlankNodeOrIRI node) {
                if (node instanceof IRI) {
                    return toNT((IRI) node);
                } else {
                    return toNT((BlankNode) node);
                }
            }
        });
    }

    static void parse(final InputStream in, final Graph graph) {
        parse(in, new TripleSink() {
            @Override
            public void add(Triple triple) {
                graph.add(triple);
            }

        });
    }

    static void parse(InputStream in, TripleSink sink) {
        final JsonParserFactory factory = Json.createParserFactory(null);
        final JsonParser jsonParser = factory.createParser(in, Charset.forName("utf-8"));
        JarqlParser jsonLdParser = new JarqlParser(jsonParser, sink);
        jsonLdParser.parse();
    }

    private final JsonParser jsonParser;
    private final TripleSink sink;
    private final Map<String, BlankNode> label2bnodeMap = new HashMap<>();

    private JarqlParser(JsonParser jsonParser, TripleSink sink) {
        this.jsonParser = jsonParser;
        this.sink = sink;
    }

    private void parse() {
        final Event firstEvent = jsonParser.next();
        switch (firstEvent) {
            case START_OBJECT: {
                parseRootJsonObject();
                break;
            }
            case START_ARRAY: {
                parseRootJsonArray();
                break;
            }
            default: {
                throw new RuntimeException("Document should start with object: " + firstEvent);
            }
        }
    }

    private void parseRootJsonObject() {
        parseJsonObject(ROOT);
        sink.add(new TripleImpl(ROOT, RDF.type, ROOT_TYPE));
    }
    
    private void parseRootJsonArray() {
        parseJsonArray(object -> {
            if (object instanceof Literal) {
                throw new RuntimeException("Elements of root array must not be literal");
            }
            sink.add(new TripleImpl((BlankNodeOrIRI) object, RDF.type, ROOT_TYPE));
        });
    }

    
    

    public void parseJsonObject(BlankNodeOrIRI subject) {
        ALLKEYS: while(true) {
            JsonParser.Event event = jsonParser.next();
            if (event == Event.END_OBJECT) {
                break ALLKEYS;
            }
            if (!event.equals(JsonParser.Event.KEY_NAME)) {
                throw new RuntimeException("Sorry: " + event+ jsonParser.getString());
            }
            String key = jsonParser.getString();
            final IRI predicate = new IRI(prefix + key);
            final Event next = jsonParser.next();
            switch (next) {
                case VALUE_STRING: 
                case VALUE_NUMBER: {
                    final String value = jsonParser.getString();
                    final Literal literal = new PlainLiteralImpl(value);
                    sink.add(new TripleImpl(subject, predicate, literal));
                    break;
                }
                case VALUE_TRUE: {
                    final Literal literal = new TypedLiteralImpl("true", XSD.boolean_);
                    sink.add(new TripleImpl(subject, predicate, literal));
                    break;
                }
                case VALUE_FALSE: {
                    final Literal literal = new TypedLiteralImpl("false", XSD.boolean_);
                    sink.add(new TripleImpl(subject, predicate, literal));
                    break;
                }
                case START_ARRAY: {
                    //new JsonArrayParser(subject, predicate);
                    parseJsonArray(object -> sink.add(new TripleImpl(subject, predicate, object)));
                    break;
                }
                case START_OBJECT: {
                    final BlankNode object = new BlankNode();
                    sink.add(new TripleImpl(subject, predicate, object));
                    parseJsonObject(object);
                    break;
                }
                case VALUE_NULL: {
                	break;
                }
                default: {
                    throw new RuntimeException("Not supported here: " + next);
                }
            }
        }
    }

    
    
    private void parseJsonArray(Consumer<RDFTerm> elementProcessor) {
        ARRAY: while (true) {
            JsonParser.Event element = jsonParser.next();
            switch (element) {
                case VALUE_STRING: {
                    final String value = jsonParser.getString();
                    final Literal literal = new PlainLiteralImpl(value);
                    elementProcessor.accept(literal);
                    break;
                }
                case START_ARRAY: {
                    throw new RuntimeException("We don't know what to do with nested arrays");
                }
                case START_OBJECT: {
                    final BlankNode object = new BlankNode();
                    elementProcessor.accept(object);
                    parseJsonObject(object);
                    break;
                }
                case END_ARRAY: {
                    break ARRAY;
                }
                default: {
                    throw new RuntimeException("Not supported here: " + element);
                }
            }
        }
    }
}
