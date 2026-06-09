# Gepsi

Project repository.

## CI/CD

GitHub Actions workflow at `.github/workflows/ci.yml` runs on every push and pull request to `main`:

- **Secret scan** — gitleaks blocks commits containing leaked credentials.
- **Dependency review** — flags vulnerable dependencies introduced in PRs.
- **CodeQL** — static analysis (auto-enables once source files exist).
- **Build/test placeholder** — wire to your stack when added.

Dependabot (`.github/dependabot.yml`) opens weekly PRs for GitHub Actions updates.

## Security

See [SECURITY.md](./SECURITY.md) for reporting vulnerabilities.

## Recommended repo settings

Configure in GitHub UI (`Settings → Branches → Branch protection rules` for `main`):

- Require pull request before merging
- Require status checks: `secret-scan`, `dependency-review`
- Require signed commits
- Require linear history
- Restrict who can push to `main`
