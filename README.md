**계층 구조**

1. Port 계층 (외부와의 경계)
 - in : UseCase (인터페이스)
 - out : Repository (인터페이스)

2. Application 계층
    - Service (구현체)

3. Domain 계층 (비즈니스 로직)
    - Entity
    - Enum

4. Infra 계층 ( HTTP 요청 및 DB 접근 )
    - persistence : RepositoryImpl (구현체)
    - Web : Controller, ResponseDto, RequestDto

**의존성 흐름**

Controller → UseCase(interface) → Service(구현)→ Repository(interface)→ RepositoryImpl(JPA)


**이유**

사실 이전까지는 전통적인 MVC 패턴만 사용해왔습니다.
이번 프로젝트를 통해 아키텍처를 알아보며 **“도메인 로직의 순수성을 보장”**하고,
**“변경에 강한 구조”**를 지닌 설계가 아키텍처의 목적이라고 생각했습니다.
그러한 점을 느껴보고자 클린 아키텍처의 구조(핵사고날과 혼합이 된것 같지만..)를 채택했고, 아래와 같은 기준으로 설계했습니다.

**도메인 로직의 순수성을 보장**

도메인 계층은 기술 의존이 없도록 구성했습니다.
→ Entity, Enum 등 핵심 개념은 domain 패키지에 위치하며, 외부 기술에 의존하지 않습니다.

비즈니스 흐름은 Application(Service) 계층으로 분리했습니다.
→ 핵심 로직은 UseCase를 통해 외부와 통신(?)하고, Service로 구현하였습니다.

외부 기술과 도메인을 분리했습니다.
→ Controller, JPA,( 차후 Redis) 등 기술 의존 요소는 infra 계층에만 존재합니다.


**변경에 강한 구조를 만들기 위해 한 일**


의존성 역전 원칙(DIP)을 적용했습니다.

→ 항상 안쪽 계층(domain, application)을 향해 의존하도록 구조화했습니다.

→ 예: Controller → UseCase → Service → Repository (interface)

Port 구조를 적용하여 유연한 확장을 고려했습니다.
→ UseCase와 Repository는 port 계층의 interface로 선언
→ 구현체는 infra, application 계층에서 담당하여 쉽게 교체 가능합니다.

각 계층은 역할만 책임지고 서로 강하게 결합하지 않도록 하기 위해 노력했습니다.