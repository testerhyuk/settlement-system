# Ad Performance Data Pipeline
### 광고 성과 분석을 위한 실시간·배치 데이터 파이프라인

Kafka, Flink, ClickHouse, Airflow를 기반으로 광고 노출·클릭·전환·예산 차감 결과·유저 세그먼트 이벤트를 처리하고, 캠페인/세그먼트 단위 성과 지표를 실시간 및 일별 배치로 집계하는 데이터 파이프라인입니다.

이 프로젝트는 광고 데이터의 수집, 처리, 저장, 리포팅, 모니터링까지 end-to-end 흐름을 설계하고, 장애 격리·backlog 복구·데이터 정합성·부하 테스트를 통해 운영 관점의 안정성을 검증하는 데 초점을 두었습니다.

---

## Project Highlights

| 항목 | 내용 |
| --- | --- |
| 실시간 처리 | Kafka topic 분리와 Flink event-time window 기반 1분 단위 성과 집계 |
| 세그먼트 분석 | user segment bootstrap, Kafka compacted topic, Flink broadcast state 기반 segment performance 집계 |
| 배치 리포팅 | Airflow DAG로 ClickHouse minute-level metric을 일별 campaign/segment mart로 재집계 |
| 안정성 설계 | Parse DLQ, Unknown Segment DLQ, Kafka backlog recovery, Airflow retry 검증 |
| 데이터 신뢰성 | campaign/segment/window/mart 정합성 테스트 자동화 |
| 성능 검증 | 150 iterations/sec, 30분 부하 테스트에서 API 실패율 0%, Kafka lag 최종 0, DLQ 0건 |
| 운영 관측 | Prometheus/Grafana 기반 API, Kafka, Flink, ClickHouse 대시보드 구성 |

---

## Project Goal

광고 시스템에서는 노출, 클릭, 전환, 예산 차감 결과가 서로 다른 시점과 경로로 발생합니다.  
이 이벤트들을 단순히 저장하는 것만으로는 캠페인 성과, 비용 효율, 세그먼트별 반응을 신뢰할 수 있는 지표로 제공하기 어렵습니다.

이 프로젝트에서는 다음 목표를 설정했습니다.

- 광고 행동 로그를 Kafka 기반으로 수집하고 이벤트 유형별로 분리한다.
- Flink event-time window를 이용해 1분 단위 실시간 metric을 생성한다.
- 캠페인/세그먼트 단위 성과 지표를 ClickHouse에 저장한다.
- ClickHouse의 minute-level metric을 Airflow로 일별 mart로 재집계한다.
- 파싱 실패, 세그먼트 미매칭, Flink 중지, ClickHouse 장애 상황에서 데이터 흐름이 어떻게 격리·복구되는지 검증한다.
- 데이터 정합성, 장애 복구, 부하 테스트를 스크립트로 검증 가능하게 만든다.

---

## Design Approach

이 프로젝트는 광고 데이터 파이프라인에 필요한 데이터 흐름과 운영 요구사항을 먼저 정의한 뒤 컴포넌트를 선택했습니다.

### Requirements Driven Design

| 요구사항 | 설계 방향 |
| --- | --- |
| 광고 이벤트는 순간적으로 대량 유입될 수 있음 | API가 직접 분석 DB에 쓰지 않고 Kafka를 통해 비동기 수집 |
| 노출·클릭·전환·예산 결과는 발생 빈도와 의미가 다름 | 이벤트 유형별 Kafka topic 분리 |
| 성과 지표는 이벤트 발생 시각 기준으로 집계해야 함 | Flink event-time window 사용 |
| 잘못된 단일 이벤트가 전체 streaming job을 멈추면 안 됨 | parse DLQ와 unknown segment DLQ 설계 |
| 세그먼트 정보는 광고 이벤트와 별도 흐름으로 갱신됨 | Kafka compacted topic과 Flink broadcast state 사용 |
| 실시간 metric과 광고주 리포팅용 mart는 목적이 다름 | ClickHouse minute-level metric과 Airflow daily mart 분리 |
| Airflow DAG는 같은 날짜에 대해 재실행될 수 있음 | batch_run_id와 latest view 기반 mart 조회 |
| 운영 중 병목과 장애를 확인할 수 있어야 함 | Prometheus/Grafana 기반 관측 지표 구성 |

### Design Principles

| 원칙 | 설명 |
| --- | --- |
| 장애 도메인 분리 | API, stream processing, batch reporting이 서로의 장애를 직접 전파하지 않도록 Kafka와 Airflow를 통해 분리 |
| 이벤트 유실 위험 완화 | API 이후 이벤트를 Kafka에 적재하고, Flink 중지 시에도 Kafka backlog로 보존되도록 설계 |
| 실패 이벤트 격리 | 파싱 실패와 세그먼트 미매칭을 DLQ로 분리하여 전체 job 안정성 확보 |
| 재실행 가능성 | Airflow mart는 batch_run_id 기반으로 같은 날짜 재실행을 허용 |
| 조회 일관성 | latest view를 통해 성공한 최신 batch 결과만 리포팅 조회에 사용 |
| 운영 관측 가능성 | API, Kafka, Flink, ClickHouse 지표를 Grafana에서 확인 가능하도록 구성 |
| 검증 가능한 설계 | 정합성, 복구, 장애, 부하 테스트를 스크립트로 자동화 |

### Technology Decisions

| 기술 | 선택 이유 |
| --- | --- |
| Kafka | 광고 이벤트 수집과 분석 처리를 분리하고, Flink 중지 시에도 이벤트를 backlog로 보존하기 위해 사용 |
| Kafka Streams | 예산 충전/차감 이벤트를 state store 기반으로 처리하고 budget result를 생성하기 위해 사용 |
| Flink | event-time 기반 window 집계와 상태 기반 스트리밍 처리가 필요했기 때문에 사용 |
| ClickHouse | campaign/segment 성과 지표를 빠르게 집계·조회하는 OLAP 저장소로 사용 |
| Airflow | 실시간 metric을 일별 리포팅 mart로 재가공하고, DAG retry와 실행 흐름을 관리하기 위해 사용 |
| PostgreSQL | 광고 캠페인, 유저 세그먼트 등 원천 상태성 데이터를 저장하기 위해 사용 |
| Prometheus/Grafana | API, Kafka, Flink, ClickHouse 상태를 운영 관점에서 관측하기 위해 사용 |
| k6 | 목표 부하를 수치화하고 API 처리 성능을 검증하기 위해 사용 |

---

## Architecture Decisions

### 1. API가 직접 분석 DB에 쓰지 않고 Kafka를 사용한 이유

광고 이벤트는 순간적으로 트래픽이 몰릴 수 있고, 분석 파이프라인의 장애가 API 응답 실패로 직접 전파되면 안 됩니다.  
따라서 광고 행동 이벤트 API는 이벤트를 Kafka에 발행하는 역할을 중심으로 두고, 이후 실시간 집계는 Flink가 비동기로 처리하도록 분리했습니다.

이 구조를 통해 API와 분석 파이프라인의 장애 도메인을 분리하고, Flink가 중지된 동안에도 Kafka에 이벤트를 보관했다가 재기동 후 backlog를 처리할 수 있게 했습니다.

### 2. 이벤트 유형별 topic을 분리한 이유

노출, 클릭, 전환, 예산 차감 결과는 발생 빈도와 의미가 다릅니다.  
노출은 가장 빈번하게 발생하고, 전환은 상대적으로 드물며, 예산 차감 결과는 클릭 이후 별도 처리 흐름에서 생성됩니다.

이를 하나의 topic에 섞으면 schema 관리와 장애 격리가 어려워질 수 있기 때문에 이벤트 유형별 topic으로 분리했습니다.  
이후 Flink에서 각 이벤트를 독립적으로 parse하고, 개별 metric pipeline과 performance pipeline으로 전달하도록 구성했습니다.

### 3. 실시간 metric과 일별 mart를 분리한 이유

Flink가 생성하는 1분 단위 metric은 실시간 모니터링과 빠른 성과 확인에는 적합하지만, 광고주 리포팅이나 장기 조회에는 일별 단위로 재집계된 mart가 더 적합합니다.

따라서 ClickHouse에는 minute-level metric을 먼저 저장하고, Airflow가 이를 일별 campaign/segment mart로 재집계하도록 설계했습니다.  
이 구조는 실시간 처리와 리포팅 배치의 책임을 분리하고, mart 재생성이나 검증을 Airflow 단에서 독립적으로 수행할 수 있게 합니다.

### 4. 세그먼트 정보를 Flink broadcast state로 처리한 이유

광고 이벤트는 userId를 포함하고, 세그먼트 정보는 별도 데이터로 관리됩니다.  
캠페인 성과를 세그먼트 단위로 분석하려면 광고 이벤트 처리 시점에 userId가 어떤 segment에 속하는지 확인해야 합니다.

세그먼트 정보는 전체 광고 이벤트에 비해 상대적으로 작고, 모든 Flink task에서 참조해야 하므로 broadcast state로 관리했습니다.  
또한 user-segments topic을 compacted topic으로 구성하고, Flink user segment source는 earliest offset부터 읽도록 하여 job 재시작 시 최신 segment 상태를 다시 구성할 수 있는 기반을 마련했습니다.

---

## Architecture Overview

| 단계 | 컴포넌트 | 역할 |
| --- | --- | --- |
| 1 | API Server | 광고 노출·클릭·전환·유저 세그먼트 이벤트 수집 및 Kafka 발행 |
| 2 | ad-streams | 광고 예산 충전/차감 이벤트 처리 및 budget result 발행 |
| 3 | Kafka | 이벤트 유형별 topic 분리, buffering, consumer group 기반 처리 |
| 4 | Flink ad-analytics | event-time window 기반 1분 metric 생성 및 performance metric 조합 |
| 5 | ClickHouse | 실시간 metric 및 일별 mart 저장 |
| 6 | Airflow | 일별 campaign/segment mart 생성, 검증, retry 관리 |
| 7 | Prometheus/Grafana | API, Kafka, Flink, ClickHouse 운영 지표 수집 및 대시보드 구성 |
| 8 | DLQ Topics | 파싱 실패 및 세그먼트 미매칭 이벤트 격리 |

---

## Core Data Flow

### Real-time Metric Flow

1. API Server가 광고 노출·클릭·전환 이벤트를 Kafka topic으로 발행합니다.
2. ad-streams가 예산 충전/차감 이벤트를 처리하고 budget result topic으로 결과를 발행합니다.
3. Flink ad-analytics가 각 topic을 읽고 JSON parse를 수행합니다.
4. parse에 성공한 이벤트는 event-time 기준 1분 window로 집계됩니다.
5. campaign budget, impression, click, conversion metric이 ClickHouse에 저장됩니다.
6. 개별 metric stream을 campaign_id와 window_start 기준으로 조합해 campaign performance metric을 생성합니다.
7. 광고 이벤트와 user segment broadcast state를 조합해 segment performance metric을 생성합니다.

### Batch Reporting Flow

1. ClickHouse에 저장된 minute-level campaign/segment metric을 Airflow DAG가 조회합니다.
2. report_date 기준으로 campaign/segment 단위 일별 metric을 재집계합니다.
3. mart 적재마다 batch_run_id를 부여합니다.
4. source aggregate와 mart aggregate를 비교해 정합성을 검증합니다.
5. daily_mart_batch_runs에 RUNNING/SUCCESS 실행 상태와 source_count/mart_count를 기록하고, 실패 시에는 Airflow task state와 callback을 통해 실패 원인을 추적하도록 구성했습니다.
6. latest view는 성공한 batch 중 finished_at이 가장 최신인 batch_run_id만 조회합니다.

---

## Engineering Challenges & Decisions

### 1. Event-time window가 닫히지 않던 문제

초기에는 Kafka에서 수집한 예산 차감 결과를 Flink 1분 window로 집계했지만, API 호출 후 충분한 시간이 지나도 ClickHouse에 metric이 적재되지 않는 문제가 발생했습니다.

처음에는 timestamp assigner 문제로 의심했지만, 실제 원인은 watermark 진행 방식이었습니다.  
Flink event-time window는 watermark가 window end를 넘어야 결과를 emit하는데, 일부 stream에 더 이상 이벤트가 들어오지 않으면 해당 stream이 idle 상태로 남아 watermark 진행을 막을 수 있습니다.

이를 해결하기 위해 parse 이후 typed event stream에 `WatermarkStrategy.forBoundedOutOfOrderness(...).withIdleness(...)`를 적용했습니다.  
일정 시간 이벤트가 없는 stream은 idle로 간주되어 watermark 계산에서 제외되도록 했고, 이후 window가 정상적으로 닫히며 ClickHouse에 metric이 적재되는 것을 확인했습니다.

이 과정에서 event-time 처리는 단순히 timestamp를 부여하는 것만으로 충분하지 않고, source idleness와 watermark propagation까지 함께 고려해야 한다는 점을 확인했습니다.

### 2. 시간 표현 불일치로 인한 이벤트 처리 불안정

초기 DTO에서는 `LocalDateTime`을 사용했고, 일부 이벤트는 JSON 직렬화 과정에서 timestamp 숫자 또는 timezone 없는 문자열로 발행되었습니다.  
이로 인해 API, Kafka, Flink, ClickHouse 사이에서 이벤트 발생 시각 해석이 달라질 수 있는 문제가 있었습니다.

광고 성과 집계는 window 기준 시간이 핵심이기 때문에, 시간 표현이 흔들리면 집계 결과의 신뢰성이 떨어집니다.

이를 해결하기 위해 shared DTO의 이벤트 시각을 `Instant` 기준으로 통일하고, Jackson 설정에서 JavaTimeModule과 ISO-8601 직렬화를 적용했습니다.  
이후 Kafka 메시지에는 UTC 기준 timestamp가 일관되게 기록되도록 정리했습니다.

이 결정은 단순 타입 변경이 아니라, 분산 파이프라인에서 시간 기준을 하나로 고정하기 위한 설계였습니다.

### 3. User Segment state race 문제

유저 세그먼트 기반 성과 집계를 위해 광고 이벤트와 user segment 정보를 Flink에서 조인해야 했습니다.  
초기에는 user segment 이벤트가 먼저 들어와 broadcast state에 저장되어 있을 것이라고 가정했지만, 실제 스트리밍 환경에서는 광고 이벤트가 segment state보다 먼저 도착할 수 있습니다.

이 경우 정상 이벤트임에도 segment 정보를 찾지 못해 Unknown Segment DLQ로 분리되는 문제가 발생했습니다.

이를 해결하기 위해 user segment 정보를 Kafka compacted topic으로 관리하고, API 서버 시작 시 DB의 user_segment 테이블을 Kafka로 bootstrap하도록 설계했습니다.  
또한 bootstrap 완료 marker를 도입해 Flink가 segment state 초기화 여부를 판단할 수 있도록 했습니다.

현재 정책은 bootstrap이 완료되지 않았거나 segment state가 없는 이벤트를 무작정 처리하지 않고 Unknown Segment DLQ로 격리하는 방식입니다.  
이를 통해 세그먼트 기준이 불명확한 이벤트가 잘못된 segment metric으로 집계되는 것을 방지하고, 별도 DLQ를 통해 원인을 추적할 수 있게 했습니다.

### 4. 파싱 실패가 Flink job 장애로 이어지는 문제

Kafka 메시지는 API 또는 다른 시스템을 통해 유입되기 때문에, 잘못된 JSON이나 스키마 불일치 메시지가 들어올 수 있습니다.  
초기 구조에서 역직렬화 실패가 그대로 예외로 전파되면 Flink job 전체 안정성에 영향을 줄 수 있었습니다.

이를 해결하기 위해 각 이벤트 파이프라인에 parse process function을 두고, 파싱 실패 시 원본 메시지와 에러 메시지를 별도 Kafka DLQ topic으로 발행하도록 설계했습니다.

이 방식은 잘못된 단일 메시지가 전체 streaming job을 중단시키지 않도록 격리하면서, 이후 실패 원인을 추적할 수 있는 근거도 남깁니다.

검증은 각 source topic에 broken JSON을 주입하고, 대응되는 parse DLQ topic에 원본 메시지와 에러 메시지가 기록되는지 확인하는 방식으로 수행했습니다.

### 5. ClickHouse mart 재실행 멱등성 문제

Airflow로 일별 campaign/segment mart를 생성할 때, 같은 날짜의 DAG를 재실행하면 중복 데이터가 생성될 수 있습니다.  
초기에는 `DELETE + INSERT` 방식을 고려했지만, ClickHouse의 delete mutation은 즉시 반영되지 않을 수 있어 재실행 시점에 따라 중복 조회 가능성이 남습니다.

이를 해결하기 위해 mart 적재마다 `batch_run_id`를 부여하고, 실행 상태를 `daily_mart_batch_runs` 테이블에 기록했습니다.  
리포팅 조회는 원본 mart 테이블을 직접 보지 않고, 성공한 batch 중 최신 batch만 바라보는 latest view를 통해 수행하도록 설계했습니다.

이 방식은 mart 테이블을 append-only로 유지하면서도, 조회 결과는 항상 최신 성공 batch 기준으로 제공할 수 있게 합니다.  
또한 실패한 batch와 성공한 batch의 이력을 분리해 Airflow 실행 상태를 추적할 수 있습니다.

### 6. ClickHouse 장애 시 Airflow retry와 복구 검증

운영 환경에서는 저장소 장애가 발생할 수 있기 때문에, Airflow mart 생성이 실패했을 때 어떻게 감지되고 복구되는지 검증했습니다.

ClickHouse를 중지한 상태에서 Airflow DAG를 실행해 task가 `up_for_retry` 상태로 전환되는지 확인했습니다.  
ClickHouse 장애 중에는 batch status를 ClickHouse에 기록하는 것 역시 실패할 수 있으므로, 장애 감지는 Airflow task state를 기준으로 확인했습니다.

이후 ClickHouse를 복구하고 DAG를 다시 실행해 mart가 정상 생성되고, `daily_mart_batch_runs`에 SUCCESS 상태와 source_count/mart_count가 기록되는지 확인했습니다.

이 검증은 정상 처리뿐 아니라 저장소 장애 상황에서 Airflow retry와 복구 후 mart 재생성이 가능한지를 확인하기 위한 테스트였습니다.

### 7. Flink 중지 중 Kafka backlog 복구 검증

Flink job이 중지된 상태에서 API를 통해 광고 이벤트를 계속 발행하면, Kafka에는 아직 처리되지 않은 이벤트가 backlog로 남습니다.

이 상태에서 Flink job을 재기동한 뒤 Kafka backlog가 소비되고 ClickHouse metric이 생성되는지 확인했습니다.  
이를 통해 Kafka가 분석 파이프라인 장애 구간의 buffering 역할을 수행하고, Flink 재기동 후 처리가 이어질 수 있음을 검증했습니다.

이 테스트는 Flink 내부 상태의 완전한 장애 복구를 증명하기 위한 것이 아니라, API 이후 Kafka에 적재된 이벤트가 Flink 중지 시간 동안 유실되지 않고 후속 처리될 수 있는지를 검증하기 위한 목적이었습니다.

---

## Validation Results

| 검증 항목 | 결과 |
| --- | --- |
| Parse DLQ 테스트 | 잘못된 JSON 메시지가 각 parse DLQ topic으로 격리됨 |
| Unknown Segment DLQ 테스트 | 세그먼트 정보가 없는 광고 이벤트가 unknown-segments DLQ로 분리됨 |
| Flink backlog recovery 테스트 | job 중지 중 Kafka에 쌓인 backlog를 재기동 후 처리 |
| Airflow retry 테스트 | ClickHouse 장애 시 task retry 상태 확인, 복구 후 mart 생성 |
| 데이터 정합성 테스트 | campaign/segment/window/mart 집계 결과가 기대값과 일치 |
| 30분 부하 테스트 | 150 iterations/sec 지속 부하에서 API 실패율 0%, Kafka lag 최종 0, DLQ 0건 |

---

## Data Consistency Validation

정합성 검증은 단순히 데이터가 적재됐는지가 아니라, source event와 metric/mart 결과가 수식상 일치하는지를 확인하는 방식으로 진행했습니다.

| 검증 대상 | 검증 내용 |
| --- | --- |
| Campaign performance metric | 동일 window에서 impression, click, conversion, budget result가 기대 count와 amount로 집계되는지 확인 |
| Segment performance metric | 동일 user가 속한 segment별로 campaign 성과와 일관된 metric이 생성되는지 확인 |
| Daily campaign mart | source campaign metric의 group count와 합계가 mart 결과와 일치하는지 확인 |
| Daily segment mart | source segment metric의 segment-campaign group count와 합계가 mart 결과와 일치하는지 확인 |

이 검증을 통해 실시간 metric과 일별 mart가 단순히 적재되는 수준을 넘어, 집계 수식과 결과 값이 기대값과 일치하는지 확인했습니다.

---

## Load Test Result

DAU 10만 규모의 광고 트래픽을 다음과 같이 단순 모델링했습니다.

| 항목 | 값 |
| --- | ---: |
| 가정 DAU | 100,000 |
| 유저당 일 광고 노출 | 50 |
| 일 노출 이벤트 | 5,000,000 |
| 평균 초당 노출 이벤트 | 약 58 events/sec |
| 피크 계수 | 2.5 |
| 목표 피크 부하 | 약 150 impressions/sec |

k6 테스트에서는 1 iteration이 기본적으로 impression 1건을 생성하고, click/conversion/budget deduct는 확률적으로 추가 발생하도록 구성했습니다.  
따라서 부하 목표는 노출 이벤트 기준 150 iterations/sec로 설정했고, 실제 HTTP 요청은 click, budget deduct, conversion 요청이 추가되며 평균 약 175 req/sec 수준으로 발생했습니다.

| 항목 | 결과 |
| --- | ---: |
| 테스트 시간 | 30분 |
| 목표 부하 | 150 iterations/sec |
| 총 iterations | 270,001 |
| 총 HTTP requests | 316,306 |
| 평균 HTTP request rate | 약 175.7 req/sec |
| API 실패율 | 0.00% |
| API p95 latency | 2.04 ms |
| Kafka consumer lag | 테스트 종료 후 0 |
| Flink checkpoint duration | 대부분 10 ~ 35 ms |
| DLQ 메시지 | 부하 테스트 중 0건 |

테스트 중 Kafka consumer lag가 주기적으로 상승/회복되는 패턴을 확인했습니다.  
다만 최종 lag가 0으로 수렴했고, API 실패율, Flink checkpoint, ClickHouse 적재, 데이터 정합성 기준을 모두 만족했기 때문에 현재 목표 부하에서는 병목으로 판단하지 않았습니다.

향후 목표 TPS를 상향하거나 lag 회복 시간이 SLO를 초과할 경우 Kafka partition 분산, Flink parallelism, checkpoint interval, ClickHouse sink batch/flush 정책 순으로 튜닝할 계획입니다.

---

## Observability

운영 중 데이터 흐름과 병목을 확인하기 위해 Prometheus/Grafana 기반 대시보드를 구성했습니다.

| 지표 | 목적 |
| --- | --- |
| API RPS | 목표 부하 유입 여부 확인 |
| API latency | p95/p99 지연 확인 |
| API error rate | 4xx/5xx 비율 확인 |
| Kafka consumer lag | Flink 처리 지연 및 backlog 여부 확인 |
| Kafka topic ingress | 이벤트 유입량 확인 |
| Flink records in/out | Flink 처리량 확인 |
| Flink checkpoint duration | checkpoint 지연 및 장애 징후 확인 |
| ClickHouse inserted rows | 분석 저장소 적재량 확인 |
| ClickHouse memory | OLAP 저장소 리소스 사용량 확인 |

Grafana 대시보드를 통해 30분 부하 테스트 동안 API 처리량, Kafka lag, Flink checkpoint, ClickHouse insert 상태를 함께 확인했습니다.  
이를 통해 단순 API 응답 성공뿐 아니라 streaming pipeline과 OLAP sink까지 데이터가 정상적으로 흘러가는지 관측할 수 있도록 했습니다.

---

## Limitations & Next Steps

| 한계 | 개선 방향 |
| --- | --- |
| 로컬 Docker 환경 검증 | 운영 환경에서는 독립 클러스터와 리소스 제한 기반 검증 필요 |
| 단일 campaign 중심 부하 테스트 | multi-campaign, multi-advertiser, multi-segment 분산 부하 테스트로 확장 |
| ClickHouse JDBC sink at-least-once 특성 | 운영 환경에서는 dedup key, ReplacingMergeTree, materialized view 등 중복 방지 전략 검토 |
| Alert rule 미구성 | Kafka lag, Flink checkpoint failure, DLQ 증가율 기반 alert 추가 |
| Kafka lag sawtooth 패턴 | partition 분산, Flink parallelism, checkpoint interval, sink flush 정책 튜닝 |
| Airflow batch status 저장소 단일화 | ClickHouse 장애 시 batch status 기록도 실패할 수 있으므로 별도 metadata 저장소 또는 알림 연계 검토 |
| 보안/접근통제 미흡 | secret 관리, 권한 분리, 개인정보/가명처리 정책 추가 필요 |
| API-Kafka 발행 원자성 | 운영 환경에서는 Transactional Outbox 또는 producer 실패 보상 로직으로 API 처리와 Kafka 발행 사이의 정합성 강화 필요 |

---

## Tech Stack

| 영역 | 기술 |
| --- | --- |
| Backend | Java 17, Spring Boot |
| Event Streaming | Apache Kafka |
| Stream Processing | Kafka Streams, Apache Flink |
| OLAP Storage | ClickHouse |
| Batch Workflow | Apache Airflow, Python |
| Monitoring | Prometheus, Grafana |
| Load Test | k6 |
| Infra | Docker Compose |
| Database | PostgreSQL |