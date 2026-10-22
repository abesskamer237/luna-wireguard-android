# Findings from supplied Geny APK

Previously reported string-level evidence:
- Go WireGuard module: `golang.zx2c4.com/wireguard`
- WebSocket dependency: `github.com/gorilla/websocket`
- transport selector: `transport must be wss, camouflage, or slowdns`
- fields: `slowdns_domain`, `slowdns_pubkey`
- headers: `X-Geny-Proof`, `X-Geny-Access`, `X-Geny-Stream`, `X-Geny-Seq`
- API base string: `https://geny.payool.net/api/v1/`
- native proxy library: `libhev-socks5-tunnel.so`

These indicate code/dependencies/strings in the APK, not proof of current server availability, active phone configuration, exact wire format, or interoperability. This project does not copy Geny's proprietary transport.
