#!/usr/bin/env bash
# Déploie la webapp sur volthost (192.168.1.87), servie par systemd sur le port 8913
# (service liftbook-web.service → python3 ~/liftbook-serve/liftbook-serve.py).
# Le serveur envoie Cache-Control: no-store : rien d'autre à faire après le rsync.
set -e
cd "$(dirname "$0")/.."
# exclusions de sécurité : dev/seed.html écrase le localStorage du visiteur et déconnecte ;
# deploy-volthost.sh + README.md exposent la topologie interne (IP LAN/Tailscale, SSH, tunnel).
rsync -az --delete \
  --exclude 'tests/' --exclude 'CONTRAT.md' --exclude 'extract_web.py' \
  --exclude 'dev/' --exclude 'deploy-volthost.sh' --exclude 'README.md' \
  web/ volt@192.168.1.87:/srv/liftbook-web/
curl -s -o /dev/null -w "http://192.168.1.87:8913 → %{http_code}\n" http://192.168.1.87:8913/
curl -s -o /dev/null -w "seed.html → %{http_code} (attendu 404)\n" http://192.168.1.87:8913/dev/seed.html
