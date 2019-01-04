/**
 * 
 */
package org.bgu.ise.ddb.history;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

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
import com.mongodb.operation.OrderBy;

import org.bgu.ise.ddb.registration.RegistarationController;
import org.bgu.ise.ddb.items.ItemsController;
/**
 * @author Alex
 *
 */
@RestController
@RequestMapping(value = "/history")
public class HistoryController extends ParentController{
	
	
	/**
	 * The function inserts to the system storage triple(s)(username, title, timestamp). 
	 * The timestamp - in ms since 1970
	 * Advice: better to insert the history into two structures( tables) in order to extract it fast one with the key - username, another with the key - title
	 * @param username
	 * @param title
	 * @param response
	 */
	@RequestMapping(value = "insert_to_history", method={RequestMethod.GET})
	public void insertToHistory (@RequestParam("username")    String username,
			@RequestParam("title")   String title,
			HttpServletResponse response){
		
		RegistarationController userC = new RegistarationController();
		ItemsController itemC = new ItemsController();
		MongoClient mongo = null;
		try {
			if(!userC.isExistUser(username) || !itemC.isExistItem(title))
			{
				HttpStatus status = HttpStatus.CONFLICT;
				response.setStatus(status.value());
			}
			else
			{
			      mongo = new MongoClient( "localhost" , 27017 ); 
			      MongoDatabase database = mongo.getDatabase("InbalAndAsaf"); 
			      MongoCollection<Document> collection = database.getCollection("HISTORY"); 
			      Document document = new Document() 
			      .append("USERNAME", username) 
			      .append("TITLE", title) 
			      .append("TIME_STAMP", (new Timestamp(System.currentTimeMillis())).getTime());
			      collection.insertOne(document); 
				HttpStatus status = HttpStatus.OK;
				response.setStatus(status.value());
			}
		} catch (Exception e) {
			e.printStackTrace();
			HttpStatus status = HttpStatus.CONFLICT;
			response.setStatus(status.value());
		}
		finally{
			if(mongo!=null)
			{
				  mongo.close();
			}
		}
	}
	
	
	/**
	 * The function retrieves  users' history
	 * The function return array of pairs <title,viewtime> sorted by VIEWTIME in descending order
	 * @param username
	 * @return
	 */
	@RequestMapping(value = "get_history_by_users",headers="Accept=*/*", method={RequestMethod.GET},produces="application/json")
	@ResponseBody
	@org.codehaus.jackson.map.annotate.JsonView(HistoryPair.class)
	public  HistoryPair[] getHistoryByUser(@RequestParam("entity")    String username){
		
        List<HistoryPair> historyPairList = new ArrayList<HistoryPair>();
		MongoClient mongo = null;
		try{
		mongo =new MongoClient( "localhost" , 27017 ); 
	    MongoDatabase database = mongo.getDatabase("InbalAndAsaf"); 
	    MongoCollection<Document> collection = database.getCollection("HISTORY");
	    BasicDBObject search = new BasicDBObject();
	    search.put("USERNAME", username);
	    FindIterable<Document> iterDoc = collection.find(search).sort(new BasicDBObject("TIME_STAMP", OrderBy.DESC.getIntRepresentation()));
        Iterator<Document> it = iterDoc.iterator(); 
	            
        while (it.hasNext()) { 
	    	Document d = it.next();
	    	historyPairList.add(new HistoryPair(d.getString("TITLE"),new Date((long)(d.get("TIME_STAMP")))));
	     }
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally{
			if(mongo!=null)
			{
				  mongo.close();
			}
		}	
	     HistoryPair[] historyPairs = new HistoryPair[historyPairList.size()];
	     historyPairs = historyPairList.toArray(historyPairs);
	

		return historyPairs;
	}
	
	
	/**
	 * The function retrieves  items' history
	 * The function return array of pairs <username,viewtime> sorted by VIEWTIME in descending order
	 * @param title
	 * @return
	 */
	@RequestMapping(value = "get_history_by_items",headers="Accept=*/*", method={RequestMethod.GET},produces="application/json")
	@ResponseBody
	@org.codehaus.jackson.map.annotate.JsonView(HistoryPair.class)
	public  HistoryPair[] getHistoryByItems(@RequestParam("entity")    String title){
        
		List<HistoryPair> historyPairList = new ArrayList<HistoryPair>();
		MongoClient mongo =null;
		
		try{
		mongo = new MongoClient( "localhost" , 27017 ); 
	    MongoDatabase database = mongo.getDatabase("InbalAndAsaf"); 
	    MongoCollection<Document> collection = database.getCollection("HISTORY");
	    BasicDBObject search = new BasicDBObject();
	    search.put("TITLE", title);
	    FindIterable<Document> iterDoc = collection.find(search).sort(new BasicDBObject("TIME_STAMP", OrderBy.DESC.getIntRepresentation()));
        Iterator<Document> it = iterDoc.iterator(); 
	            
        while (it.hasNext()) { 
	    	Document d = it.next();
	    	historyPairList.add(new HistoryPair(d.getString("USERNAME"),new Date((long)(d.get("TIME_STAMP")))));
	     }
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally{
			if(mongo!=null)
			{
				  mongo.close();
			}
		}
			
	     HistoryPair[] historyPairs = new HistoryPair[historyPairList.size()];
	     historyPairs = historyPairList.toArray(historyPairs);
	

		return historyPairs;
		
	}
	
	/**
	 * The function retrieves all the  users that have viewed the given item
	 * @param title
	 * @return
	 */
	@RequestMapping(value = "get_users_by_item",headers="Accept=*/*", method={RequestMethod.GET},produces="application/json")
	@ResponseBody
	@org.codehaus.jackson.map.annotate.JsonView(HistoryPair.class)
	public  User[] getUsersByItem(@RequestParam("title") String title){
		
		RegistarationController userc = new RegistarationController();
		
		MongoClient mongo = null;
        List<String> usersNames = new ArrayList<String>();
        List<User> currUsersList = new ArrayList<User>();

		try{
			
	    User[] allUsers  = userc.getAllUsers();
		
	    mongo= new MongoClient( "localhost" , 27017 ); 
	    MongoDatabase database = mongo.getDatabase("InbalAndAsaf"); 
	    MongoCollection<Document> collection = database.getCollection("HISTORY");
	    BasicDBObject search = new BasicDBObject();
	    search.put("TITLE", title);
	    FindIterable<Document> iterDoc = collection.find(search);
        Iterator<Document> it = iterDoc.iterator(); 
		        
        while (it.hasNext()) { 
	    	Document d = it.next();
	    	usersNames.add(d.getString("USERNAME"));
	     }
					
		for (int i = 0; i < usersNames.size(); i++) {
			String currUserName = usersNames.get(i);
			boolean found = false;
			for(int j=0;j<allUsers.length;j++)
			{
				if(currUserName.equals(allUsers[j].getUsername()) && !found)
				{
					currUsersList.add(allUsers[j]);
					found = true;
				}
			}
		}
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally{
			if(mongo!=null)
			{
				  mongo.close();
			}
		}
		User[] Users = new User[currUsersList.size()];
		Users = currUsersList.toArray(Users);
		
		return Users;
	}
	
	/**
	 * The function calculates the similarity score using Jaccard similarity function:
	 *  sim(i,j) = |U(i) intersection U(j)|/|U(i) union U(j)|,
	 *  where U(i) is the set of usernames which exist in the history of the item i.
	 * @param title1
	 * @param title2
	 * @return
	 */
	@RequestMapping(value = "get_items_similarity",headers="Accept=*/*", method={RequestMethod.GET},produces="application/json")
	@ResponseBody
	public double  getItemsSimilarity(@RequestParam("title1") String title1,
			@RequestParam("title2") String title2){

		double ret = 0.0;
		List<String> usersNamesIntersection = new ArrayList<String>();
        List<String> currUsersListunion = new ArrayList<String>();
		try{
		User[] Users1 = getUsersByItem(title1);
		User[] Users2 = getUsersByItem(title2);
	
        for(int i = 0; i<Users1.length;i++)
        {
        	User user1 = Users1[i];
        	currUsersListunion.add(user1.getUsername());
        	for(int j = 0; j<Users2.length;j++)
            {
        		User user2 = Users2[j];		
            	if(user1.getUsername().equals(user2.getUsername()))
            	{
            		usersNamesIntersection.add(user1.getUsername());
            	}
            }	
        }
		
        for(int j = 0; j<Users2.length;j++)
        {
    		User user2 = Users2[j];		
        	if(!currUsersListunion.contains(user2.getUsername()))
        	{
        		currUsersListunion.add(user2.getUsername());
        	}
        }
		} catch (Exception e) {
			e.printStackTrace();
		}
        
        if(currUsersListunion.size()==0)
        {
        	return ret;
        }
		
        ret = ((double)(usersNamesIntersection.size()))/(currUsersListunion.size());
		
		return ret;
	}
	

}
