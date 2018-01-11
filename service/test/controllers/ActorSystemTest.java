package controllers;

import static org.junit.Assert.assertTrue;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import controllers.actorutility.ActorSystemFactory;
import controllers.actorutility.impl.LocalActorSystem;
import controllers.actorutility.impl.RemoteActorSystem;
import org.junit.BeforeClass;
import org.sunbird.common.models.util.PropertiesCache;

public class ActorSystemTest {
  
  static String provider = null;
      
  @BeforeClass
  public static void setUp() {
    
    provider = PropertiesCache.getInstance().getProperty("api_actor_provider");
  }
  
  //@Test
  public void testActorSystem(){
    Object obj = ActorSystemFactory.getActorSystem();
     if(provider.equalsIgnoreCase("local")){
       assertTrue(obj instanceof LocalActorSystem);
     } else {
       assertTrue(obj instanceof RemoteActorSystem);
     }
  }
  
  //@Test
  public void testActorRef(){
    Object obj = ActorSystemFactory.getActorSystem().initializeActorSystem();
     if(provider.equalsIgnoreCase("local")){
    	 assertTrue(obj instanceof ActorRef);
     } else {
    	 assertTrue(obj instanceof ActorSelection);
     }
  }

}
