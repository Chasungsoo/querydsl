package co.kr.qdsl.querydsl;

import co.kr.qdsl.querydsl.dto.MemberDto;
import co.kr.qdsl.querydsl.dto.QMemberDto;
import co.kr.qdsl.querydsl.dto.UserDto;
import co.kr.qdsl.querydsl.entity.Member;
import co.kr.qdsl.querydsl.entity.QMember;
import co.kr.qdsl.querydsl.entity.QTeam;
import co.kr.qdsl.querydsl.entity.Team;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import java.util.List;

import static co.kr.qdsl.querydsl.entity.QMember.member;
import static co.kr.qdsl.querydsl.entity.QTeam.*;
import static com.querydsl.jpa.JPAExpressions.select;
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
  public void sort() {
    em.persist(new Member(null, 100));
    em.persist(new Member("member5", 100));
    em.persist(new Member("member6", 100));

    List<Member> memberList = queryFactory
        .selectFrom(member)
        .where(member.age.eq(100))
        .orderBy(member.age.desc(), member.username.asc().nullsLast())
        .fetch();

    assertThat(memberList.get(0).getUsername()).isEqualTo("member5");
  }

  @Test
  public void paging() {
    List<Member> result = queryFactory.selectFrom(member)
        .orderBy(member.age.desc())
        .offset(1)
        .limit(2)
        .fetch();

    assertThat(result.size()).isEqualTo(2);
  }

  @Test
  public void aggregation() {
    List<Tuple> result = queryFactory
        .select(
            member.count(),
            member.age.sum(),
            member.age.min(),
            member.age.max(),
            member.age.avg()
        ).from(member)
        .fetch();
    Tuple tuple = result.get(0);
    assertThat(tuple.get(member.count())).isEqualTo(4);
    assertThat(tuple.get(member.age.sum())).isEqualTo(100);
    assertThat(tuple.get(member.age.avg())).isEqualTo(25);

  }

  /**
   * 팀의 이름과 각 팀의 평균 연령 구하기.
   *
   * @throws Exception
   */
  @Test
  public void group() throws Exception {

    List<Tuple> fetch = queryFactory.select(team.name, member.age.avg())
        .from(member)
        .join(member.team, team)
        .groupBy(team.name)
        .fetch();

    Tuple teamA = fetch.get(0);
    Tuple teamB = fetch.get(1);

    assertThat(teamA.get(team.name)).isEqualTo("teamA");
    assertThat(teamA.get(member.age.avg())).isEqualTo(15);

    assertThat(teamB.get(team.name)).isEqualTo("teamB");
    assertThat(teamB.get(member.age.avg())).isEqualTo(35);

  }

  @Test
  public void join() throws Exception {
    List<Member> teamA = queryFactory
        .selectFrom(member)
        .leftJoin(member.team, team)
        .where(team.name.eq("teamA"))
        .fetch();


    assertThat(teamA)
        .extracting("username")
        .containsExactly("member1", "member2");
  }

  @Test
  public void theta_join() {
    em.persist(new Member("teamA"));
    em.persist(new Member("teamB"));

    List<Member> result = queryFactory
        .select(member)
        .from(member, team)
        .where(member.username.eq(team.name))
        .fetch();

    assertThat(result)
        .extracting("username")
        .containsExactly("teamA", "teamB");
  }


  /**
   * 회원과 팀을 조인하면서 팀 A인 팀만 조인, 회원은 모두 조회
   */
  @Test
  public void join_on_filtering() {

    List<Tuple> teamA = queryFactory
        .select(member, team)
        .from(member)
        .leftJoin(member.team, team)
        .on(team.name.eq("teamA"))
        .fetch();

    for (Tuple tuple : teamA) {
      System.out.println("tuple" + tuple);
    }
  }


  @PersistenceUnit
  EntityManagerFactory emf;

  @Test
  public void fetchJoinNo() throws Exception {
    em.flush();
    em.clear();

    Member findMember = queryFactory
        .selectFrom(member)
        .where(member.username.eq("member1"))
        .fetchOne();

    boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());

    assertThat(loaded).as("패치조인 미적용").isFalse();
  }

  @Test
  public void fetchJoinUse() throws Exception {
    em.flush();
    em.clear();

    Member findMember = queryFactory
        .selectFrom(member)
        .join(member.team, team).fetchJoin()
        .where(member.username.eq("member1"))
        .fetchOne();

    boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());

    assertThat(loaded).as("패치조인 적용").isTrue();
  }


  @Test
  public void subQuery() throws Exception {
    QMember memberSub = new QMember("memberSub");
    List<Member> result = queryFactory.selectFrom(member)
        .where(member.age.eq(select(memberSub.age.max())
            .from(memberSub)
        ))
        .fetch();

    assertThat(result).extracting("age").containsExactly(40);

  }

  @Test
  public void subQueryGoe() throws Exception {
    QMember memberSub = new QMember("memberSub");
    List<Member> result = queryFactory.selectFrom(member)
        .where(member.age.goe(
            select(memberSub.age.avg())
                .from(memberSub)
        ))
        .fetch();
    assertThat(result).extracting("age").containsExactly(30, 40);
  }

  @Test
  public void selectSubQuery() throws Exception {
    QMember memberSub = new QMember("memberSub");
    List<Tuple> result = queryFactory
        .select(
            member.username,
            select(memberSub.age.avg()).from(memberSub))
        .from(member)
        .fetch();

  }

  @Test
  public void basicCase() {
    List<String> result = queryFactory
        .select(
            member.age
                .when(10).then("10살")
                .when(20).then("20살")
                .when(30).then("30살")
                .otherwise("50이상")
        ).from(member)
        .fetch();
    for (String s : result) {
      System.out.println("s=" + s);
    }
  }

  @Test
  public void complexCase() throws Exception {
    List<String> result = queryFactory
        .select(
            new CaseBuilder()
                .when(member.age.between(10, 20)).then("0~20살")
                .otherwise("기타"))
        .from(member)
        .fetch();

    for (String s : result) {
      System.out.println("s=" + s);
    }
  }

  @Test
  public void constant() throws Exception {
    List<Tuple> result = queryFactory
        .select(member.username, Expressions.constant("A"))
        .from(member)
        .fetch();

    for (Tuple tuple : result) {
      System.out.println("tuple = " + tuple);
    }
  }

  @Test
  public void concat() {
    List<String> result = queryFactory
        .select(member.username.concat("_").concat(member.age.stringValue()))
        .from(member)
        .where(member.username.eq("member1"))
        .fetch();

    for (String s : result) {
      System.out.println("s = " + s);

    }
  }

  @Test
  public void simpleProjection() {
    List<String> result = queryFactory
        .select(member.username)
        .from(member)
        .fetch();

    for (String s : result) {
      System.out.println("s = " + s);
    }
  }

  @Test
  public void tupleProjection() {
    List<Tuple> result = queryFactory
        .select(member.username, member.age)
        .from(member)
        .fetch();
    for (Tuple tuple : result) {
      String username = tuple.get(member.username);
      Integer age = tuple.get(member.age);
      System.out.println("username  = " + username);
      System.out.println("age  = " + age);
    }
  }


  @Test
  public void findDtoByJPQL() {
    List<MemberDto> resultList = em.createQuery("select new co.kr.qdsl.querydsl.dto.MemberDto(m.username,m.age) from Member m", MemberDto.class)
        .getResultList();
    for (MemberDto memberDto : resultList) {
      System.out.println("memberDto = " + memberDto);
    }
  }

  @Test
  public void findDtoBySetter() throws Exception {
    List<MemberDto> result = queryFactory
        .select(Projections.bean(MemberDto.class,
            member.username,
            member.age))
        .from(member)
        .fetch();

    for (MemberDto memberDto : result) {
      System.out.println("memberdto = "+ memberDto);
    }
  }

  @Test
  public void findDtoByField() throws Exception {
    List<MemberDto> result = queryFactory
        .select(Projections.fields(MemberDto.class,
            member.username,
            member.age))
        .from(member)
        .fetch();

    for (MemberDto memberDto : result) {
      System.out.println("memberdto = "+ memberDto);
    }
  }

  @Test
  public void findDtoByConstructor() throws Exception {
    List<MemberDto> result = queryFactory
        .select(Projections.constructor(MemberDto.class,
            member.username,
            member.age))
        .from(member)
        .fetch();

    for (MemberDto memberDto : result) {
      System.out.println("memberdto = "+ memberDto);
    }
  }

  @Test
  public void findUserDto() throws Exception {
    List<UserDto> result = queryFactory
        .select(Projections.fields(UserDto.class,
            member.username.as("name"),
            member.age))
        .from(member)
        .fetch();

    for (UserDto userDto : result) {
      System.out.println("userDto = "+ userDto);
    }
  }

  @Test
  public void findDtoByQueryProjection() {
    List<MemberDto> result = queryFactory
        .select(new QMemberDto(member.username, member.age))
        .from(member)
        .fetch();
    for (MemberDto memberDto : result) {
      System.out.println("memberDto = " + memberDto);
    }
  }
}
