# ⚔️ 루틴 퀘스트 — Native Android

RPG 스타일 습관 트래커 (Java + Android Native)

---

## 📲 APK 받는 방법

1. GitHub 저장소 상단 **Actions** 탭
2. 최근 **🏗️ APK 빌드** 클릭
3. 하단 **Artifacts** → `루틴퀘스트-APK-숫자` 다운로드
4. zip 압축 풀기 → `app-debug.apk` 폰으로 전송 후 설치

> 설정 → 보안 → 출처를 알 수 없는 앱 설치 허용

---

## 🗂️ 프로젝트 구조

```
app/src/main/
├── java/com/routinequest/app/
│   ├── data/
│   │   ├── Habit.java          - 습관 데이터 모델
│   │   ├── DataManager.java    - SharedPreferences 저장소
│   │   ├── GameEngine.java     - XP/레벨/업적/칭호 계산
│   │   ├── DateUtils.java      - 날짜 유틸
│   │   └── Title.java          - 칭호 모델
│   ├── ui/
│   │   ├── MainActivity.java   - 탭 전환 + 슬라이드 애니메이션
│   │   ├── QuestFragment.java  - 퀘스트(오늘/과거) 탭
│   │   ├── CharacterFragment.java - 캐릭터 대시보드
│   │   ├── StatsFragment.java  - 주간/월별 통계
│   │   ├── AddHabitSheet.java  - 습관 추가 바텀시트
│   │   └── SettingsSheet.java  - 설정/백업 바텀시트
│   └── adapter/
│       └── HabitAdapter.java   - RecyclerView 어댑터
└── res/
    ├── layout/                 - XML 레이아웃
    ├── anim/                   - 슬라이드 애니메이션 4종
    ├── drawable/               - 벡터 아이콘, 배경
    └── values/                 - 색상, 문자열, 테마
```

---

## ✨ 주요 기능

- **탭 전환 슬라이드 애니메이션** — 방향에 따라 좌/우 슬라이드
- **체크 바운스 애니메이션** — 습관 완료 시 탄성 효과
- **XP 바 카운트업 애니메이션** — 캐릭터 탭 진입 시
- **과거 날짜 수정** — 날짜 네비게이터로 이전 날 체크 가능
- **칭호 시스템** — 18개 칭호, 탭하면 즉시 장착
- **습관별 업적** — 각 습관마다 연속/누적 업적 자동 생성
- **백업 내보내기/불러오기** — JSON 파일로 데이터 이식

---

## 🔄 업데이트 방법

코드 수정 후 GitHub push → Actions가 자동 빌드
