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


## Uploaded WireGuard profile findings

The supplied `.conf` is shaped like an ordinary WireGuard client profile: `[Interface]` carries a per-client private key, IPv4 address, DNS and MTU; `[Peer]` carries a server public key, endpoint, `AllowedIPs` and `PersistentKeepalive`. The sample values `10.231.0.x/32`, DNS `10.231.0.1`, and MTU `1280` align with a separate peer-sharing subnet. An endpoint under `192.168.0.0/16` is private and is not globally reachable unless a local network routes it.

The source archive also indicates separate main-tunnel and local peer-sharing paths. The custom main path includes native components and server-provided transport profiles; the sample `.conf` does not disclose or implement that path. LUNA therefore keeps an explicit standard-WireGuard-only boundary rather than copying proprietary headers or pretending the transports are compatible.

## LUNA activation-link implementation

The Android client accepts `luna://enroll?api=<url-encoded-https-api>&token=<one-time-token>`. It validates the link shape, rejects non-HTTPS API URLs, duplicates and malformed tokens, then only prefills the form. The user must verify the API domain and press the enrollment button; there is no automatic network request on link open. The URI must never contain a WireGuard client private key or the administrator token.
