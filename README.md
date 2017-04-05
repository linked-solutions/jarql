[![Build Status](https://travis-ci.org/jarql/jarql.svg?branch=master)](https://travis-ci.org/jarql/jarql)


# JARQL
SPARQL construct queries on JSON files

JARQL allows to execute SPARQL Construct Queries against JSON files.  The project was inspired by [Tarql](https://github.com/tarql/tarql) which provides similar functionality for CSV files. With JARQL you can easily transform JSON Documents to RDF.

## Build
Instead of building yourself you can download it from the release page.

In order to create an executable .jar containing all the dependencies just run:

    mvn package -Pexecutable

## Usage

    java -jar jarql-<version>.jar <JSON-File> <Query-File>

For example if you have a file called `paperino.json` with the following content:

```json
{
        "parent": [{
                "name": "Paperino",
                "children": ["Qui", "Quo", "Qua"],
                "fiance": {"name": "Paperina"}
        }, {
                "name": "Topolino",
                "children": ["Tip", "Tap", "Top"],
                "fiance": {"name": "Minnie"}
        }]
}
```

And a file called `paperino.query` with the following content:

```sparql
PREFIX : <http://example.com/>
PREFIX jarql: <http://jarql.com/>

CONSTRUCT { 
    ?p :name ?n; 
       :child [:name ?cn].
}
WHERE {
    jarql:root jarql:parent ?p.
    ?p jarql:name ?n.
    ?p jarql:children ?cn.
}
```

Invoking `jarql-1.0-pre1.jar` as follows:

    java -jar jarql-1.0-pre1.jar paperino.json paperino.query

will output the following RDF:

```turtle
[ <http://example.com/child>  [ <http://example.com/name>
                    "Qui"^^<http://www.w3.org/2001/XMLSchema#string> ] ;
  <http://example.com/child>  [ <http://example.com/name>
                    "Qua"^^<http://www.w3.org/2001/XMLSchema#string> ] ;
  <http://example.com/child>  [ <http://example.com/name>
                    "Quo"^^<http://www.w3.org/2001/XMLSchema#string> ] ;
  <http://example.com/name>   "Paperino"^^<http://www.w3.org/2001/XMLSchema#string>
] .

[ <http://example.com/child>  [ <http://example.com/name>
                    "Top"^^<http://www.w3.org/2001/XMLSchema#string> ] ;
  <http://example.com/child>  [ <http://example.com/name>
                    "Tap"^^<http://www.w3.org/2001/XMLSchema#string> ] ;
  <http://example.com/child>  [ <http://example.com/name>
                    "Tip"^^<http://www.w3.org/2001/XMLSchema#string> ] ;
  <http://example.com/name>   "Topolino"^^<http://www.w3.org/2001/XMLSchema#string>
] .

```
### Advanced usage
Add all metadata you want using ```BIND``` combined with [SPARQL 1.1 functions](https://www.w3.org/TR/sparql11-query/#SparqlOps)!

Create URI...
```
BIND (URI(CONCAT('http://example.com/ns#', ?var)) AS ?uri)
```
...add language tags...
```
BIND (STRLANG(?string, 'en') AS ?with_language_tag)
```
...or datatypes!
```
BIND (STRDT(?num, xsd:integer) AS ?int)
```

## Current limitations

 * Nested arrays in JSON are not supported
 * The order of JSon Arrays is irrelevant
