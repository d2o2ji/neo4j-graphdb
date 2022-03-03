package neo4j;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PathExpanders;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.Paths;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Uniqueness;
import org.neo4j.graphdb.traversal.Paths.PathDescriptor;

import neo4j.Neo4jGraphBuilder;
import neo4j.Neo4jGraphSearcher.LabeledPathDescriptor;
import neo4j.Neo4jGraphSearcher.LabeledPathDescriptorForXML;
import neo4j.ArrayExpander;

public class Neo4jGraphSearcher {
	private static GraphDatabaseService graphDb;
	private static Index<Node> indexService;
	private TraversalDescription traversal;

	private int depth;

	String relationTypeFileName = null;

	boolean xmlOutput = false;
	boolean jsonOutput = false;

	String direction = null;
//	Word2VecManager word2vec = null;

	public Neo4jGraphSearcher(String dbPath) {
		graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(new File(dbPath.trim()));

		traversal = graphDb.traversalDescription().depthFirst().uniqueness(Uniqueness.RELATIONSHIP_GLOBAL);

		registerShutdownHook();

//		 loadWord2Vec();
	}

//	public void loadWord2Vec() 
//	{
//		String prop_file = "./word2vec.properties";
//    	Properties props = new Properties();
//    	try {
//			props.load(new FileInputStream(new File(prop_file)));
//			word2vec = new Word2VecManager();
//			String model_file = props.getProperty("word2vecModelFile");
//			word2vec.loadModel(model_file);	
//		} catch (FileNotFoundException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}
	private static void registerShutdownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				graphDb.shutdown();
			}
		});
	}

	public void shutDown() {
		graphDb.shutdown();
		System.out.println("graphDB shut down.");
	}

	public void setXMLOutput(boolean xmlOutput) {
		this.xmlOutput = xmlOutput;
	}

	public boolean isXMLOutput() {
		return xmlOutput;
	}

	public void setJSONOutput(boolean jsonOutput) {
		this.jsonOutput = jsonOutput;
	}

	public boolean isJSONOutput() {
		return jsonOutput;
	}

	public void setDirection(String direction) {
		this.direction = direction;
	}

	public String getDirection() {
		return direction;
	}

	private static Node getNode(String name) {
		Node node = indexService.get(Neo4jGraphBuilder.NAME_KEY, name).getSingle();
		return node;
	}

	public void setDepth(int depth) {
		this.depth = depth;
	}

	public int getDepth() {
		return depth;
	}

	public void setRelationTypeFileName(String relationTypeFileName) {
		this.relationTypeFileName = relationTypeFileName;
	}

	public String getRelationTypeFileName() {
		return relationTypeFileName;
	}

	/**
	 * 
	 * @param nodeOne
	 * @param nodeTwo
	 */
	public void shortestPathSearch(String nodeOne, String nodeTwo) {
		try (Transaction tx = graphDb.beginTx()) {
			indexService = graphDb.index().forNodes("nodes");

			// So let's find the shortest path between Neo4j and Agent Smith
			Node n1 = getNode(nodeOne);
			Node n2 = getNode(nodeTwo);
			// START SNIPPET: shortestPathUsage

			PathFinder<Path> finder = GraphAlgoFactory.shortestPath(
					// PathExpanders.forConstantDirectionWithTypes(readRelationTypeFile(getRelationTypeFileName()))
					PathExpanders.allTypesAndDirections(), getDepth());
			Path foundPath = finder.findSinglePath(n1, n2);
			System.out.println("Shorted Path");
			if (!isXMLOutput() && !isJSONOutput()) {
				System.out.println(Paths.pathToString(foundPath, new LabeledPathDescriptor()));
			} else if (isJSONOutput()) {
				System.out.println(getJSON(foundPath));
			} else {
				String result = Paths.pathToString(foundPath, new LabeledPathDescriptorForXML());
				System.out.println("<paths>\n" + "<path length=" + "\"" + foundPath.length() + "\"" + ">\n" + result
						+ "\n</path>\n</paths>\n");
			}
		}
	}

//    public double computeSimilarity(String path)
//    {
//    	double total_score = 0.0;
//    	path = CoronaABCModelAnalyzer.getCleanTextMore(path);
//	    String[] split = path.split("--");
//	    for (int i = 0; i < split.length; i += 2) {
//
//	    	if (i < split.length-2) {
//	    		String left = split[i].trim().toLowerCase().replaceAll(" ", "_");
//	    		String right = split[i+2].trim().toLowerCase().replaceAll(" ", "_");
//	    		String triple = left + " : " +split[i+1].trim()+" : " + right;
//    	    	System.out.println("path== " + triple);
//    	    	
//    	    	double score = word2vec.getSimilarityBetweenTwoTerm(left, right);
//    	    	if (new Double(score).isNaN()) {
//    	    		score = 0.0;
//    	    	}
//    	    	
//    	    	total_score += score;
//	    	}
//	    }
//    	System.out.println(total_score);
//	    
//	    return total_score;
//    }

	/*
	 * Find path for closed discovery
	 */
	public void findBetweenPath(String nodeOne, String nodeTwo, int limit) {
		try (Transaction tx = graphDb.beginTx()) {
			indexService = graphDb.index().forNodes("nodes");

			// So let's find the all paths between nodeOne and nodeTwo
			Node n1 = getNode(nodeOne);
			Node n2 = getNode(nodeTwo);

			// we need to decide whether to use shortestPath
			PathFinder<Path> finder = null;
			if (getDirection().toLowerCase().equals("both")) {
				// finder = GraphAlgoFactory.shortestPath(
				finder = GraphAlgoFactory.allSimplePaths(
						PathExpanders.forConstantDirectionWithTypes(readRelationTypeFile(getRelationTypeFileName()))
						// new ArrayExpander(getDirections(getRelationTypeFileName()),readRelationTypeFile(getRelationTypeFileName()))
						, getDepth());
			} else {
				finder = GraphAlgoFactory.allPaths(
						// PathExpanders.forConstantDirectionWithTypes(readRelationTypeFile(getRelationTypeFileName()))
						new ArrayExpander(getDirections(getRelationTypeFileName()),readRelationTypeFile(getRelationTypeFileName())),getDepth());
			}

			try {
				String file_name = "";
				if (isXMLOutput()) {
					file_name = nodeOne.replaceAll("\\s+", "_") + "_" + nodeTwo.replaceAll("\\s+", "_") + ".xml";
				} else {
					file_name = "./result/" + nodeOne.replaceAll("\\s+", "_") + "_" + nodeTwo.replaceAll("\\s+", "_") + ".txt";
				}

				Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file_name), "utf-8"));

				if (isXMLOutput()) {
					System.out.println("<paths>\n");
					writer.write("<paths>\n");
				}

				ArrayList<String> results = new ArrayList();
				Iterable<Path> paths = finder.findAllPaths(n1, n2);
				List<Path> pathList = StreamSupport.stream(paths.spliterator(), false).collect(Collectors.toList());

				try {
					if (paths.iterator().next() == null) {
						System.out.println("There is no path");
						return;
					}
				} catch (Exception e) {
					System.out.println("There is no path");
					return;
				}
				
				for (int i = 0; i < pathList.size(); i++) {
					Path p = pathList.get(i);
					if (!isXMLOutput() && !isJSONOutput()) {
						String pa = Paths.pathToString(p, new LabeledPathDescriptor());
						
						if (!results.contains(pa)) {
							results.add(pa);
							System.out.println(pa);
						}
//	 	        		double score = computeSimilarity(pa);
//	 	        		if (!results.contains(pa) && score > 0.2) {
//	 	        			results.add(pa);
//	 	        			
//	 	        			System.out.println("both--> " + pa);	
//	 	        			writer.write(pa + "\n");
//	 	        		}

					} else if (isJSONOutput()) {
						writer.write(getJSON(p) + "\n");
					} else {
						System.out.println("<path length=" + "\"" + p.length() + "\"" + ">\n"
								+ Paths.pathToString(p, new LabeledPathDescriptorForXML()) + "\n</path>\n");
						writer.write("<path length=" + "\"" + p.length() + "\"" + ">\n"
								+ Paths.pathToString(p, new LabeledPathDescriptorForXML()) + "\n</path>\n");
					}
				}

				if (results.size() > 0) {
					for (String r : results) {
						writer.write(r + "\n");
					}
				}

				writer.close();

				System.out.println("Relation Counts: " + results.size());

			} catch (Exception e) {
				e.printStackTrace();
			}

		}
	}

	/**
	 * Traverse for open discovery
	 * 
	 * @param node
	 * 
	 */
	public void traverseBaseTraverser(String node) {
		try (Transaction tx = graphDb.beginTx()) {
			indexService = graphDb.index().forNodes("nodes");
			Node n1 = getNode(node);

			System.out.println("Paths starting from one node -- Open Discovery");

			try {
				String file_name = "";
				if (isXMLOutput()) {
					file_name = node.replaceAll("\\s+", "_") + ".xml";
				} else {
					file_name = "./result/" + node.replaceAll("\\s+", "_") + ".txt";
				}

				Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file_name), "utf-8"));
				ArrayList<String> results = new ArrayList();

				if (isXMLOutput()) {
					System.out.println("<paths>\n");
					writer.write("<paths>\n");
				}

				if (getDirection().toLowerCase().equals("both")) {
					for (Path path : traversal
							.expand(PathExpanders
									.forConstantDirectionWithTypes(readRelationTypeFile(getRelationTypeFileName())))
							// .expand(new ArrayExpander(getDirections(getRelationTypeFileName()),readRelationTypeFile(getRelationTypeFileName())))
							.evaluator(Evaluators.toDepth(getDepth())).traverse(n1)) {

						if (!isXMLOutput() && !isJSONOutput()) {
							String pa = Paths.pathToString(path, new LabeledPathDescriptor());
							if (!results.contains(pa)) {
								results.add(pa);

								System.out.println("both--> " + pa);
							}

						} else if (isJSONOutput()) {
							writer.write(getJSON(path) + "\n");
						} else {
							System.out.println("<path length=" + "\"" + path.length() + "\"" + ">\n"
									+ Paths.pathToString(path, new LabeledPathDescriptorForXML()) + "\n</path>\n");
							writer.write("<path length=" + "\"" + path.length() + "\"" + ">\n"
									+ Paths.pathToString(path, new LabeledPathDescriptorForXML()) + "\n</path>\n");
						}
					}
				} else {
					for (Path path : traversal
							.expand(new ArrayExpander(getDirections(getRelationTypeFileName()),
									readRelationTypeFile(getRelationTypeFileName())))
							.evaluator(Evaluators.toDepth(getDepth())).traverse(n1)) {

						if (!isXMLOutput()) {
							System.out.println(Paths.pathToString(path, new LabeledPathDescriptor()));
							writer.write(Paths.pathToString(path, new LabeledPathDescriptor()) + "\n");
						} else if (!isJSONOutput()) {
							writer.write(getJSON(path));
						} else {
							System.out.println("<path length=" + "\"" + path.length() + "\"" + ">\n"
									+ Paths.pathToString(path, new LabeledPathDescriptorForXML()) + "\n</path>\n");
							writer.write("<path length=" + "\"" + path.length() + "\"" + ">\n"
									+ Paths.pathToString(path, new LabeledPathDescriptorForXML()) + "\n</path>\n");
						}
					}
				}
				if (isXMLOutput()) {
					System.out.println("\n</paths>");
					writer.write("\n</paths>");
				}

				if (results.size() > 0) {
					for (String r : results) {
						writer.write(r + "\n");
					}
				}
				writer.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	/**
	 * Traverse only with chosed relation types
	 * 
	 * @param node
	 * @param relations
	 * @return
	 */
	public String traverseBaseTraverserWithType(String node, ArrayList<String> relations) {
		String output = "";
		try (Transaction tx = graphDb.beginTx()) {
			indexService = graphDb.index().forNodes("nodes");
			Node n1 = getNode(node);

			System.out.println("Open Discovery with chosen types");

			for (Path path : traversal
					// .expand(PathExpanders.forConstantDirectionWithTypes(getRelationType(relations)))
					.expand(new ArrayExpander(getDirections(relations), getRelationType(relations)))
					.evaluator(Evaluators.toDepth(getDepth())).traverse(n1)) {
				output += Paths.pathToString(path, new LabeledPathDescriptor()) + "\n";
			}

		}
		return output;
	}

	/**
	 * determine the relation type
	 * 
	 * @param relationTypeFile
	 * @return
	 */
	public static RelationshipType[] readRelationTypeFile(String relationTypeFile) {
		List<RelationshipType> types = new ArrayList();
		try {
			for (String line : FileUtils.readLines(new File(relationTypeFile), "utf-8")) {
				types.add(DynamicRelationshipType.withName(line.trim()));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		RelationshipType[] typeArray = new RelationshipType[types.size()];
		typeArray = types.toArray(typeArray);

		return typeArray;
	}

	public Direction[] getDirections(String relationTypeFile) {
		List<Direction> types = new ArrayList();
		try {
			for (String line : FileUtils.readLines(new File(relationTypeFile), "utf-8")) {
				if (getDirection().toLowerCase().equals("outgoing")) {
					types.add(Direction.OUTGOING);
				} else if (getDirection().toLowerCase().equals("incoming")) {
					types.add(Direction.INCOMING);
				} else if (getDirection().toLowerCase().equals("both")) {
					types.add(Direction.BOTH);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		Direction[] typeArray = new Direction[types.size()];
		typeArray = types.toArray(typeArray);

		return typeArray;
	}

	/**
	 * build relation type array
	 * 
	 * @param relations
	 * @return
	 */
	public RelationshipType[] getRelationType(ArrayList<String> relations) {
		List<RelationshipType> types = new ArrayList();
		try {
			for (String line : relations) {
				System.out.println(line);
				types.add(DynamicRelationshipType.withName(line.trim()));
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		RelationshipType[] typeArray = new RelationshipType[types.size()];
		typeArray = types.toArray(typeArray);

		return typeArray;
	}

	public Direction[] getDirections(ArrayList<String> relations) {
		List<Direction> types = new ArrayList();
		try {
			for (String line : relations) {
				types.add(Direction.OUTGOING);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		Direction[] typeArray = new Direction[types.size()];
		typeArray = types.toArray(typeArray);

		return typeArray;
	}

	public static void main(String[] args) throws Exception {
		String db_path = "./neo4j_db";
		String mode = "closed";  //closed, open

		mode = mode.trim();

		String inputFile = "./start_node.txt";
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(inputFile)), "UTF-8"));
		String line = "";

		for (line = br.readLine(); line != null; line = br.readLine()) {
			String nodeOne = line.trim().toLowerCase();
			String nodeTwo = "tachycardia";

			int depth = 2;
			String relationTypeFileName = "./relation_types.txt";

			String direction = "both"; // outgoing, incoming, both

			Neo4jGraphSearcher searcher = new Neo4jGraphSearcher(db_path);

			searcher.setDirection(direction);
			searcher.setDepth(depth);

			searcher.setRelationTypeFileName(relationTypeFileName);

			 int limit=1000;

			 if (mode.equals("open")) {
				System.out.println("Paths from a node -- Open Discovery");
				searcher.traverseBaseTraverser(nodeOne);
			} else {
				System.out.println("Paths in between two nodes -- Closed Discovery");
				searcher.findBetweenPath(nodeOne, nodeTwo, limit);
			}
			searcher.shutDown();
		}
	}

	
	 // Modified version of DefaultPathDescriptor
	public static class LabeledPathDescriptor<T extends Path> implements PathDescriptor<T> {
		public String nodeRepresentation(Path path, Node node) {
			return "(" + node.getProperty("name") + ")";
		}

		public String relationshipRepresentation(Path path, Node from, Relationship relationship) {
			String prefix = "--", suffix = "--";
			if (from.equals(relationship.getEndNode())) {
				prefix = "<--";
			} else {
				suffix = "-->";
			}

			return prefix + "[" + relationship.getType().name() + "]" + suffix;
		}
	}

	public static class LabeledPathDescriptorForXML<T extends Path> implements PathDescriptor<T> {
		public String nodeRepresentation(Path path, Node node) {
			String result = "<node cui=" + "\"" + node.getProperty("cui") + "\"" + " type=" + "\""
					+ node.getProperty("type") + "\"" + ">"
					+ StringEscapeUtils.escapeXml((String) node.getProperty("name")) + "</node>\n";

			return result;
		}

		public String relationshipRepresentation(Path path, Node from, Relationship relationship) {
			String prefix = "--", suffix = "--";
			if (from.equals(relationship.getEndNode())) {
				prefix = "<--";
			} else {
				suffix = "-->";
			}

			// return prefix + "[" + relationship.getType().name() + "]" + suffix;
			return "<arrow>to_left</arrow>\n" + "<relation_type>" + relationship.getType().name() + "</relation_type>\n"
					+ "<negation>" + relationship.getProperty("negation") + "</negation>\n"
					+ "<arrow>to_right</arrow>\n";

		}
	}

	public String getJSON(Path p) {
		JSONArray jsonArray = new JSONArray();

		Iterator<Relationship> r_it = p.relationships().iterator();
		while (r_it.hasNext()) {
			Relationship rel = r_it.next();
			String edge_path = rel.getType().toString();

			JSONObject jsonObject = new JSONObject();

			if (!jsonObject.containsValue(rel.getStartNode().getProperty("name"))
					&& !jsonObject.containsValue(rel.getStartNode().getProperty("name"))) {
				jsonObject.put("source", rel.getStartNode().getProperty("name"));
				jsonObject.put("target", rel.getEndNode().getProperty("name"));
				jsonObject.put("type", edge_path);

				jsonArray.add(jsonObject);
			}
		}

		return jsonArray.toJSONString();
	}

}
