#!/bin/bash
# filepath: /Users/mohithk/Desktop/wiki-kafka-project/dashboard/throughput.sh

# Colors
CYAN='\033[0;36m'
YELLOW='\033[1;33m'
NC='\033[0m'
BOLD='\033[1m'

TOPIC=${1:-"wiki_changes"}
DURATION=${2:-60}

echo -e "${CYAN}${BOLD}📈 Kafka Throughput Monitor${NC}"
echo -e "${BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "Topic: ${YELLOW}$TOPIC${NC}"
echo -e "Duration: ${YELLOW}$DURATION seconds${NC}"
echo -e "${BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

# Get initial offset
INITIAL=$(docker exec kafka-broker kafka-run-class kafka.tools.GetOffsetShell \
    --broker-list localhost:9092 \
    --topic $TOPIC 2>/dev/null | awk -F: '{sum += $3} END {print sum}')

echo "Starting count: $INITIAL"
sleep $DURATION

# Get final offset
FINAL=$(docker exec kafka-broker kafka-run-class kafka.tools.GetOffsetShell \
    --broker-list localhost:9092 \
    --topic $TOPIC 2>/dev/null | awk -F: '{sum += $3} END {print sum}')

echo "Final count: $FINAL"

MESSAGES=$((FINAL - INITIAL))
RATE=$(echo "scale=2; $MESSAGES / $DURATION" | bc)

echo ""
echo -e "${BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${CYAN}Results:${NC}"
echo -e "  Messages processed: ${YELLOW}$MESSAGES${NC}"
echo -e "  Throughput: ${YELLOW}$RATE msg/sec${NC}"
echo -e "${BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"