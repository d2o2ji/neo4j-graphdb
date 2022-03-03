package neo4j;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.FileReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Properties;
import java.util.Scanner;
import java.util.regex.Pattern;
import org.apache.commons.lang3.tuple.Pair;

import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.index.Index;

public class Neo4jGraphBuilder {
	String pattern = "([0-9]+)(\\.tx\\.[0-9]+\\|)(.*)";
	
    public static final String NAME_KEY = "name";
    private static final String TYPE_KEY = "type";
//	public static String cypherFile = "./paths.txt";

    private static GraphDatabaseService graphDb;
    private static Index<Node> indexService;
    ArrayList<String> relationTypeList = null;
    
    public Neo4jGraphBuilder(String dbPath)
    {	     	  	
    	relationTypeList = new ArrayList();
    	graphDb = new GraphDatabaseFactory().newEmbeddedDatabase( new File(dbPath) );
        registerShutdownHook();
    }
    
    public void createDb(String leftName, String rightName, String leftType,
    		String rightType, String relation)
    {
    	createChain(leftName, rightName, leftType, rightType, relation);
    }
    
    private static void createChain( String leftName, String rightName, String leftType, 
    		String rightType, String relation)
    {
        Node firstNode = null;
        Node secondNode = null; 
        
        firstNode = getOrCreateNode( leftName, leftType);    	
        secondNode = getOrCreateNode( rightName, rightType);
        //노드의 Name, Type Property 출력 가능	
//        System.out.println(firstNode.toString() + "::" + firstNode.getProperty(NAME_KEY)  + ";;" + firstNode.getProperty(TYPE_KEY) + " :: " + relation + " :: " + secondNode.toString() + "::" + secondNode.getProperty(NAME_KEY));  
        
        RelationshipType rel_type = DynamicRelationshipType.withName( relation );
        Relationship relationship = firstNode.createRelationshipTo( secondNode, rel_type ); 

    }

    private static Node getOrCreateNode( String name, String type)
    {
        Node node = indexService.get( NAME_KEY, name ).getSingle();
        if ( node == null )
        {
            node = graphDb.createNode();
            node.setProperty( NAME_KEY, name );
            node.setProperty( TYPE_KEY, type );
            indexService.add( node, NAME_KEY, name );
        }
        return node;
    }

    public void readData(String inputFileName, boolean cooccur_included,ArrayList <String> stopwords) throws Exception
	{	
		System.out.println(" file " + inputFileName);
//		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(cypherFile), "UTF-8"));

		Pattern r = Pattern.compile(pattern);

		int missing_count = 0;
		HashMap<String,ArrayList<Pair<String,String>>> coEntityMap = new HashMap();
		HashMap<String,ArrayList<Pair<String,String>>> relEntityMap = new HashMap();
	    Scanner in = new Scanner(new FileReader(new File(inputFileName)));
	    int found_count = 0;
	    try ( Transaction tx = graphDb.beginTx() )
        {
	    	indexService = graphDb.index().forNodes( "nodes" );
	    	
		    while (in.hasNext()) { 
		        String line = in.nextLine();
		        
			    String[] tokens = line.split("\\|");

		        if (tokens.length < 10) {
		        	missing_count++;
		        	continue;
		        }
		        
		        String id = tokens[0];
		        String entity1 = tokens[3].toLowerCase();
		        String entity1_type = tokens[4];
		        
		        String entity2 = tokens[6].toLowerCase();
		        String entity2_type = tokens[7];
		        
		        if (entity1.length() <4 || entity2.length() <4)
		        	continue;
		        
		        if (stopwords.contains(entity1) || stopwords.contains(entity2))
					continue;

		        if (!relEntityMap.containsKey(id)) {
		        	ArrayList<Pair<String,String>> list = new ArrayList();
		        	list.add(Pair.of(entity1, entity2));
		        	relEntityMap.put(id, list);
		        } else {
		        	relEntityMap.get(id).add(Pair.of(entity1, entity2));
		        }
		        
		        if (!coEntityMap.containsKey(id)) {
		        	ArrayList<Pair<String,String>> list = new ArrayList();
		        	list.add(Pair.of(entity1, entity1_type));
		        	list.add(Pair.of(entity2, entity2_type));
		        	coEntityMap.put(id, list);
		        } else{
		        	Pair pair1 = Pair.of(entity1, entity1_type);
		        	if (!coEntityMap.get(id).contains(pair1)) {
		        		coEntityMap.get(id).add(pair1);
		        	}
		        	Pair pair2 = Pair.of(entity2, entity2_type);
		        	if (!coEntityMap.get(id).contains(pair2)) {
		        		coEntityMap.get(id).add(pair2);
		        	}
		        }
		        
		        String relation = tokens[11];
		         	
		        //store relation type for search to be used later on
				if (!relationTypeList.contains(relation)) {
					relationTypeList.add(relation);
				}

					
				//create DB
				createDb(entity1, entity2, entity1_type, entity2_type, relation);
		        System.out.println(entity1 + " :: " + relation + " :: " + entity2);
//		        bw.write(" (" + entity1  + ")-->[" + relation + "]-->(" + entity2 + ")\n");	
		        found_count++;
		    }

		    if (cooccur_included) {
			    for (Map.Entry<String, ArrayList<Pair<String,String>>> ent : coEntityMap.entrySet()) {
			    	String id = ent.getKey();
			    	boolean contained = relEntityMap.containsKey(id);
			    	
			    	ArrayList<Pair<String,String>> list = ent.getValue();
			    	for (int i = 0; i < list.size(); ++i) {
			    		Pair<String,String> pair1 = list.get(i);
			    		for (int j = i+1; j < list.size(); ++j) {
				    		Pair<String,String> pair2 = list.get(j);
				    		
				    		if (!pair1.getLeft().equals(pair2.getLeft()) && contained) {
				    			if (!relEntityMap.get(id).contains(Pair.of(pair1.getLeft(),pair2.getRight()))) {
				    				createDb(pair1.getLeft(), pair2.getLeft(), pair1.getRight(), pair2.getRight(), "CO_OCCUR");
				    			}
				    		}
			    		}
			    	}
			    }	    
			    relationTypeList.add("CO_OCCUR");
		    }
		    
		    
		    in.close(); // don't forget to close resource leaks
		    tx.success();
		    
		    System.out.println("Relation Count: " + found_count + " :: " + missing_count);
		    
        }
	}
	
	
	
    public static void registerShutdownHook()
    {
        // Registers a shutdown hook for the Neo4j instance so that it
        // shuts down nicely when the VM exits (even if you "Ctrl-C" the
        // running example before it's completed)
        Runtime.getRuntime().addShutdownHook( new Thread()
        {
            @Override
            public void run()
            {
                graphDb.shutdown();
            }
        } );
    }
    
    public void shutDown()
	{
	    graphDb.shutdown();
	    System.out.println("graphDB shut down.");   
	}   
    
    public void writeRelationType(String relationFile)
	{
		try {
			Writer writer = new BufferedWriter(new OutputStreamWriter(
		         new FileOutputStream(relationFile), "utf-8"));
			
			for (int i = 0; i < relationTypeList.size(); ++i) {
				if (i != relationTypeList.size()-1) {
					writer.write(relationTypeList.get(i) + "\n");
				} else {
					writer.write(relationTypeList.get(i));
				}
			}
			
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
    
    public static void main(String[] args) throws Exception
    {
    	String db_path = "./neo4j_db";
    	String inputFileName = "./relation_sample.txt";
    	String relationFile = "./relation_types.txt";
    	String stopwordFile = "./common_word_list.txt";
    	ArrayList<String> stopwords = new ArrayList<String>();
    	BufferedReader stop = new BufferedReader(new InputStreamReader(new FileInputStream(stopwordFile), "utf-8"));
    	for (String line = stop.readLine().toLowerCase(); line != null; line = stop.readLine()) {
    		stopwords.add(line.trim());
    	}
    	stop.close();

    	
    	boolean cooccur_included = false;
    	Neo4jGraphBuilder builder = new Neo4jGraphBuilder(db_path);
    	builder.readData(inputFileName, cooccur_included,stopwords);
    	
    	builder.writeRelationType(relationFile);
    	builder.shutDown();
    }
}
