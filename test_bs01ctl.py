import tempfile
import unittest
from pathlib import Path

import bs01ctl


class Bs01CtlTemplateTests(unittest.TestCase):
    def test_render_service_template_replaces_placeholders_and_adds_metadata(self):
        with tempfile.TemporaryDirectory() as tmpdir:
            tmp = Path(tmpdir)
            template = tmp / "example.service"
            template.write_text(
                "\n".join(
                    [
                        "[Service]",
                        "User=__SERVICE_USER__",
                        "Group=__SERVICE_GROUP__",
                        "WorkingDirectory=__PROJECT_ROOT__/backend",
                        "ExecStart=__NPM_BIN__ run serve",
                    ]
                ),
                encoding="utf-8",
            )
            context = {
                "project_root": "/srv/vidsprout",
                "service_user": "bs01",
                "service_group": "bs01",
                "npm_bin": "/usr/local/bin/npm",
            }

            rendered = bs01ctl.render_service_template(template, context, include_metadata=True)

            self.assertIn("# ProjectRoot=/srv/vidsprout", rendered)
            self.assertIn("User=bs01", rendered)
            self.assertIn("Group=bs01", rendered)
            self.assertIn("WorkingDirectory=/srv/vidsprout/backend", rendered)
            self.assertIn("ExecStart=/usr/local/bin/npm run serve", rendered)

    def test_read_installed_service_context_reads_metadata_comments(self):
        with tempfile.TemporaryDirectory() as tmpdir:
            tmp = Path(tmpdir)
            unit = tmp / "bs01-gunicorn.service"
            unit.write_text(
                "\n".join(
                    [
                        "# Managed by bs01ctl.py from deploy/systemd/bs01-gunicorn.service",
                        "# ProjectRoot=/srv/vidsprout",
                        "# ServiceUser=bs01",
                        "# ServiceGroup=apps",
                        "# NpmBin=/usr/bin/npm",
                        "",
                        "[Service]",
                        "ExecStart=/srv/vidsprout/.venv/bin/gunicorn",
                    ]
                ),
                encoding="utf-8",
            )

            ctx = bs01ctl.read_installed_service_context(unit)

            self.assertEqual(
                ctx,
                {
                    "project_root": "/srv/vidsprout",
                    "service_user": "bs01",
                    "service_group": "apps",
                    "npm_bin": "/usr/bin/npm",
                },
            )


if __name__ == "__main__":
    unittest.main()
