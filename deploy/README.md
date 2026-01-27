# BS01 Deployment Guide

This document lists production-critical environment variables and a minimal deployment checklist using systemd + Gunicorn + Celery. Adapt as needed.

## Environment variables (see deploy/env.example)
- Backend core
  - SECRET_KEY (required)
  - DEBUG (false for production)
  - ALLOWED_HOSTS (comma-separated)
  - SITE_URL (public API base, e.g. https://api.example.com)
  - MEDIA_URL (/media or absolute CDN URL)
- Auth
  - API_ENABLE_SESSION_AUTH (default false)
  - REFRESH_TOKEN_LIFETIME_DAYS (default 60)
- Email
  - CONTACT_EMAIL_TO (required for contact form)
  - ADMIN_EMAIL_LIST (optional comma-separated)
  - EMAIL_HOST, EMAIL_PORT, EMAIL_HOST_USER, EMAIL_HOST_PASSWORD, EMAIL_USE_TLS
  - DEFAULT_FROM_EMAIL
- Database (PostgreSQL recommended)
  - DB_ENGINE, DB_NAME, DB_USER, DB_PASSWORD, DB_HOST, DB_PORT, DB_CONN_MAX_AGE
- Cache / Celery / Redis
  - REDIS_URL, USE_REDIS_CACHE
  - CELERY_BROKER_URL, CELERY_RESULT_BACKEND
  - CELERY_TASK_ALWAYS_EAGER=false (ensure async in prod)
- CORS / CSRF
  - CORS_ALLOWED_ORIGINS, CSRF_TRUSTED_ORIGINS
  - CORS_ALLOW_CREDENTIALS
- Security (when DEBUG=false)
  - SECURE_HSTS_SECONDS, SECURE_SSL_REDIRECT, USE_X_FORWARDED_PROTO
- Uploads / Throttles
  - VIDEO_MAX_SIZE_BYTES
  - THROTTLE_* (see env.example for full list)

## Minimal deployment steps
1) Setup OS deps (example on Ubuntu):
   - apt install -y python3-venv python3-dev build-essential nginx redis-server
2) Create virtualenv and install deps:
   - python3 -m venv /root/BS01/.venv
   - /root/BS01/.venv/bin/pip install -U pip
   - /root/BS01/.venv/bin/pip install -r /root/BS01/requirements.txt
3) Configure environment:
   - cp deploy/env.example backend/.env
   - Edit backend/.env to your values (SECRET_KEY, DB_*, REDIS_*, SITE_URL, CORS_*)
4) Initialize database:
   - cd backend; /root/BS01/.venv/bin/python manage.py migrate
   - (optional) /root/BS01/.venv/bin/python manage.py createsuperuser
5) Static/media
   - If using object storage, point MEDIA_URL to CDN and keep MEDIA_ROOT for worker temp.
   - For local storage, serve MEDIA_ROOT via Nginx (see sample below).
6) Systemd services
   - Gunicorn: deploy/systemd/bs01-gunicorn.service
   - Celery worker: deploy/systemd/bs01-celery.service
   - Celery beat: deploy/systemd/bs01-celery-beat.service
   - Enable & start:
     - systemctl daemon-reload
     - systemctl enable bs01-gunicorn bs01-celery bs01-celery-beat
     - systemctl start bs01-gunicorn bs01-celery bs01-celery-beat
7) Nginx (very brief sketch)
   - proxy_pass to 127.0.0.1:8000 for API
   - serve /media/ from backend MEDIA_ROOT (local) or set appropriate caching headers if MEDIA_URL is CDN
   - enable gzip and HLS range requests for .ts/.m3u8 as needed

## Notes
- Use a non-root user in systemd services in production (User=bs01, Group=bs01) and set proper permissions.
- For object storage, views already use default_storage.url/exists; Celery tasks assume local files under MEDIA_ROOT. If storing originals remotely, add a download-to-temp step before ffmpeg/ffprobe and upload results afterwards.
- Ensure CORS_ALLOWED_ORIGINS and CSRF_TRUSTED_ORIGINS are correctly set for your frontends.
