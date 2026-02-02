#!/bin/bash
# Image Generator Agent - CLI Mode
# Usage: ./generate.sh "image prompt"

if [ $# -eq 0 ]; then
    echo "Usage: ./generate.sh \"image prompt\""
    echo ""
    echo "Examples:"
    echo "  ./generate.sh \"a sunset over mountains\""
    echo "  ./generate.sh \"a futuristic city at night\""
    echo "  ./generate.sh \"a cat wearing a space helmet\""
    exit 1
fi

# Use the showcase CLI
clj -M:cli image "$1"
