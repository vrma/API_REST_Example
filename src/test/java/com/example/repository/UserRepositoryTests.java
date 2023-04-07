package com.example.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.example.user.Role;
import com.example.user.User;
import com.example.user.UserRepository;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
public class UserRepositoryTests {

    @Autowired
    private UserRepository userRepository;

    private User user0;

    @BeforeEach
    void setup() {
        user0 = User.builder()
            .firstName("Primer Usuario")
            .lastName("Primer Apellido")
            .email("u1@gmail.com")
            .password("123456")
            .role(Role.USER)
            .build();
    }

    @DisplayName("Test para agregar un user")
    @Test
    void testAddUser() {
        // given - dado que:
        User user1 = User.builder()
                .firstName("Test User 1")
                .lastName("Machado")
                .password("1234")
                .email("user1@gmail.com")
                .role(Role.USER)
                .build();

        // when
        User userAdded = userRepository.save(user1);

        // then
        assertThat(userAdded).isNotNull();
        assertThat(userAdded.getId()).isGreaterThan(0);
    }

    @DisplayName("Test para listar usuarios")
    @Test
    void textFindAllUsers() {
        // given 

        User user1 = User.builder()
                .firstName("Test User 1")
                .lastName("Machado")
                .password("1234")
                .email("user1@gmail.com")
                .role(Role.USER)
                .build();

        User user2 = User.builder()
                .firstName("Test User 1")
                .lastName("Machado")
                .password("1234")
                .email("user2@gmail.com")
                .role(Role.USER)
                .build();

        userRepository.save(user0);
        userRepository.save(user1);
        userRepository.save(user2);

        // when

        List<User> usuarios = userRepository.findAll();

        // then

        assertThat(usuarios).isNotNull();
        assertThat(usuarios.size()).isEqualTo(6);

    }

    @DisplayName("Test para recuperar un usuario por su id")
    @Test
    void findUserById() {
        // given

        userRepository.save(user0);

        // when

        User user = userRepository.findById(user0.getId()).get();

        // then

        assertThat(user.getId()).isNotEqualTo(0L);

    }

    @DisplayName("Test para actualizar un user")
    @Test
    void updateUser () {

        // given

        userRepository.save(user0);

        // when

        User userGuardado = userRepository.findByEmail(user0.getEmail()).get();

        userGuardado.setLastName("Perico");
        userGuardado.setFirstName("Perez");
        userGuardado.setEmail("pp@gmail.com");

        User userUpdated = userRepository.save(userGuardado);

        // then

        assertThat(userUpdated.getEmail()).isEqualTo("pp@gmail.com");
        assertThat(userUpdated.getFirstName()).isEqualTo("Perez");  

    }

    @DisplayName("Test para eliminar un user")
    @Test
    void testDeleteUser() {

        // given
        userRepository.save(user0);

        // when
        userRepository.delete(user0);
        Optional<User> optionalUser = userRepository.findByEmail(user0.getEmail());

        // then 
        assertThat(optionalUser).isEmpty();

    }

    

}
