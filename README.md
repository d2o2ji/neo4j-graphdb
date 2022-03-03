# neo4j-graphdb
a project for using Neo4j graph database in Java

## Neo4jGraphBuilder
a class for building Neo4j database

### Input:
a txt file consisting of relations between two entities   
#### Relation format:   
['document_id' | 'sentence_id' | 'entity1_span' | 'entity1_name' | 'entity1_type' | 'entity2_span' |  'entity2_name' | 'entity2_type' | 'negation' | 'tense' | 'relation_verb' | 'relation_type' | 'sentence']
#### Relation example:
26655260|16856|5 6|bisphenol A|COMPOUND|25 25|insulin|GENE|POSITIVE|ACTIVE|VERB|TREATS|In rodents, acute exposure to bisphenol A is responsible for modifications of insulin synthesis and secretion in pancreatic beta cells but also for modifications of insulin signalling in liver, skeletal muscle and adipose tissue, which both lead to insulin-resistance, a major condition in pathophysiology of metabolic syndrome, obesity and type 2 diabetes.

### Output:
a Neo4j database folder consisting of nodes and relations   

   
   
## Neo4jGraphSearcher
a class for retrieving paths from a Neo4j database built with Neo4jGraphBuilder   
- A maximum depth of paths can be specified with the variable "depth".   
- A discovery mode can be specified with the variable "mode".   
- You can choose one of two types of path discovery: closed discovery or open discovery.   
- Closed discovery: paths in between two nodes --> findBetweenPath( )   
- Open discovery: paths from a node --> traverseBaseTraverser( )

### Input:
a Neo4j database folder (=an output of Neo4jGraphBuilder)

### Output:
a txt file consisting of paths between a start node and an end node
#### Output example (depth 2):
(campesterol)--[COEXISTS_WITH]-->(ezetimibe)<--[TREATS]--(hyperlipidemia)

