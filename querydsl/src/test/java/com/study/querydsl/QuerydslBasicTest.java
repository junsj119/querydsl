package com.study.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.study.querydsl.dto.MemberDto;
import com.study.querydsl.dto.QMemberDto;
import com.study.querydsl.dto.UserDto;
import com.study.querydsl.entity.Member;
import com.study.querydsl.entity.QMember;
import com.study.querydsl.entity.QTeam;
import com.study.querydsl.entity.Team;
import org.assertj.core.api.Assert;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import java.util.List;

import static com.study.querydsl.entity.QMember.*;
import static com.study.querydsl.entity.QTeam.*;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @Autowired
    EntityManager em;

    @BeforeEach
    public void before(){
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
    public void startJQPL(){
        //member1을 찾아라
        String qlString = "select m from Member m where m.username = :username";

        Member findMember = em.createQuery(qlString, Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        Assertions.assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void startQuerydsl(){
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        //QMember m = new QMember("m"); //별칭 직접 지정
        //QMember m = QMember.member; //기본 인스턴스 사용

        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        Assertions.assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void search(){
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1")
                .and(member.age.eq(10)))
                .fetchOne();

        Assertions.assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void searchAndParam(){
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        Member findMember = queryFactory
                .selectFrom(member)
                .where(
                        member.username.eq("member1"),
                        member.age.eq(10)
                    )
                .fetchOne();

        Assertions.assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void resultFetch(){
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);

        //List
        List<Member> fetch = queryFactory
                .selectFrom(member)
                .fetch();

        //단 건
        Member findMember1 = queryFactory
                .selectFrom(member)
                .fetchOne();

        //처음 한 건 조회
        Member findMember2 = queryFactory
                .selectFrom(member)
                .fetchFirst();

        //페이징에서 사용
        QueryResults<Member> results = queryFactory
                .selectFrom(member)
                .fetchResults();
        //results.getLimit(); 등등 results 안에는 페이징 관련 친구들이 담겨있다.

        //count 쿼리로 변경
        long count = queryFactory
                .selectFrom(member)
                .fetchCount();
    }
    /*
    회원 정렬 순서
    1. 회원 나이 내림차순(desc)
    2. 회원 이름 올림차순(asc)
    단 2에서 회원 이름이 없으면 마지막에 출력(nulls last)
     */

    @Test
    public void sort(){
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);

        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);

        Assertions.assertThat(member5.getUsername()).isEqualTo("member5");
        Assertions.assertThat(member6.getUsername()).isEqualTo("member6");
        Assertions.assertThat(memberNull.getUsername()).isNull();
    }

    @Test
    public void paging1(){
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);

        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetch();

        Assertions.assertThat(result.size()).isEqualTo(2);
    }

    @Test
    public void paging2(){
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);

        QueryResults<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetchResults();

        Assertions.assertThat(result.getTotal()).isEqualTo(4);
        Assertions.assertThat(result.getLimit()).isEqualTo(2);
        Assertions.assertThat(result.getOffset()).isEqualTo(1);
        Assertions.assertThat(result.getResults().size()).isEqualTo(2);
    }

    @Test
    public void aggregation(){
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);

        List<Tuple> result = queryFactory
                .select(
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                )
                .from(member)
                .fetch();
        //tuple이란 여러개의 타입이 있을 때 막 꺼내올 수 있는 것.

        Tuple tuple = result.get(0);
        Assertions.assertThat(tuple.get(member.count())).isEqualTo(4);
        Assertions.assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        Assertions.assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        Assertions.assertThat(tuple.get(member.age.max())).isEqualTo(40);
        Assertions.assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }

    //팀의 이름과 각 팀의 평균 연령을 구해라.
    @Test
    public void group() throws Exception{
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);

        List<Tuple> result = queryFactory
                .select(
                        team.name, member.age.avg()
                )
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        Assertions.assertThat(teamA.get(team.name)).isEqualTo("teamA");
        Assertions.assertThat(teamA.get(member.age.avg())).isEqualTo(15);

        Assertions.assertThat(teamB.get(team.name)).isEqualTo("teamB");
        Assertions.assertThat(teamB.get(member.age.avg())).isEqualTo(35);
    }

    /*
    * TeamA에 소속된 모든 회원
    * */
    @Test
    public void join() throws Exception {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);

        List<Member> result = queryFactory
                .selectFrom(member)
                .join(member.team, team)        //join(별칭 대상, 별칭으로 사용할 Q타입)    //select m from Member m join m.team t
                .where(team.name.eq("teamA"))
                .fetch();

        Assertions.assertThat(result)
                .extracting("username")
                .containsExactly("member1", "member2");
    }

    /*
    * 세타 조인
    * 회원의 이름이 팀 이름과 같은 회원을 조회
    * */
    @Test
    public void theta_join(){
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);

        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        List<Member> result = queryFactory
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();

        Assertions.assertThat(result)
                .extracting("username")
                .containsExactly("teamA", "teamB");
    }

    /*
    * 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
    * JPQL : select m, t from Member m left join m.team t on t.name = 'teamA'
    * */
    @Test
    public void join_on_filtering(){
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);

        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team)
                .on(team.name.eq("teamA"))
                .fetch();
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }


    /*
     * 연관관계가 없는 엔티티 외부 조인
     * 회원의 이름이 팀 이름과 같은 대상을 외부 조인
     * */
    @Test
    public void join_on_no_relation(){
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);

        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team)
                .on(member.username.eq(team.name))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("Tuple = " + tuple);
        }
    }

    @PersistenceUnit
    EntityManagerFactory emf;

    @Test
    public void fetchJoinNo(){
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);

        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        Assertions.assertThat(loaded).as("페치 조인 미적용").isFalse();
    }

    @Test
    public void fetchJoinUser(){
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);

        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        Assertions.assertThat(loaded).as("페치 조인 적용").isTrue();
    }

    /*
    * 나이가 가장 많은 회원 조회
    * */
    @Test
    public void subQuery(){
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);

        QMember memberSub = new QMember("memberSub");
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        JPAExpressions.select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetch();

        Assertions.assertThat(result).extracting("age")
                .containsExactly(40);
    }

    /*
     * 나이가 평균 이상인 회원 조회
     * */
    @Test
    public void subQueryGoe(){
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);

        QMember memberSub = new QMember("memberSub");
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        JPAExpressions.select(memberSub.age.avg())
                                .from(memberSub)
                ))
                .fetch();

        Assertions.assertThat(result).extracting("age")
                .containsExactly(30, 40);
    }

    /*
     * 나이가 평균 이상인 회원 조회
     * */
    @Test
    public void subQueryIn(){
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);

        QMember memberSub = new QMember("memberSub");
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.in(
                        JPAExpressions
                                .select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.gt(10))
                ))
                .fetch();

        Assertions.assertThat(result).extracting("age")
                .containsExactly(20, 30, 40);
    }

    @Test
    public void selectSubQuery(){
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        QMember memberSub = new QMember("memberSub");

        List<Tuple> result = queryFactory
                .select(member.username,
                        JPAExpressions
                                .select(memberSub.age.avg())
                                .from(memberSub))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    public void basicCase(){
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        List<String> result = queryFactory
                .select(member.age
                        .when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void complexCase(){
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        queryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20살")
                        .when(member.age.between(21, 30)).then("21~30살")
                        .otherwise("기타"))
                .from(member)
                .fetch();
    }

    @Test
    public void constant(){
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        List<Tuple> result = queryFactory
                .select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    public void concat(){
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);

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
    public void simpleProjection(){
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);

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
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);

        List<Tuple> result = queryFactory
                .select(member.username, member.age)
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);

            System.out.println("username=" + username);
            System.out.println("age=" + age);
        }
    }

    @Test
    public void findDtoByJPQL(){
        List<MemberDto> result = em.createQuery(
                        "select new com.study.querydsl.dto.MemberDto(m.username, m.age) " +
                                "from Member m", MemberDto.class)
                .getResultList();
    }

    @Test
    public void findDtoBySetter(){
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        List<MemberDto> result = queryFactory
                .select(Projections.bean(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();
    }

    @Test
    public void findDtoByFields(){
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        List<MemberDto> result = queryFactory
                .select(Projections.fields(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();
    }

    @Test
    public void findDtoByConstructor(){
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        List<MemberDto> result = queryFactory
                .select(Projections.constructor(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();
    }

    @Test
    public void findUserDto(){
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        List<UserDto> result = queryFactory
                .select(Projections.fields(UserDto.class,
                        member.username.as("name"),
                        member.age))
                .from(member)
                .fetch();
    }

    @Test
    public void findDtoByQueryProjection(){
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        List<MemberDto> result = queryFactory
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();
    }





    @Test
    public void 동적쿼리_BooleanBuilder() throws Exception {
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember1(usernameParam, ageParam);
        Assertions.assertThat(result.size()).isEqualTo(1);
    }
    private List<Member> searchMember1(String usernameCond, Integer ageCond) {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);

        BooleanBuilder builder = new BooleanBuilder();
        if (usernameCond != null) {
            builder.and(member.username.eq(usernameCond));
        }
        if (ageCond != null) {
            builder.and(member.age.eq(ageCond));
        }
        return queryFactory
                .selectFrom(member)
                .where(builder)
                .fetch();
    }



    @Test
    public void dynamicQuery_WhereParam(){
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember2(usernameParam, ageParam);
        Assertions.assertThat(result.size()).isEqualTo(1);
    }
    private List<Member> searchMember2(String usernameCond, Integer ageCond) {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        return queryFactory
                .selectFrom(member)
                .where(allEq(usernameCond, ageCond))
                .fetch();
    }
    private BooleanExpression usernameEq(String usernameCond) {
        if(usernameCond != null){
            return member.username.eq(usernameCond);
        }else{
            return null;
        }
        //return usernameCond != null ? member.username.eq(usernameCond) : null;
    }
    private BooleanExpression ageEq(Integer ageCond) {
        return ageCond != null ? member.age.eq(ageCond) : null;
    }

    private Predicate allEq(String usernameCond, Integer ageCond){
        return usernameEq(usernameCond).and(ageEq(ageCond));
    }


    @Test
    public void bulkUpdate(){
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);

        queryFactory
                .update(member)
                .set(member.username, "비회원")
                .where(member.age.lt(28))
                .execute();     //update문을 사용할 때 사용

        em.flush();
        em.clear();

        List<Member>result = queryFactory
                .selectFrom(member)
                .fetch();
    }

    @Test
    public void bulkAdd(){
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);

        long count = queryFactory
                .update(member)
                .set(member.age, member.age.add(1))
                .execute();
    }

    @Test
    public void bulkDelete(){
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        queryFactory
                .delete(member)
                .where(member.age.gt(18))
                .execute();
    }

    //member -> M 으로 바꿔주는 함수
    @Test
    public void sqlFunction(){
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);

        String result = queryFactory
                .select(Expressions.stringTemplate("function('replace', {0}, {1}, {2})",
                                                                        member.username, "member", "M"))
                .from(member)
                .fetchFirst();
    }

    //소문자로 바꾸주는 함수
    @Test
    public void sqlFunction2(){
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        queryFactory
                .select(member.username)
                .from(member)
//                .where(member.username.eq(
//                        Expressions.stringTemplate("function('lower', {0})", member.username)))
                .where(member.username.eq(member.username.lower()))
                        .fetch();
    }
}
