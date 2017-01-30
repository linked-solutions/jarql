[![Build Status](https://travis-ci.org/jarql/jarql.svg?branch=master)](https://travis-ci.org/jarql/jarql)


# JARQL
SPARQL construct queries on JSON files

JARQL allows to execute SPARQL Contsruct Queries against JSON files.  The project was inspired by [Tarql](https://github.com/tarql/tarql) which provides similar functionality for CSV files. With JARQL you can easily transform JSON Documents to RDF.

## Usage

    java -jar jarql-<version>.jar <JSON-File> <Query-File>

For example is you have a file called `paperino.json` with the following content:

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
prefix : <http://example.com/>
prefix jarql: <http://jarql.com/>

construct { ?p :name ?n; :child [:name ?cn].}
where {
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
## Current limitations

 * Nested arrays in JSON are not supported
 * The order of JSon Arrays is irrelevant
