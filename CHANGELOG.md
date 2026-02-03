# Changelog

All notable changes to this project are documented here.

## [2.1.0] — 2026-02-03

### Security
- Replaced bearer-style API key authentication with request-bound HMAC signing
- Added timestamp window enforcement to mitigate replay attacks
- Added nonce caching to detect and block request replays
- Bound signatures to HTTP method, path, and request body

### Architecture
- Formalized SynapSys as a local-only control plane behind a trusted edge
- Clarified trust boundary between Everlybot and SynapSys

### Validation
- Verified authentication, replay protection, and tamper resistance using live traffic
- Confirmed SynapSys is unreachable from public network paths

## [2.0.0] — 2026-01-XX

### Architecture
- Initial public release of SynapSys broker framework
- Introduced guard pipeline abstraction and provider strategy pattern
