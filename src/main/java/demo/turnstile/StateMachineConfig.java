package demo.turnstile;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachine;
import org.springframework.statemachine.config.EnumStateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineConfigurationConfigurer;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import org.springframework.statemachine.listener.StateMachineListener;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.state.State;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.ContainerInfo;
import com.spotify.docker.client.messages.HostConfig;
import com.spotify.docker.client.messages.PortBinding;

import java.util.*;

@Configuration
@EnableStateMachine
public class StateMachineConfig extends EnumStateMachineConfigurerAdapter<States, Events> {
	
	private static DockerClient docker; 
	private static  String id;

    @Override
    public void configure(StateMachineConfigurationConfigurer<States, Events> config) throws Exception {
		config
			.withConfiguration()
			.autoStartup(true)
			.listener(listener());
    }

    @Bean
    public StateMachineListener<States, Events> listener() {
        return new StateMachineListenerAdapter<States, Events>() {
            @Override
            public void stateChanged(State<States, Events> from, State<States, Events> to) {
            	try{
				if(to.getId()==States.OPEN)
            	{
            		docker = DefaultDockerClient.fromEnv().build();
					System.out.println("downloding busybox");
            		// Pull an image
            		docker.pull("busybox");
 					System.out.println("downloded busybox");           		
            		final String[] ports = {"80", "22"};
            		final Map<String, List<PortBinding>> portBindings = new HashMap<String, List<PortBinding>>();
            		for (String port : ports) {
            		    List<PortBinding> hostPorts = new ArrayList<PortBinding>();
            		    hostPorts.add(PortBinding.of("0.0.0.0", port));
            		    portBindings.put(port, hostPorts);
            		}
 					System.out.println("PortBinding busybox");   
            		// Bind container port 443 to an automatically allocated available host port.
            		List<PortBinding> randomPort = new ArrayList<PortBinding>();
            		randomPort.add(PortBinding.randomPort("0.0.0.0"));
            		portBindings.put("443", randomPort);
            		
            		final HostConfig hostConfig = HostConfig.builder().portBindings(portBindings).build();
 					System.out.println("hostConfig busybox");   
            		// Create container with exposed ports
            		final ContainerConfig containerConfig = ContainerConfig.builder()
            		    .hostConfig(hostConfig)
            		    .image("busybox").exposedPorts(ports)
            		    .cmd("sh", "-c", "while :; do sleep 1; done")
            		    .build();
 					System.out.println("ContainerConfig busybox") ; 
            		final ContainerCreation creation = docker.createContainer(containerConfig);
            		id = creation.id();
					System.out.println("id "+id);
            		// Inspect container
            		final ContainerInfo info = docker.inspectContainer(id);
 					System.out.println("ContainerInfo busybox"); 
            		// Start container
            		docker.startContainer(id);
            	}
            	if(to.getId()==States.CLOSE)
            	{
					if (id !=null )
					{
						docker.killContainer(id);
						// Remove container
						docker.removeContainer(id);
						// Close the docker client
						docker.close();
					}

            	}
            	}
            	catch(Exception e)
            	{
            		e.printStackTrace();
            	}
            	
                System.out.println("State change to " + to.getId());
            }
        };
    }

    @Override
    public void configure(StateMachineStateConfigurer<States, Events> states) throws Exception {
		states
			.withStates()
			.initial(States.CLOSE)
			.states(EnumSet.allOf(States.class));
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<States, Events> transitions) throws Exception {
		transitions
			.withExternal()
			.source(States.OPEN).target(States.CLOSE).event(Events.UNLOCK)
			.and()
			.withExternal()
			.source(States.CLOSE).target(States.OPEN).event(Events.LOCK);
    }

}