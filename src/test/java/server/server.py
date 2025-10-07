#!/usr/bin/env python3
import http.server
import socketserver
import os

# Port d'écoute
PORT = 8000

# Se placer dans le répertoire du script
os.chdir(os.path.dirname(os.path.abspath(__file__)))

# Handler qui sert les fichiers du répertoire courant
Handler = http.server.SimpleHTTPRequestHandler

# Lancer le serveur
with socketserver.TCPServer(("", PORT), Handler) as httpd:
    print(f"Serving files in {os.getcwd()} at http://localhost:{PORT}")
    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        print("\nArrêt du serveur")
        httpd.server_close()
