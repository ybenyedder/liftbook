#!/usr/bin/env python3
"""Dev host: serves the RN-web shell on the same origin as the Metro bundle (avoids CORS on font assets)."""
import http.server, urllib.request, os, sys

PORT = int(sys.argv[1]) if len(sys.argv) > 1 else 8091
ROOT = os.path.dirname(os.path.abspath(__file__))
UP = "http://localhost:8081"

class H(http.server.SimpleHTTPRequestHandler):
    def __init__(self, *a, **kw):
        super().__init__(*a, directory=ROOT, **kw)
    def do_GET(self):
        if self.path.startswith("/index.bundle") or self.path.startswith("/assets/") or self.path.startswith("/node_modules/"):
            try:
                req = urllib.request.Request(UP + self.path, headers={"User-Agent": self.headers.get("User-Agent", "")})
                with urllib.request.urlopen(req, timeout=120) as r:
                    self.send_response(r.status)
                    for k, v in r.headers.items():
                        if k.lower() not in ("transfer-encoding", "content-length", "access-control-allow-origin"):
                            self.send_header(k, v)
                    body = r.read()
                    self.send_header("Content-Length", str(len(body)))
                    self.end_headers()
                    self.wfile.write(body)
            except Exception as e:
                self.send_error(502, str(e))
            return
        super().do_GET()

print(f"serving {ROOT} on {PORT}, proxying bundle/assets to {UP}")
http.server.ThreadingHTTPServer(("0.0.0.0", PORT), H).serve_forever()
