package fu.de190025.reservation;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
@SpringBootApplication
@EnableFeignClients
public class DE190025ReservationServiceApplication {
 public static void main(String[] args) { SpringApplication.run(DE190025ReservationServiceApplication.class,args); }
}
