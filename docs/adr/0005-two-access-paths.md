# Two access paths

Training officers authenticate through an external OIDC provider, which may be shared
by several deployments, and are admitted only if the deployment they are entering
also lists them. Crew members present a join code and no identity at all.

The allowlist exists precisely because the identity provider is shared: it
authenticates every station's officers equally, so a Tampere officer would otherwise
sign straight into Helsinki's instance and its scenario library. Authentication
proves who someone is; the deployment decides whose officers they are — the same
boundary drawn in ADR-0003.

The crew path is deliberately unauthenticated. Training is not access control, and
provisioning accounts for a Tuesday drill would not survive contact with a fire
station. A join code grants nothing outside the one exercise it belongs to.

## Consequences

The first officer of a deployment is bootstrapped from configuration; existing
officers add the rest. Do not collapse the allowlist into "any authenticated user" —
it is what keeps deployments isolated from one another, and the shared identity
provider cannot do that job. Join codes must be unguessable enough that a running
exercise cannot be walked into, and they stay live for the whole exercise so that a
position can be retaken.
