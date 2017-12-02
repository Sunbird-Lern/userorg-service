import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import controllers.actorutility.ActorSystemFactory;
import controllers.actorutility.impl.LocalActorSystem;
import controllers.actorutility.impl.RemoteActorSystem;
import static org.junit.Assert.*;

import org.junit.BeforeClass;
import org.junit.Test;
import org.sunbird.common.models.util.PropertiesCache;

public class ActorSystemTest {
  
  static String provider = null;
      
  @BeforeClass
  public static void setUp() {
    
    provider = PropertiesCache.getInstance().getProperty("api_actor_provider");
  }
  
  @SuppressWarnings("deprecation")
  @Test
  public void testActorSystem(){
    Object obj = ActorSystemFactory.getActorSystem();
     if(provider.equalsIgnoreCase("local")){
       assertTrue(obj instanceof LocalActorSystem);
     } else {
       assertTrue(obj instanceof RemoteActorSystem);
     }
  }
  
  @SuppressWarnings("deprecation")
  @Test
  public void testActorRef(){
    Object obj = ActorSystemFactory.getActorSystem().initializeActorSystem();
     if(provider.equalsIgnoreCase("local")){
    	 assertTrue(obj instanceof ActorRef);
     } else {
    	 assertTrue(obj instanceof ActorSelection);
     }
  }

}
