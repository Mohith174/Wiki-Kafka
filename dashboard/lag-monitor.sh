#!/bin/bash
# filepath: /Users/mohithk/Desktop/wiki-kafka-project/dashboard/lag-monitor.sh

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'
BOLD='\033[1m'

echo -e "${CYAN}${BOLD}📊 Kafka Consumer Lag Monitor${NC}"
echo -e "${BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

while true; do
    echo -e "\n${YELLOW}Timestamp: $(date '+%Y-%m-%d %H:%M:%S')${NC}\n"
    
    # Get consumer group lag
    docker exec kafka-broker kafka-consumer-groups \
        --bootstrap-server localhost:9092 \
        --describe \
        --all-groups 2>/dev/null | head -50
    
    echo -e "\n${BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "Press ${RED}Ctrl+C${NC} to exit | Refreshing every 10 seconds..."
    
    sleep 10
done