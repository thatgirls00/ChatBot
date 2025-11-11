# 📌 Description

한경국립대학교 학사공지·장학공지·한경공지·학생/교직원/기숙사 식단·학사일정 등을
LLM 기반 자연어 인터페이스로 조회할 수 있는 Spring Boot REST API 서버입니다.

사용자는
```
“이번 주 학사공지 알려줘”
“오늘 교직원식당 뭐 나와?”
“저번 달 장학금 공지 정리해줘”
```
처럼 자연어로 물어보고, 서버는 LLM + DB + 캐시 + 크롤러를 조합해
의도를 분석하고, 필요한 데이터를 찾아 응답합니다.

# 🧠 LLM & 서버 로직 (How it works?)

## 1. 사용자 입력 정제 (전처리 레이어)
1.	클라이언트(웹/카카오/기타)에서 자연어 질의가 들어오면,
ChatController → ChatService 로 전달됩니다.
2.	ChatService 는 먼저 입력 정제를 수행합니다.
	- 비속어 필터링
	- 반복 문자/줄임말/띄어쓰기 보정
	- 이모지, 특수문자 제거
	- “오늘/내일/저번주/저번달” 같은 표현 → 내부적으로 처리 가능한 형태로 유지
3.	이 단계에서 로그에 “원본 입력 / 정제된 입력” 을 같이 남겨 나중에 디버깅과 개선에 활용합니다.

👉 목적: LLM이 의미 분석에만 집중할 수 있도록 노이즈를 최소화하는 단계.

## 2. Intent & Keyword 추출 (LLM 분석 레이어)

정제된 문장에 대해 LlmService 가 OpenAI GPT API를 호출합니다.
이때 LLM에게 “intent + keywords 를 JSON 형식으로만 응답하라”고 프롬프트를 구성합니다.

intent 값은 실제 MySQL 테이블과 1:1로 매핑됩니다:
- ACADEMIC_NOTICE → academic_notices (학사공지)
- SCHOLARSHIP_NOTICE → scholarship_notices (장학공지)
- HANKYONG_NOTICE → hankyong_notices (한경공지)
- STUDENT_MEAL → student_meals (학생식당 식단)
- FACULTY_MEAL → faculty_meals (교직원식당 식단)
- DORM_MEAL → dorm_meals (기숙사 식단)
- ACADEMIC_SCHEDULE → academic_schedule (학사일정)

LLM 응답이 오면:
	1.	intent 값이 위 7개 중 하나인지 확인
	2.	해당되면 → 도메인별 서비스로 라우팅
	3.	해당 안 되거나 “없음”인 경우 → Fallback 로직(일반 LLM 답변 모드)으로 전환
  

## 3. Intent 라우팅 & DB 조회 (도메인 레이어)

예를 들어 intent = "STUDENT_MEAL" 인 경우:
1.	MealService 로 라우팅
2.	keywords.date_range 를 바탕으로 내부 날짜 파싱: 
- “오늘” → LocalDate.now()
- “이번 주” → 해당 주 월요일 ~ 일요일 범위
- “저번 주 / 저번 달” → 이전 주/이전 달 범위 계산
3.	파싱된 날짜를 기준으로 student_meals 테이블을 조회
- 예: WHERE meal_date BETWEEN :start AND :end
4.	조회 결과를 카카오/웹용 JSON 응답 포맷으로 변환해 반환

교직원/기숙사 식단도 같은 플로우로 동작합니다:
- FACULTY_MEAL → faculty_meals 조회
- DORM_MEAL → dorm_meals 조회

공지/일정도 같은 구조로 처리됩니다:
- ACADEMIC_NOTICE → academic_notices
- SCHOLARSHIP_NOTICE → scholarship_notices
- HANKYONG_NOTICE → hankyong_notices
- ACADEMIC_SCHEDULE → academic_schedule

각 테이블에서 인덱스가 걸려 있는 컬럼(예: notice_date, meal_date, hash 등)을 중심으로
필터링해 빠르게 데이터를 조회합니다.


## 4. Fallback: Intent 분류 실패 시 LLM 직접 응답

LLM이 intent를 “없음” 혹은 애매하게 분류한 경우:
1.	ChatService 에서 도메인 라우팅을 시도하지 않고, 다시 LLM에게 “그냥 일반 질문 답변 모드” 로 재요청
2.	이때는 DB 연동 대신, 일반 QA 모드로 동작:
- “그 내용은 학교 데이터베이스에 없는 질문이지만, 일반적으로는 …”
라는 식의 답변을 생성
3.	이렇게 해서 사용자가 “챗봇이 아예 모른다”는 느낌보단 최대한 답을 주는 경험을 하게 함

👉 구조적으로:
도메인 챗봇 (DB조회 우선) → 실패 시 일반 LLM 챗봇으로 한 단계 다운그레이드.
