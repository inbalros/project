/**
 * 
 */
package org.bgu.ise.ddb.items;

import java.io.IOException;







import javax.servlet.http.HttpServletResponse;

import org.bgu.ise.ddb.MediaItems;
import org.bgu.ise.ddb.ParentController;
import org.bgu.ise.ddb.User;
import org.bson.Document;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;







import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import oracle.jdbc.OracleTypes;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * @author Alex
 *
 */
@RestController
@RequestMapping(value = "/items")
public class ItemsController extends ParentController {
	
	 private void fileToOracleDataBase(){
		 try {
			 	String path = "C:\\Users\\USER\\Documents\\films.csv";
	            Class.forName("oracle.jdbc.driver.OracleDriver"); //registration of the driver
	            Connection conn = DriverManager.getConnection("jdbc:oracle:thin:@ora1.ise.bgu.ac.il:1521/oracle","asafwlo", "abcd");//connection
	            conn.setAutoCommit(false);
	            
	            PreparedStatement ps = null;

		        BufferedReader br = null;
		        String line = "";
		        String cvsSplitBy = ",";
		        String query;
		        
		        
		        try {

		            br = new BufferedReader(new FileReader(path));
		            while ((line = br.readLine()) != null) {

		                String[] movie = line.split(cvsSplitBy);
		                query = "INSERT INTO MEDIAITEMS(TITLE, PROD_YEAR) VALUES (?,?)";
		                ps = conn.prepareStatement(query); //compiling query in the DB
		                ps.setString(1,movie[0]);
		                ps.setInt(2, Integer.parseInt(movie[1]));
		                ps.executeUpdate();
		                conn.commit();
		            }
		            ps.close();

		        } catch (FileNotFoundException e) {
		            e.printStackTrace();
		        } catch (IOException e) {
		            e.printStackTrace();
		        } catch (SQLException e) {
		            e.printStackTrace();
		        } catch (NumberFormatException e) {
		            e.printStackTrace();
		        }
		        finally {
		            if (br != null) {
		                try {
		                    br.close();
		                } catch (IOException e) {
		                    e.printStackTrace();
		                }
		            }
		        }
	           
	            conn.close();
		 
		 }catch (Exception e) {
	            e.printStackTrace();
	        }
   
	    }
	
	/**
	 * The function copy all the items(title and production year) from the Oracle table MediaItems to the System storage.
	 * The Oracle table and data should be used from the previous assignment
	 */
	@RequestMapping(value = "fill_media_items", method={RequestMethod.GET})
	public void fillMediaItems(HttpServletResponse response){
		//fileToOracleDataBase();
		PreparedStatement ps = null;
		MongoClient mongo  = null;
		Connection conn = null;
		try {
        Class.forName("oracle.jdbc.driver.OracleDriver"); //registration of the driver
        conn = DriverManager.getConnection("jdbc:oracle:thin:@ora1.ise.bgu.ac.il:1521/oracle","asafwlo", "abcd");//connection
        conn.setAutoCommit(false);
        
        	String selectFromMI = "select * from MediaItems MI";
            ps = conn.prepareStatement(selectFromMI);
            ResultSet rs = ps.executeQuery();

            mongo = new MongoClient( "localhost" , 27017 ); 
		    MongoDatabase database = mongo.getDatabase("InbalAndAsaf"); 
		    MongoCollection<Document> collection = database.getCollection("MEDIA_ITEMS"); 
            while (rs.next()) {
               String title = (rs.getString("TITLE"));
               int year = (rs.getInt("PROD_YEAR"));
               if(!isExistItem(title))
               {
			      Document document = new Document() 
			      .append("TITLE", title) 
			      .append("PROD_YEAR", year);
			      collection.insertOne(document);
               }
            }
            HttpStatus status = HttpStatus.OK;
    		response.setStatus(status.value());
		}
        catch (Exception e) {
            e.printStackTrace();
            HttpStatus status = HttpStatus.CONFLICT;
    		response.setStatus(status.value());
        }
		finally{
			if(mongo!=null)
				mongo.close();
			if(ps!=null)
				try {
					ps.close();
				} catch (SQLException e) {
					e.printStackTrace();
				} 
			if(conn!=null)
				try {
					conn.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
		}
	}

	/**
	 * The function copy all the items from the remote file,
	 * the remote file have the same structure as the films file from the previous assignment.
	 * You can assume that the address protocol is http
	 * @throws IOException 
	 */
	@RequestMapping(value = "fill_media_items_from_url", method={RequestMethod.GET})
	public void fillMediaItemsFromUrl(@RequestParam("url")    String urladdress,
			HttpServletResponse response) throws IOException{
		System.out.println(urladdress);
		
		URL url = new URL(urladdress);
		BufferedReader br = null;
        String line = "";
        String cvsSplitBy = ",";
        MongoClient mongo = null;
        try {
        	mongo = new MongoClient( "localhost" , 27017 ); 
		    MongoDatabase database = mongo.getDatabase("InbalAndAsaf"); 
		    MongoCollection<Document> collection = database.getCollection("MEDIA_ITEMS"); 
            br = new BufferedReader(new InputStreamReader(url.openStream()));
            while ((line = br.readLine()) != null) {
                
            	String[] movie = line.split(cvsSplitBy);
                
                if(!isExistItem(movie[0]))
                {
                	 Document document = new Document() 
         			.append("TITLE", movie[0]) 
         			.append("PROD_YEAR", Integer.parseInt(movie[1]));
         			collection.insertOne(document); 
                }
              }
            HttpStatus status = HttpStatus.OK;
    		response.setStatus(status.value());
    		
        }catch(Exception e){
        	e.printStackTrace();
        	HttpStatus status = HttpStatus.CONFLICT;
    		response.setStatus(status.value());
        }
        finally {
        	if(mongo!=null)
        		mongo.close();
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
		
	}
	
	public boolean isExistItem(@RequestParam("movieTitle") String movieTitle) throws IOException{

		boolean result = false;
		
		MongoClient mongo = null;
		
		try{
		mongo = new MongoClient( "localhost" , 27017 ); 
	    MongoDatabase database = mongo.getDatabase("InbalAndAsaf"); 
	    MongoCollection<Document> collection = database.getCollection("MEDIA_ITEMS");
	    BasicDBObject search = new BasicDBObject();
	    search.put("TITLE", movieTitle);
	    FindIterable<Document> iterDoc = collection.find(search);
        Iterator<Document> it = iterDoc.iterator(); 
	    
	    if (it.hasNext()) { 
	    	result = true;
	     }
	    
		}
		catch(Exception e)
		{
			 e.printStackTrace();
		}
		finally{
			if(mongo!=null)
			     mongo.close();
		}
		return result;
	}
	
	/**
	 * The function retrieves from the system storage N items,
	 * order is not important( any N items) 
	 * @param topN - how many items to retrieve
	 * @return
	 */
	@RequestMapping(value = "get_topn_items",headers="Accept=*/*", method={RequestMethod.GET},produces="application/json")
	@ResponseBody
	@org.codehaus.jackson.map.annotate.JsonView(MediaItems.class)
	public  MediaItems[] getTopNItems(@RequestParam("topn")    int topN){
		
		if(topN<1)
		{
			return new MediaItems[0];
		}
		MongoClient mongo = null;
        List<MediaItems> mediaItemsList = new ArrayList<MediaItems>();

		try
		{
		mongo = new MongoClient( "localhost" , 27017 ); 
	    MongoDatabase database = mongo.getDatabase("InbalAndAsaf"); 
	    MongoCollection<Document> collection = database.getCollection("MEDIA_ITEMS");
	    FindIterable<Document> iterDoc = collection.find().limit(topN);
        Iterator<Document> it = iterDoc.iterator(); 
	    
	    while (it.hasNext()) { 
	    	Document d = it.next();
	    	mediaItemsList.add(new MediaItems(d.getString("TITLE"),d.getInteger("PROD_YEAR")));
	     }
		}
		catch(Exception e)
		{
			 e.printStackTrace();
		}
		finally{
			if(mongo!=null)
			     mongo.close();
		}
				
	     MediaItems[] mediaItems = new MediaItems[mediaItemsList.size()];
	     mediaItems = mediaItemsList.toArray(mediaItems);
	
		return  mediaItems;
	}
		

}
