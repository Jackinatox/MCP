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
import http.server
import os

port = int(os.environ.get("SERVER_PORT", 8080))

class Handler(http.server.BaseHTTPRequestHandler):
    def do_GET(self):
        body = b"Hello, World!\n"
        self.send_response(200)
        self.send_header("Content-Type", "text/plain")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def log_message(self, format, *args):
        print(f"[{self.address_string()}] {format % args}", flush=True)

print(f"Starting Hello World HTTP server on port {port}", flush=True)
http.server.HTTPServer(("0.0.0.0", port), Handler).serve_forever()
EOF

echo "Installation complete."