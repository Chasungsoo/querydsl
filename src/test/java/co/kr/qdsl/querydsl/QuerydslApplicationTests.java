package co.kr.qdsl.querydsl;

import co.kr.qdsl.querydsl.entity.Hello;
import co.kr.qdsl.querydsl.entity.QHello;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class QuerydslApplicationTests {

  @Autowired
  EntityManager em;

  @Test
  void contextLoads() {
    Hello hello = new Hello();
    em.persist(hello);
    JPAQueryFactory query = new JPAQueryFactory(em);
    QHello qHello = QHello.hello;

    Hello hello1 = query
        .selectFrom(qHello)
        .fetchOne();

    assertThat(hello1).isEqualTo(hello);
    assertThat(hello1.getId()).isEqualTo(hello.getId());
  }

}
