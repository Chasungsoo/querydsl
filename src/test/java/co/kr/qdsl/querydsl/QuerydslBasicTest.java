package co.kr.qdsl.querydsl;

import co.kr.qdsl.querydsl.entity.Member;
import co.kr.qdsl.querydsl.entity.QMember;
import co.kr.qdsl.querydsl.entity.Team;
import com.querydsl.core.QueryResults;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

import java.util.List;

import static co.kr.qdsl.querydsl.entity.QMember.member;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

  @Autowired
  EntityManager em;
  JPAQueryFactory queryFactory;

  @BeforeEach
  public void testsetting() {
    queryFactory = new JPAQueryFactory(em);
    Team teamA = new Team("teamA");
    Team teamB = new Team("teamB");
    em.persist(teamA);
    em.persist(teamB);

    Member member1 = new Member("member1", 10, teamA);
    Member member2 = new Member("member2", 20, teamA);
    Member member3 = new Member("member3", 30, teamB);
    Member member4 = new Member("member4", 40, teamB);
    em.persist(member1);
    em.persist(member2);
    em.persist(member3);
    em.persist(member4);
  }

  @Test
  public void startJPQL() {
    String qlString =
        "select m from Member m " +
            "where m.username = :username";
    Member findMember = em.createQuery(qlString, Member.class)
        .setParameter("username", "member1")
        .getSingleResult();
    assertThat(findMember.getUsername()).isEqualTo("member1");
  }


  @Test
  public void startQueryDsl() {
    Member findMember = queryFactory.select(member)
        .from(member)
        .where(member.username.eq("member1"))
        .fetchOne();

    assertThat(findMember.getUsername()).isEqualTo("member1");
  }

  @Test
  public void search() {
    Member findMember = queryFactory
        .selectFrom(QMember.member)
        .where(
            QMember.member.username.eq("member1"),
            QMember.member.age.eq(10))
        .fetchOne();
    assertThat(findMember.getUsername()).isEqualTo("member1");
  }

  @Test
  public void resultFetch() {
    QueryResults<Member> memberQueryResults = queryFactory
        .selectFrom(member)
        .fetchResults();
    memberQueryResults.getTotal();

    queryFactory.selectFrom(member).fetchCount();
  }

  @Test
  public void sort(){
    em.persist(new Member(null,100));
    em.persist(new Member("member5",100));
    em.persist(new Member("member6",100));

    List<Member> memberList = queryFactory
        .selectFrom(member)
        .where(member.age.eq(100))
        .orderBy(member.age.desc(), member.username.asc().nullsLast())
        .fetch();

    assertThat(memberList.get(0).getUsername()).isEqualTo("member5");
  }

  @Test
  public void paging1(){
    List<Member> result = queryFactory.selectFrom(member)
        .orderBy(member.age.desc())
        .offset(1)
        .limit(2)
        .fetch();

    assertThat(result.size()).isEqualTo(2);
  }
}
