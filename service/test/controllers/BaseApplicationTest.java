package controllers;


import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import modules.OnRequestHandler;
import modules.StartModule;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import play.Application;
import play.Mode;
import play.inject.guice.GuiceApplicationBuilder;
import play.test.Helpers;
import util.RequestInterceptor;

import java.io.File;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.management.*")
@PrepareForTest({RequestInterceptor.class})
public abstract class BaseApplicationTest {
    protected Application application;
    private ActorSystem system;
    private Props props;

    public <T> void setup(Class<T> actorClass){
        try {
        application =
                new GuiceApplicationBuilder()
                        .in(new File("path/to/app"))
                        .in(Mode.TEST)
                        .disable(StartModule.class)
                        .build();
        Helpers.start(application);
        system = ActorSystem.create("system");
        props = Props.create(actorClass);
        ActorRef subject = system.actorOf(props);
        BaseController.setActorRef(subject);
        PowerMockito.mockStatic(RequestInterceptor.class);
        PowerMockito.when(RequestInterceptor.verifyRequestData(Mockito.any())).thenReturn("userId");
        PowerMockito.mockStatic(OnRequestHandler.class);
        PowerMockito.doReturn("12345678990").when(OnRequestHandler.class,"getCustodianOrgHashTagId");
        }catch (Exception e){
            e.printStackTrace();
        }
    }

}