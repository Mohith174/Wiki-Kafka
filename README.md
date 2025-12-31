# Wiki Kafka - Real-Time Event Streaming System

![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.1-green)
![Kafka](https://img.shields.io/badge/Kafka-3.6.1-black)
![License](https://img.shields.io/badge/License-MIT-blue)

> 💥 **TL;DR:** Ingest 50+ Wikipedia edits/second → Process with Kafka Streams → Visualize in real-time

A production-grade event streaming system that ingests Wikipedia's real-time edit stream, processes events using Kafka Streams, and provides comprehensive monitoring with Prometheus and Grafana.

---

## 📐 Architecture

### System Overview

```
┌─────────────────────┐
│  WIKIMEDIA SSE      │
│  Stream API         │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│  INGESTION SERVICE  │
│  Port: 8084         │
│  • SSE Consumer     │
│  • Event Enricher   │
│  • Kafka Producer   │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│  KAFKA CLUSTER      │
│  Ports: 9092, 2181  │
│  • Broker           │
│  • Zookeeper        │
│  • Schema Registry  │
└──────────┬──────────┘
           │
           ├─────────────────┬─────────────────┐
           ▼                 ▼                 ▼
┌──────────────────┐  ┌──────────────┐  ┌──────────────┐
│ STREAM PROCESSOR │  │   CONSUMER   │  │ MONITORING   │
│ Port: 8082       │  │   SERVICE    │  │   LAYER      │
│ • Kafka Streams  │  │   Port: 8083 │  │              │
│ • Aggregations   │  │   • Fast     │  │ • Prometheus │
│ • State Store    │  │   • Slow     │  │ • Grafana    │
└──────────────────┘  └──────────────┘  │ • Alertmgr   │
                                         └──────────────┘
```

### Data Flow

```
Wikimedia → Ingestion → Stream → Consumer → SSE Feed
                 ↓          ↓         ↓
            wiki_changes  Processor  Service
                            ↓
                      wiki_aggregates
                            ↓
                      Metrics → Prometheus → Grafana
```

---

| Component | Technology | Version | Purpose |
|-----------|-----------|---------|---------|
| Runtime | Java | 17 | Application runtime |
| Framework | Spring Boot | 3.2.1 | Application framework |
| Messaging | Apache Kafka | 3.6.1 | Event streaming platform |
| Stream Processing | Kafka Streams | 3.6.1 | Stateful event processing |
| Metrics | Prometheus | Latest | Metrics collection |
| Visualization | Grafana | Latest | Dashboards and alerting |
| Alerting | Alertmanager | Latest | Alert routing |
| Containerization | Docker | Latest | Service deployment |
| HTTP Client | OkHttp | 4.12.0 | SSE stream consumption |
| Serialization | Jackson | 2.16.1 | JSON processing |

---

## 🚀 Quick Start

### Prerequisites

- Docker and Docker Compose
- Java 17 (for local development only)
- Maven 3.8+ (for local development only)

### 1. Build the Services

```bash
mvn clean package -DskipTests
```

> **Note:** First build takes ~2-3 minutes to download dependencies.

### 2. Start the Stack

```bash
docker-compose up -d
```

### 3. Verify Services are Running

```bash
docker-compose ps
```

Expected output:

| NAME | STATUS | PORTS |
|------|--------|-------|
| zookeeper | running | 0.0.0.0:2181->2181/tcp |
| kafka-broker | running | 0.0.0.0:9092->9092/tcp |
| schema-registry | running | 0.0.0.0:8081->8081/tcp |
| kafka-ui | running | 0.0.0.0:8080->8080/tcp |
| ingestion-service | running | 0.0.0.0:8084->8084/tcp |
| stream-processor | running | 0.0.0.0:8082->8082/tcp |
| consumer-service | running | 0.0.0.0:8083->8083/tcp |
| prometheus | running | 0.0.0.0:9090->9090/tcp |
| grafana | running | 0.0.0.0:3000->3000/tcp |
| alertmanager | running | 0.0.0.0:9093->9093/tcp |


### 4. Access the Services

| Service | URL | Credentials |
|---------|-----|-------------|
| Kafka UI | http://localhost:8080 | - |
| Prometheus | http://localhost:9090 | - |
| Grafana | http://localhost:3000 | admin / admin |
| Alertmanager | http://localhost:9093 | - |
| Web Dashboard | `open dashboard/index.html` | - |
| Ingestion Metrics | http://localhost:8084/actuator/prometheus | - |
| Processor Metrics | http://localhost:8082/actuator/prometheus | - |
| Consumer Metrics | http://localhost:8083/actuator/prometheus | - |

> **Note:** Open the web dashboard directly from the file system to avoid CORS issues: `open dashboard/index.html`

---

## 📊 Services Deep Dive

### Ingestion Service (Port 8084)

Connects to Wikimedia's real-time SSE stream and publishes events to Kafka.

**Key Features:**
- Server-Sent Events (SSE) consumption using OkHttp
- Automatic reconnection with exponential backoff
- Event enrichment (language extraction, region tagging)
- Idempotent Kafka producer with `acks=all`

**Producer Configuration:**

```java
// Durability settings
config.put(ProducerConfig.ACKS_CONFIG, "all");
config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
config.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE);
config.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);

// Batching for throughput
config.put(ProducerConfig.LINGER_MS_CONFIG, 20);
config.put(ProducerConfig.BATCH_SIZE_CONFIG, 32768);
```

**Health Check:**

```bash
curl http://localhost:8084/actuator/health
```

---

### Stream Processor (Port 8082)

Processes raw events using Kafka Streams with windowed aggregations.

**Key Features:**
- 1-minute tumbling window aggregations
- Counts events per wiki server (e.g., en.wikipedia.org)
- State store backed by RocksDB
- Fault-tolerant with automatic state recovery

**Stream Topology:**

```
wiki_changes (source topic)
    ↓
Parse JSON
    ↓
Extract serverName (e.g., "en.wikipedia.org")
    ↓
GroupBy serverName
    ↓
Windowed Count (1-minute windows)
    ↓
wiki_aggregates (sink topic)
```

**Kafka Streams Configuration:**

```java
props.put(StreamsConfig.APPLICATION_ID_CONFIG, "wiki-stream-processor");
props.put(StreamsConfig.PROCESSING_GUARANTEE_CONFIG, "at_least_once");
props.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 1000);
```

---

### Consumer Service (Port 8083)

Demonstrates consumer group behavior with fast and slow consumers.

**Consumer Groups:**

| Group | Processing Time | Purpose |
|-------|----------------|---------|
| fast | ~0ms | Real-time processing demo |
| slow | 100ms delay | Consumer lag demonstration |
| wiki-consumer-group | ~0ms | General consumption |

**Why Two Consumers?**

The slow consumer intentionally creates lag to demonstrate:
- Consumer group rebalancing
- Lag monitoring in Prometheus/Grafana
- Backpressure scenarios

---

## 📈 Monitoring

### Prometheus Metrics

All services expose metrics at `/actuator/prometheus`.

**Key Metrics:**

| Metric | Description |
|--------|-------------|
| `kafka_consumer_records_consumed_total` | Total records consumed |
| `kafka_consumer_records_lag` | Current consumer lag |
| `kafka_producer_record_send_total` | Total records sent |
| `kafka_streams_task_process_total` | Streams records processed |
| `http_server_requests_seconds` | HTTP endpoint latency |
| `jvm_memory_used_bytes` | JVM memory usage |

### Alert Rules

Pre-configured alerts in `alert_rules.yml`:

| Alert | Condition | Severity |
|-------|-----------|----------|
| HighConsumerLag | lag > 1000 for 5m | warning |
| KafkaBrokerDown | broker unreachable for 1m | critical |
| ServiceDown | any service down for 1m | critical |
| HighErrorRate | 5xx rate > 0.1/s for 5m | warning |

### Grafana Dashboards

Import or create dashboards to visualize:
- Message throughput (msgs/sec)
- Consumer lag per group
- Processing latency (p50, p95, p99)
- Service health status

**Suggested Panels:**

```
Messages per Second:
  rate(kafka_producer_record_send_total[1m])

Consumer Lag:
  kafka_consumer_records_lag

Processing Latency (p99):
  histogram_quantile(0.99, rate(http_server_requests_seconds_bucket[5m]))

Service Health:
  up{job=~"ingestion-service|stream-processor|consumer-service"}
```

---

## 🔧 Development

### Running Services Locally

Start only infrastructure:

```bash
docker-compose up -d zookeeper kafka-broker schema-registry kafka-ui prometheus grafana alertmanager
```

Run services with Maven:

```bash
# Terminal 1
cd ingestion-service
mvn spring-boot:run

# Terminal 2
cd stream-processor
mvn spring-boot:run

# Terminal 3
cd consumer-service
mvn spring-boot:run
```

---

## 🐚 Useful Kafka Commands

### List topics

```bash
docker exec kafka-broker kafka-topics \
  --list \
  --bootstrap-server localhost:9092
```

### Describe topic

```bash
docker exec kafka-broker kafka-topics \
  --describe \
  --topic wiki_changes \
  --bootstrap-server localhost:9092
```

### View consumer groups

```bash
docker exec kafka-broker kafka-consumer-groups \
  --list \
  --bootstrap-server localhost:9092
```

### Check consumer lag

```bash
docker exec kafka-broker kafka-consumer-groups \
  --describe \
  --group slow \
  --bootstrap-server localhost:9092
```

### Read from topic

```bash
docker exec kafka-broker kafka-console-consumer \
  --topic wiki_changes \
  --from-beginning \
  --max-messages 5 \
  --bootstrap-server localhost:9092
```

### Get topic offsets

```bash
docker exec kafka-broker kafka-run-class kafka.tools.GetOffsetShell \
  --broker-list localhost:9092 \
  --topic wiki_changes
```

---

## 📜 Monitoring Scripts

### Terminal dashboard with service status

```bash
./dashboard/dashboard.sh
```

### Monitor consumer lag in real-time

```bash
./dashboard/lag-monitor.sh
```

### Calculate throughput over 60 seconds

```bash
./dashboard/throughput.sh wiki_changes 60
```

---

## 📋 Configuration Reference

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| SPRING_KAFKA_BOOTSTRAP_SERVERS | localhost:9092 | Kafka broker address |
| SERVER_PORT | varies | Service HTTP port |
| SPRING_PROFILES_ACTIVE | default | Spring profile |

### Kafka Topics

| Topic | Partitions | Purpose |
|-------|-----------|---------|
| wiki_changes | auto-created | Raw Wikipedia events |
| wiki_aggregates | auto-created | Windowed aggregations |

### Service Ports

| Service | Port | Protocol |
|---------|------|----------|
| Zookeeper | 2181 | TCP |
| Kafka Broker | 9092, 29092 | TCP |
| Schema Registry | 8081 | HTTP |
| Kafka UI | 8080 | HTTP |
| Ingestion Service | 8084 | HTTP |
| Stream Processor | 8082 | HTTP |
| Consumer Service | 8083 | HTTP |
| Prometheus | 9090 | HTTP |
| Grafana | 3000 | HTTP |
| Alertmanager | 9093 | HTTP |

---

## 🔍 Troubleshooting

### Services Won't Start

**Check container logs:**

```bash
docker-compose logs -f ingestion-service
```

**Verify Kafka is healthy:**

```bash
docker exec kafka-broker kafka-broker-api-versions \
  --bootstrap-server localhost:9092
```

**Check Zookeeper:**

```bash
docker exec zookeeper zkCli.sh -server localhost:2181 ls /brokers/ids
```

---

### No Messages Flowing

**1. Check ingestion service logs for SSE connection status**

**2. Verify Wikimedia stream is accessible:**

```bash
curl -N https://stream.wikimedia.org/v2/stream/recentchange
```

**3. Check topic has messages:**

```bash
docker exec kafka-broker kafka-run-class kafka.tools.GetOffsetShell \
  --broker-list localhost:9092 \
  --topic wiki_changes
```

---

### High Consumer Lag

The slow consumer intentionally creates lag. For production scenarios:

1. Increase consumer instances (scale horizontally)
2. Reduce processing time per message
3. Increase topic partition count
4. Tune consumer fetch settings

---

### Memory Issues

**Check container memory usage:**

```bash
docker stats
```

**Increase container memory limits in docker-compose.yml:**

```yaml
services:
  stream-processor:
    deploy:
      resources:
        limits:
          memory: 2G
```

---

## 🛑 Stopping the Stack

### Stop all services (preserves data)

```bash
docker-compose down
```

### Stop and remove all volumes (clean state)

```bash
docker-compose down -v
```

### Stop specific service

```bash
docker-compose stop ingestion-service
```

---

## 🎓 Key Learnings

- Implemented exactly-once semantics with idempotent producers
- Built stateful stream processing with Kafka Streams windowing
- Configured observability stack (Prometheus + Grafana + Alertmanager)
- Handled backpressure with fast/slow consumer demonstration

---

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/x-feature`)
3. Commit your changes (`git commit -m 'Add x feature'`)
4. Push to the branch (`git push origin feature/x-feature`)
5. Open a Pull Request

---

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

---

## 🙏 Acknowledgments

- Wikimedia Foundation for the public SSE stream
- Apache Kafka community
- Spring Boot team
