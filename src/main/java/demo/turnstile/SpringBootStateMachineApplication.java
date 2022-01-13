package demo.turnstile;


import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.statemachine.StateMachine;

@SpringBootApplication
public class SpringBootStateMachineApplication implements CommandLineRunner {

    private final StateMachine<States, Events> stateMachine;

    public SpringBootStateMachineApplication(StateMachine<States, Events> stateMachine) {
        this.stateMachine = stateMachine;
    }

    public static void main(String[] args) {
        SpringApplication.run(SpringBootStateMachineApplication.class, args);
    }

    @Override
    public void run(String... args) {
		while(true) {		
		long seconds = System.currentTimeMillis() / 1000l;
		if(seconds% 234 == 0)
			stateMachine.sendEvent(Events.LOCK);
		if(seconds% 111 == 0)
			stateMachine.sendEvent(Events.UNLOCK);
		}
    }

}