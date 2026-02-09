package com.litsite.lit;

import static org.assertj.core.api.Assertions.assertThat;

import com.litsite.lit.models.MyUser;
import com.litsite.lit.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void testSaveAndFind() {
        MyUser user = new MyUser("Jipip", "123", "Жипип", "jipip2004@mail.ru", LocalDateTime.now(), "Привет! Я Жипип (реальное имя)");

        MyUser saved = userRepository.save(user);
        assertThat(saved.getUserId()).isNotNull();

        MyUser found = userRepository.findById(Integer.valueOf(saved.getUserId())).orElse(null);
        assertThat(found).isNotNull();
        assertThat(found.getUsername()).isEqualTo("Жипип");
        assertThat(found.getEmail()).isEqualTo("jipip2004@mail.ru");
    }
}
