Frontend development service units live here.

These units run Vue CLI or uni-app development servers and are not intended for production deployment.
Install them only when you explicitly want long-running dev servers on a host:

```bash
python3 bs01ctl.py setup-services --include-frontend-dev
```

Templates use placeholders for project root, runtime user/group, and npm path.
Override them at install time when needed:

```bash
python3 bs01ctl.py setup-services --include-frontend-dev --project-root /srv/vidsprout --service-user devuser --service-group devuser
```
