package si.confreg.registration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Entry point of the conference registration backend. */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class RegistrationApplication {

  public static void main(String[] args) {
    SpringApplication.run(RegistrationApplication.class, args);
  }
}
