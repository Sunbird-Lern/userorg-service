import akka.actor.UntypedAbstractActor;

public class MockActor extends UntypedAbstractActor{

	@Override
	public void onReceive(Object arg0) throws Throwable {
        sender().tell("test", getSelf());		
	}

}
