package kr.hhplus.be.server.user.domain;

import jakarta.persistence.*;
import kr.hhplus.be.server.common.base.BaseTimeEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long point;

   // @Version
    //private Long version;


    //UserRepositoryAdapter 테스트용
    public User(long userId, long point) {
        this.id = userId;
        this.point = point;
    }


    public User(Object o, String user1, long l) {
        super();
    }

    @Builder
    public User(long point){
        this.point = point;

    }

    public void chargePoint(long amount){

        this.point += amount;
    }

    public void usePoint(long amount){
        if(this.point < amount){
            throw new IllegalArgumentException("포인트가 부족합니다.");
        }

        this.point -= amount;
    }

}
