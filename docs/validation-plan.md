# Validation plan before release

1. Add parser tests for valid and invalid WireGuard profiles.
2. Integrate the official WireGuard userspace engine; do not implement cryptography yourself.
3. Handle VpnService permission, protect tunnel sockets, revoke, network changes, foreground notification, and disconnect.
4. Implement HTTPS API calls with certificate validation and short-lived enrollment tokens.
5. Verify server handshake using `wg show`.
6. Test IPv4/IPv6 routes, DNS, reconnect, airplane mode, battery optimization, and leak behavior.
7. Add instrumentation tests and verify release signing.
8. Do not ship until end-to-end tests pass.
