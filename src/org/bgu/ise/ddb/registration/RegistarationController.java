/**
 * 
 */
package org.bgu.ise.ddb.registration;


import java.sql.Timestamp;
import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.bgu.ise.ddb.ParentController;
import org.bgu.ise.ddb.User;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Calendar;
import java.util.Date;
import java.util.Iterator; 
import java.util.List;

import org.bson.Document; 

import java.util.ArrayList;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.ServerAddress;
import com.mongodb.MongoCredential;
import com.mongodb.MongoClientOptions;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.client.MongoDatabase; 
import com.mongodb.client.MongoCollection; 
import com.mongodb.client.FindIterable; 


/**
 * @author Alex
 *
 */
@RestController
@RequestMapping(value = "/registration")
public class RegistarationController extends ParentController{
	   
	/**
	 * The function checks if the username exist,
	 * in case of positive answer HttpStatus in HttpServletResponse should be set to HttpStatus.CONFLICT,
	 * else insert the user to the system  and set to HttpStatus in HttpServletResponse HttpStatus.OK
	 * @param username
	 * @param password
	 * @param firstName
	 * @param lastName
	 * @param response
	 */
	@RequestMapping(value = "register_new_customer", method={RequestMethod.POST})
	public void registerNewUser(@RequestParam("username") String username,
			@RequestParam("password")    String password,
			@RequestParam("firstName")   String firstName,
			@RequestParam("lastName")  String lastName,
			HttpServletResponse response){
		System.out.println(username+" "+password+" "+lastName+" "+firstName);
		
		MongoClient mongo =null;
		try {
			if(isExistUser(username))
			{
				HttpStatus status = HttpStatus.CONFLICT;
				response.setStatus(status.value());
			}
			else
			{
			      mongo = new MongoClient( "localhost" , 27017 ); 
			      MongoDatabase database = mongo.getDatabase("InbalAndAsaf"); 
			      MongoCollection<Document> collection = database.getCollection("USERS"); 
			      Document document = new Document() 
			      .append("USERNAME", username) 
			      .append("PASSWORD", password) 
			      .append("FIRST_NAME", firstName) 
			      .append("LAST_NAME", lastName)
			      .append("REGISTRATION_DATE", (new Timestamp(System.currentTimeMillis())));
			      collection.insertOne(document); 

				HttpStatus status = HttpStatus.OK;
				response.setStatus(status.value());
				
			}
		} catch (Exception e) {
			HttpStatus status = HttpStatus.CONFLICT;
			response.setStatus(status.value());
			e.printStackTrace();
		}
		finally{
			if(mongo!=null)
				{
				mongo.close();
				}
		}

	}
	
	/**
	 * The function returns true if the received username exist in the system otherwise false
	 * @param username
	 * @return
	 * @throws IOException
	 */
	@RequestMapping(value = "is_exist_user", method={RequestMethod.GET})
	public boolean isExistUser(@RequestParam("username") String username) throws IOException{
		System.out.println(username);
		boolean result = false;
		
		MongoClient mongo = null;
		try{
		mongo= new MongoClient( "localhost" , 27017 ); 
	    MongoDatabase database = mongo.getDatabase("InbalAndAsaf"); 
	    MongoCollection<Document> collection = database.getCollection("USERS");
	    BasicDBObject search = new BasicDBObject();
	    search.put("USERNAME", username);
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
			if(mongo != null)
				mongo.close();
		}
		return result;
	}
	
	/**
	 * The function returns true if the received username and password match a system storage entry, otherwise false
	 * @param username
	 * @return
	 * @throws IOException
	 */
	@RequestMapping(value = "validate_user", method={RequestMethod.POST})
	public boolean validateUser(@RequestParam("username") String username,
			@RequestParam("password")    String password) throws IOException{

		boolean result = false;
		
		MongoClient mongo= null;
		try{
		mongo = new MongoClient( "localhost" , 27017 ); 
	    MongoDatabase database = mongo.getDatabase("InbalAndAsaf"); 
	    MongoCollection<Document> collection = database.getCollection("USERS");
	    BasicDBObject search = new BasicDBObject();
	    search.put("USERNAME", username);
	    search.put("PASSWORD", password);
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
			if(mongo != null)
				mongo.close();
		}
	     
		return result;
		
	}
	
	/**
	 * The function retrieves number of the registered users in the past n days
	 * @param days
	 * @return
	 * @throws IOException
	 */
	@RequestMapping(value = "get_number_of_registred_users", method={RequestMethod.GET})
	public int getNumberOfRegistredUsers(@RequestParam("days") int days) throws IOException{
		System.out.println(days+"");
		int result = 0;
		MongoClient mongo = null;
		try{
		mongo = new MongoClient( "localhost" , 27017 ); 
	    MongoDatabase database = mongo.getDatabase("InbalAndAsaf"); 
	    MongoCollection<Document> collection = database.getCollection("USERS");
	    FindIterable<Document> iterDoc = collection.find();
        Iterator<Document> it = iterDoc.iterator(); 
	    
        Calendar c = Calendar.getInstance();
        System.out.println(c.getTime()); 
        c.set(Calendar.DATE, c.get(Calendar.DATE)-days);
        
        System.out.println(c.getTime()); 
        
	    while (it.hasNext()) { 
	    	Document d = it.next();
	    	Date t = (Date)d.get("REGISTRATION_DATE");
	    	if(t.getTime()>c.getTime().getTime())
	    	{
		    	result++;	    		
	    	}
	    }
		}catch(Exception e)
		{
			e.printStackTrace();
		}
		finally{
			if(mongo != null)
				mongo.close();
		}
		
		return result;
		
	}
	
	/**
	 * The function retrieves all the users
	 * @return
	 */
	@RequestMapping(value = "get_all_users",headers="Accept=*/*", method={RequestMethod.GET},produces="application/json")
	@ResponseBody
	@org.codehaus.jackson.map.annotate.JsonView(User.class)
	public  User[] getAllUsers(){
		User[] Users;
        List<User> usersList = new ArrayList<User>();
		MongoClient mongo = null;
		try{
		mongo = new MongoClient( "localhost" , 27017 ); 
	    MongoDatabase database = mongo.getDatabase("InbalAndAsaf"); 
	    MongoCollection<Document> collection = database.getCollection("USERS");
	    FindIterable<Document> iterDoc = collection.find();
        Iterator<Document> it = iterDoc.iterator(); 
	    

	    while (it.hasNext()) { 
	    	Document d = it.next();
	    	usersList.add(new User(d.getString("USERNAME"),d.getString("FIRST_NAME"),d.getString("LAST_NAME")));
	     }
	    
		}catch(Exception e)
		{
			e.printStackTrace();
		}
		finally{
			if(mongo != null)
				mongo.close();
		}
		
	     Users = new User[usersList.size()];
	     Users = usersList.toArray(Users);

		return Users;
	}

}




 
