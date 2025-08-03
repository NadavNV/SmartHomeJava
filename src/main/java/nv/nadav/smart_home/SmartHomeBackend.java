package nv.nadav.smart_home;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties
public class SmartHomeBackend {

	public static void main(String[] args) {
		Dotenv.configure().ignoreIfMissing().load();
		SpringApplication.run(SmartHomeBackend.class, args);
	}

}
