# Security Policy

This project handles mineral and stone processing plant operating workflows.
Treat vulnerabilities as potentially high impact even when the demo data is
synthetic — this domain's failure modes include physical worker-safety
risk from heavy-machinery hazards (crush/entanglement injury) and dust
exposure.

## Do Not Disclose Publicly

Report privately before opening public issues for:

- credential exposure
- real operator, plant or crew data exposure
- authorization bypass
- MineralPlantCoordGovernor bypass
- audit-ledger tampering
- over-disclosure in reports or exports
- unsafe robot action dispatch
- any path that lets a proposal reach a plant-operation-execution
  decision, a plant-safety-clearance decision, or a
  plant-safety-officer-override decision

## Reporting

Use GitHub private vulnerability reporting when available for the repository.
If that is unavailable, contact the repository maintainers through the
cloud-itonami organization before publishing details.

Include:

- affected commit or version
- reproduction steps
- expected and actual behavior
- impact on operator/plant data, policy enforcement or audit logging
- suggested fix, if known

## Production Guidance

- Store secrets outside Git.
- Keep real operator/plant/crew data outside this repository.
- Run policy tests before deployment.
- Export and review audit logs regularly.
- Use least privilege for operators and service accounts.
