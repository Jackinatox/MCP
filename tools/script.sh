#!/bin/ash
# Test Egg Installation Script
# Server Files: /home/container

mkdir -p /home/container
cd /home/container

echo "Downloading test server..."

for i in $(seq 1 10); do
  echo "Installing... step $i/10"
  sleep 1
done

cat > /home/container/server.py << 'EOF'
import sys

prefix = "[ECHO] "

print("Echo server started. Listening on stdin...", flush=True)

for line in sys.stdin:
    print(f"{prefix}{line}", end="", flush=True)
EOF

echo "Installation complete."