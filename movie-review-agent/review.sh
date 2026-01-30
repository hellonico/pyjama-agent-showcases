#!/bin/bash
# Movie Review Agent - CLI Mode (using Pyjama generic runner)
# Usage: ./review.sh "movie name"

if [ $# -eq 0 ]; then
    echo "Usage: ./review.sh \"movie name\""
    echo ""
    echo "Examples:"
    echo "  ./review.sh \"Inception\""
    echo "  ./review.sh \"The Matrix\""
    echo "  ./review.sh \"Interstellar\""
    exit 1
fi

# Use pyjama's generic runner with agent config
clj -J-Dagents.edn="$(pwd)/movie-review-agent.edn" \
    -M:pyjama run movie-review-agent "{\"movie-name\":\"$1\"}"


