#!/bin/bash
# filepath: /Users/mohithk/Desktop/wiki-kafka-project/dashboard/dashboard.sh

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color
BOLD='\033[1m'

# Clear screen and move cursor to top
clear

echo -e "${CYAN}${BOLD}"
echo "╔══════════════════════════════════════════════════════════════╗"
echo "║           🚀 WIKI KAFKA DASHBOARD 🚀                        ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo -e "${NC}"

# ============================================
# IMPROVED SERVICE CHECK FUNCTIONS
# ============================================

# Check HTTP service
check_http_service() {
    local url=$1
    local timeout=${2:-2}
    
    if curl -s --connect-timeout $timeout "$url" > /dev/null 2>&1; then
        echo -e "${GREEN}✓ UP${NC}"
    else
        echo -e "${RED}✗ DOWN${NC}"
    fi
}

# Check Zookeeper
check_zookeeper() {
    # Method 1: Docker health status (most reliable)
    local health=$(docker inspect --format='{{.State.Health.Status}}' zookeeper 2>/dev/null)
    if [ "$health" = "healthy" ]; then
        echo -e "${GREEN}✓ UP${NC}"
        return 0
    fi
    
    # Method 2: Check if container is running
    local state=$(docker inspect --format='{{.State.Status}}' zookeeper 2>/dev/null)
    if [ "$state" = "running" ]; then
        echo -e "${YELLOW}⚠ STARTING${NC}"
        return 0
    fi
    
    echo -e "${RED}✗ DOWN${NC}"
    return 1
}

# Check Kafka
check_kafka() {
    # Method 1: Docker health status
    local health=$(docker inspect --format='{{.State.Health.Status}}' kafka-broker 2>/dev/null)
    if [ "$health" = "healthy" ]; then
        echo -e "${GREEN}✓ UP${NC}"
        return 0
    fi
    
    # Method 2: Try listing topics
    if docker exec kafka-broker kafka-topics --bootstrap-server localhost:9092 --list &>/dev/null; then
        echo -e "${GREEN}✓ UP${NC}"
        return 0
    fi
    
    # Method 3: Check if container is running
    if docker ps --format '{{.Names}}' | grep -q "^kafka-broker$"; then
        echo -e "${YELLOW}⚠ STARTING${NC}"
        return 0
    fi
    
    echo -e "${RED}✗ DOWN${NC}"
    return 1
}

# Function to get Kafka topics count
get_topics_count() {
    docker exec kafka-broker kafka-topics --list --bootstrap-server localhost:9092 2>/dev/null | wc -l | tr -d ' '
}

# Function to get consumer lag
get_consumer_lag() {
    docker exec kafka-broker kafka-consumer-groups --bootstrap-server localhost:9092 --describe --all-groups 2>/dev/null | grep -v "TOPIC\|^$\|GROUP\|Consumer" | awk '{sum += $6} END {print (sum ? sum : 0)}'
}

# Function to get message count for a topic
get_topic_messages() {
    local topic=$1
    docker exec kafka-broker kafka-run-class kafka.tools.GetOffsetShell \
        --broker-list localhost:9092 \
        --topic "$topic" 2>/dev/null | awk -F: '{sum += $3} END {print (sum ? sum : 0)}'
}

# ============================================
# MAIN DASHBOARD LOOP
# ============================================

while true; do
    # Move cursor to line 7 (after header)
    tput cup 6 0
    
    echo -e "${BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${YELLOW}📡 SERVICE STATUS${NC}                    $(date '+%Y-%m-%d %H:%M:%S')"
    echo -e "${BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo ""
    
    # Check services with correct methods
    printf "  %-22s %s\n" "Zookeeper (2181):" "$(check_zookeeper)"
    printf "  %-22s %s\n" "Kafka Broker (9092):" "$(check_kafka)"
    printf "  %-22s %s\n" "Schema Registry:" "$(check_http_service 'http://localhost:8081/subjects')"
    printf "  %-22s %s\n" "Kafka UI (8080):" "$(check_http_service 'http://localhost:8080')"
    printf "  %-22s %s\n" "Prometheus (9090):" "$(check_http_service 'http://localhost:9090/-/healthy')"
    printf "  %-22s %s\n" "Grafana (3000):" "$(check_http_service 'http://localhost:3000/api/health')"
    printf "  %-22s %s\n" "AlertManager (9093):" "$(check_http_service 'http://localhost:9093/-/healthy')"
    printf "  %-22s %s\n" "Web Dashboard (8000):" "$(check_http_service 'http://localhost:8000')"
    
    echo ""
    echo -e "${BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${YELLOW}📊 KAFKA METRICS${NC}"
    echo -e "${BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo ""
    
    topics=$(get_topics_count)
    lag=$(get_consumer_lag)
    wiki_changes_count=$(get_topic_messages "wiki_changes")
    wiki_aggregates_count=$(get_topic_messages "wiki_aggregates")
    
    printf "  %-22s ${CYAN}%s${NC}\n" "Topics:" "$topics"
    printf "  %-22s ${CYAN}%s${NC}\n" "Consumer Lag:" "$lag"
    printf "  %-22s ${CYAN}%s${NC}\n" "wiki_changes msgs:" "$wiki_changes_count"
    printf "  %-22s ${CYAN}%s${NC}\n" "wiki_aggregates msgs:" "$wiki_aggregates_count"
    
    echo ""
    echo -e "${BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${YELLOW}📦 DOCKER CONTAINER STATUS${NC}"
    echo -e "${BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo ""
    
    # Show container health from docker
    docker ps --format "table {{.Names}}\t{{.Status}}" 2>/dev/null | head -10
    
    echo ""
    echo -e "${BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${YELLOW}🔗 QUICK LINKS${NC}"
    echo -e "${BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo ""
    echo -e "  Kafka UI:     ${BLUE}http://localhost:8080${NC}"
    echo -e "  Prometheus:   ${BLUE}http://localhost:9090${NC}"
    echo -e "  Grafana:      ${BLUE}http://localhost:3000${NC} (admin/admin123)"
    echo -e "  AlertManager: ${BLUE}http://localhost:9093${NC}"
    echo -e "  Web Dashboard:${BLUE}http://localhost:8000${NC}"
    echo ""
    echo -e "${BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "  Press ${RED}Ctrl+C${NC} to exit | Refreshing every 5 seconds..."
    
    sleep 5
done