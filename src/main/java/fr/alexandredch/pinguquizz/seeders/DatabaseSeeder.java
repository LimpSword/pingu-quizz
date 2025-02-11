package fr.alexandredch.pinguquizz.seeders;

import fr.alexandredch.pinguquizz.models.User;
import fr.alexandredch.pinguquizz.repositories.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

@Configuration
public class DatabaseSeeder {

    @Bean
    public CommandLineRunner seedDatabase(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return _ -> {
            if (userRepository.findByUsername("admin").isEmpty()) {
                User user = new User();
                user.setUsername("admin");
                user.setPassword(passwordEncoder.encode("admin123"));
                user.setRoles(Set.of("ADMIN"));

                userRepository.save(user);
                System.out.println("Seeded admin user with hashed password.");
            } else {
                System.out.println("Admin user already exists. Skipping seeding.");
            }
        };
    }
}