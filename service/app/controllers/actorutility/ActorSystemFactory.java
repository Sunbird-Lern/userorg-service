package controllers.actorutility;

import controllers.actorutility.impl.LocalActorSystem;
import controllers.actorutility.impl.RemoteActorSystem;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.PropertiesCache;

/**
 * 
 * @author Amit Kumar
 *
 */
public class ActorSystemFactory {
  
  private static RemoteActorSystem remoteActorSystem = null;
  private static LocalActorSystem localActorSystem = null;
  
  private ActorSystemFactory(){}
  
  public static ActorSystem getActorSystem(){
    PropertiesCache cache = PropertiesCache.getInstance();
    if("local".equalsIgnoreCase(cache.getProperty("api_actor_provider"))){
      ProjectLogger.log("Initializing Local Actor System");
      if(null == localActorSystem){
        localActorSystem = new LocalActorSystem();
        return localActorSystem ;
      }else{
        return localActorSystem;
      }
    }else{
      ProjectLogger.log("Initializing Remote Actor System");
      if(null == remoteActorSystem){
        remoteActorSystem = new RemoteActorSystem();
        return remoteActorSystem ;
      }else{
        return remoteActorSystem;
      }
    }
  }
}
