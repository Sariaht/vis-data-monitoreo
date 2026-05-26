#!/bin/bash
# generate-traffic.sh
# Script de tráfico sintético (bash/Linux/Mac)

BASE_URL="http://localhost:3000"
DURATION=${1:-120}   # segundos, default 2 min
DELAY=${2:-1}        # delay entre requests

echo "============================================"
echo "   Todo API - Script de Tráfico Sintético"
echo "============================================"
echo "URL Base : $BASE_URL"
echo "Duración : ${DURATION}s | Delay: ${DELAY}s"
echo "Presiona Ctrl+C para detener"
echo "--------------------------------------------"

TITLES=("Revisar logs" "Actualizar deps" "Escribir tests" "Code review" "Deploy a producción")
PRIORITIES=("LOW" "MEDIUM" "HIGH")
TODO_IDS=()
COUNT=0
START=$(date +%s)

while true; do
    NOW=$(date +%s)
    ELAPSED=$((NOW - START))
    [ $ELAPSED -ge $DURATION ] && break

    COUNT=$((COUNT + 1))
    OP=$((COUNT % 10))

    case $OP in
        0)
            STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/")
            echo "[$ELAPSED s] GET /                  -> $STATUS"
            ;;
        1)
            STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/todos")
            echo "[$ELAPSED s] GET /api/todos          -> $STATUS"
            ;;
        2)
            TITLE="${TITLES[$((RANDOM % ${#TITLES[@]}))]}"
            PRIORITY="${PRIORITIES[$((RANDOM % ${#PRIORITIES[@]}))]}"
            RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/todos" \
                -H "Content-Type: application/json" \
                -d "{\"title\":\"$TITLE $COUNT\",\"description\":\"Auto generada\",\"priority\":\"$PRIORITY\"}")
            STATUS=$(echo "$RESPONSE" | tail -1)
            ID=$(echo "$RESPONSE" | head -1 | grep -o '"id":[0-9]*' | grep -o '[0-9]*')
            [ -n "$ID" ] && TODO_IDS+=("$ID")
            echo "[$ELAPSED s] POST /api/todos         -> $STATUS"
            ;;
        3)
            STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/stats")
            echo "[$ELAPSED s] GET /api/stats          -> $STATUS"
            ;;
        4)
            STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/todos/filter/status?completed=false")
            echo "[$ELAPSED s] GET /filter/status      -> $STATUS"
            ;;
        5)
            STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/todos/filter/priority?value=HIGH")
            echo "[$ELAPSED s] GET /filter/priority    -> $STATUS"
            ;;
        6)
            if [ ${#TODO_IDS[@]} -gt 0 ]; then
                IDX=$((RANDOM % ${#TODO_IDS[@]}))
                ID="${TODO_IDS[$IDX]}"
                STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X PATCH "$BASE_URL/api/todos/$ID/complete")
                echo "[$ELAPSED s] PATCH /todos/$ID/complete -> $STATUS"
            fi
            ;;
        7)
            echo "[$ELAPSED s] GET /api/slow           -> (esperando ~2-3s...)"
            STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/slow")
            echo "[$ELAPSED s] GET /api/slow           -> $STATUS"
            ;;
        8)
            FAKE_ID=$((RANDOM + 9000))
            STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/todos/$FAKE_ID")
            echo "[$ELAPSED s] GET /todos/$FAKE_ID     -> $STATUS (404 esperado)"
            ;;
        9)
            if [ ${#TODO_IDS[@]} -gt 0 ]; then
                IDX=$((RANDOM % ${#TODO_IDS[@]}))
                ID="${TODO_IDS[$IDX]}"
                TODO_IDS=("${TODO_IDS[@]:0:$IDX}" "${TODO_IDS[@]:$((IDX+1))}")
                STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE "$BASE_URL/api/todos/$ID")
                echo "[$ELAPSED s] DELETE /api/todos/$ID   -> $STATUS"
            fi
            ;;
    esac

    sleep $DELAY
done

echo ""
echo "============================================"
echo "  Completado. Total requests: $COUNT"
echo "============================================"
