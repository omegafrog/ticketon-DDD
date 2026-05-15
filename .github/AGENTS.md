# GitHub Workflows

Purpose: build/push service images to GHCR and deploy through AWS SSM.

Look here:
- Per-service CI: `.github/workflows/*-ci.yml`
- Full release CI: `.github/workflows/common-ci.yml`
- Deploy: `.github/workflows/deploy.yml`
- Dispatcher workflow is intentionally misspelled: `.github/workflows/disptach-ci.yml`

Rules:
- Keep workflow names, image names, and deploy service names aligned.
- Do not paste AWS credentials or SSM parameter values into logs.
- Path filters should match owning module.

Commands:
- Inspect only targeted workflow files with `sed` or `rg`.
