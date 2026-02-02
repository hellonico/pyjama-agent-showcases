#!/bin/bash
# Movie Review Agent - CLI Mode
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

# Use the showcase CLI
clj -M:cli movie "$1"
